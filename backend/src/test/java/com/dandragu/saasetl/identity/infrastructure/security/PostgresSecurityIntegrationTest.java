package com.dandragu.saasetl.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.company.application.CompanyAccessDeniedException;
import com.dandragu.saasetl.company.application.CompanyAccessService;
import com.dandragu.saasetl.company.infrastructure.context.ActiveCompanySession;
import com.dandragu.saasetl.company.infrastructure.context.SecurityCompanyContext;
import com.dandragu.saasetl.identity.application.RegisterUserCommand;
import com.dandragu.saasetl.identity.application.RegistrationResult;
import com.dandragu.saasetl.identity.application.RegistrationService;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
		"server.servlet.session.cookie.secure=true",
		"nexo.security.cookie-secure=true"
})
@AutoConfigureMockMvc
@Testcontainers
class PostgresSecurityIntegrationTest {

	private static final String EMAIL = "owner@example.com";
	private static final String PASSWORD = "correct horse battery staple";

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private RegistrationService registrationService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private CompanyAccessService companyAccessService;
	@Autowired
	private ActiveCompanySession activeCompanySession;
	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM SPRING_SESSION_ATTRIBUTES");
		jdbcTemplate.update("DELETE FROM SPRING_SESSION");
		jdbcTemplate.update("DELETE FROM etl_executions");
		jdbcTemplate.update("DELETE FROM company_memberships");
		jdbcTemplate.update("DELETE FROM products");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM companies");
	}

	@Test
	void shouldIsolateEtlHistoryByActiveCompanyAndRevokeAccessImmediately() throws Exception {
		RegistrationResult first = registerUser();
		RegistrationResult second = registrationService.register(new RegisterUserCommand(
				"second@example.com",
				PASSWORD,
				"Second Company",
				new CurrencyCode("EUR")));
		AuthenticatedState firstSession = login(EMAIL, PASSWORD);
		AuthenticatedState secondSession = login("second@example.com", PASSWORD);

		downloadCsv(firstSession, "first-company.csv");
		downloadCsv(secondSession, "second-company.csv");

		mockMvc.perform(get("/api/etl/executions")
						.secure(true)
						.cookie(firstSession.session(), firstSession.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].sourceFileName").value("first-company.csv"))
				.andExpect(jsonPath("$[0].resultFileName").value("first-company-sin-email.csv"))
				.andExpect(jsonPath("$[1]").doesNotExist());

		mockMvc.perform(get("/api/etl/executions")
						.secure(true)
						.cookie(secondSession.session(), secondSession.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].sourceFileName").value("second-company.csv"))
				.andExpect(jsonPath("$[1]").doesNotExist());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM etl_executions WHERE company_id = ?",
				Integer.class,
				first.companyId())).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM etl_executions WHERE company_id = ?",
				Integer.class,
				second.companyId())).isEqualTo(1);

		jdbcTemplate.update(
				"UPDATE company_memberships SET status = 'SUSPENDED' WHERE user_id = ? AND company_id = ?",
				first.userId().value(),
				first.companyId());
		mockMvc.perform(get("/api/etl/executions")
						.secure(true)
						.cookie(firstSession.session(), firstSession.csrf()))
				.andExpect(status().isForbidden());
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldCompleteCsrfLoginMultipartSessionAndLogoutCycle() throws Exception {
		RegistrationResult registration = registerUser();
		CsrfState anonymous = getCsrf(null);

		assertThat(anonymous.session().isHttpOnly()).isTrue();
		assertThat(anonymous.session().getSecure()).isTrue();
		assertThat(anonymous.session().getAttribute("SameSite")).isEqualTo("Lax");
		assertThat(anonymous.csrf().isHttpOnly()).isFalse();
		assertThat(anonymous.csrf().getName()).isEqualTo("XSRF-TOKEN");

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.secure(true)
						.cookie(anonymous.session(), anonymous.csrf())
						.header("X-XSRF-TOKEN", anonymous.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"owner@example.com\",\"password\":\"correct horse battery staple\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.activeCompany.id").value(registration.companyId().toString()))
				.andExpect(jsonPath("$.activeCompany.role").value("OWNER"))
				.andReturn();

		Cookie authenticatedSession = requireCookie(login, "NEXO_SESSION");
		assertThat(authenticatedSession.getValue()).isNotEqualTo(anonymous.session().getValue());
		assertThat(authenticatedSession.isHttpOnly()).isTrue();
		assertThat(authenticatedSession.getSecure()).isTrue();

		CsrfState authenticated = getCsrf(authenticatedSession);
		assertThat(authenticated.token()).isNotEqualTo(anonymous.token());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM SPRING_SESSION",
				Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT MAX_INACTIVE_INTERVAL FROM SPRING_SESSION",
				Integer.class)).isEqualTo(1800);

		mockMvc.perform(get("/api/auth/session")
						.secure(true)
						.cookie(authenticatedSession, authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.activeCompany.id").value(registration.companyId().toString()))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		MockMultipartFile csv = new MockMultipartFile(
				"file", "clientes.csv", "text/csv", "id,name\n1,Ana".getBytes(StandardCharsets.UTF_8));
		mockMvc.perform(multipart("/api/csv/preview")
						.file(csv)
						.secure(true)
						.cookie(authenticatedSession, authenticated.csrf())
						.header("X-XSRF-TOKEN", authenticated.token()))
				.andExpect(status().isOk());

		MvcResult logout = mockMvc.perform(post("/api/auth/logout")
						.secure(true)
						.cookie(authenticatedSession, authenticated.csrf())
						.header("X-XSRF-TOKEN", authenticated.token()))
				.andExpect(status().isNoContent())
				.andReturn();

		assertThat(requireCookie(logout, "NEXO_SESSION").getMaxAge()).isZero();
		assertThat(requireLatestNonEmptyCookie(logout, "XSRF-TOKEN").getValue()).isNotBlank();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isZero();
		mockMvc.perform(get("/api/auth/session").secure(true).cookie(authenticatedSession))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldRegisterWithCsrfThenLoginWithoutExposingThePassword() throws Exception {
		CsrfState csrf = getCsrf(null);

		mockMvc.perform(post("/api/auth/register")
						.secure(true)
						.cookie(csrf.session(), csrf.csrf())
						.header("X-XSRF-TOKEN", csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRegistrationJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").isString())
				.andExpect(jsonPath("$.companyId").isString())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM companies", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM company_memberships WHERE role = 'OWNER' AND status = 'ACTIVE'",
				Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT password_hash FROM users WHERE normalized_email = ?",
				String.class,
				EMAIL)).doesNotContain(PASSWORD);

		AuthenticatedState authenticated = login();
		mockMvc.perform(get("/api/auth/session")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.activeCompany.role").value("OWNER"));
	}

	@Test
	void shouldRequireCsrfAndRejectInvalidOrDuplicateRegistration() throws Exception {
		CsrfState csrf = getCsrf(null);
		mockMvc.perform(post("/api/auth/register")
						.secure(true)
						.cookie(csrf.session(), csrf.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRegistrationJson()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		mockMvc.perform(post("/api/auth/register")
						.secure(true)
						.cookie(csrf.session(), csrf.csrf())
						.header("X-XSRF-TOKEN", csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"invalid\",\"password\":\"short\",\"companyName\":\"\",\"defaultCurrency\":\"euro\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REGISTRATION"));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();

		registerUser();
		CsrfState renewed = getCsrf(csrf.session());
		mockMvc.perform(post("/api/auth/register")
						.secure(true)
						.cookie(renewed.session(), renewed.csrf())
						.header("X-XSRF-TOKEN", renewed.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRegistrationJson()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	void shouldRequireExplicitSelectionWithMultipleCompaniesAndAllowSafeSwitching() throws Exception {
		RegistrationResult registration = registerUser();
		UUID secondCompany = addCompanyMembership(registration, "Second Company", "INVENTORY_MANAGER");
		AuthenticatedState authenticated = login();

		mockMvc.perform(get("/api/auth/session")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeCompany").doesNotExist());

		mockMvc.perform(get("/api/auth/companies")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].role").value("OWNER"))
				.andExpect(jsonPath("$[1].role").value("INVENTORY_MANAGER"));

		selectCompany(authenticated, registration.companyId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(registration.companyId().toString()));
		selectCompany(authenticated, secondCompany)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(secondCompany.toString()))
				.andExpect(jsonPath("$.role").value("INVENTORY_MANAGER"));

		mockMvc.perform(get("/api/auth/session")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeCompany.id").value(secondCompany.toString()));
	}

	@Test
	void shouldRejectForeignCompanySelectionAndRequireCsrf() throws Exception {
		registerUser();
		UUID foreignCompany = addCompany("Foreign Company");
		AuthenticatedState authenticated = login();

		mockMvc.perform(put("/api/auth/session/company")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"companyId\":\"" + foreignCompany + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		selectCompany(authenticated, foreignCompany)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("COMPANY_ACCESS_DENIED"));
	}

	@Test
	void shouldStopAuthorizingSuspendedMembershipOrCompanyImmediately() throws Exception {
		RegistrationResult registration = registerUser();
		AuthenticatedState authenticated = login();
		jdbcTemplate.update(
				"UPDATE company_memberships SET status = 'SUSPENDED' WHERE id = ?",
				registration.membershipId().value());

		mockMvc.perform(get("/api/auth/session")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeCompany").doesNotExist());
		selectCompany(authenticated, registration.companyId())
				.andExpect(status().isForbidden());

		jdbcTemplate.update(
				"UPDATE company_memberships SET status = 'ACTIVE' WHERE id = ?",
				registration.membershipId().value());
		selectCompany(authenticated, registration.companyId()).andExpect(status().isOk());
		jdbcTemplate.update("UPDATE companies SET status = 'SUSPENDED' WHERE id = ?", registration.companyId());

		mockMvc.perform(get("/api/auth/session")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeCompany").doesNotExist());
		selectCompany(authenticated, registration.companyId())
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldDenyAuthenticatedRoutesOutsideTheExplicitAllowlist() throws Exception {
		registerUser();
		AuthenticatedState authenticated = login();

		mockMvc.perform(get("/api/not-authorized")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldIsolateSecurityCompanyContextByAuthenticatedMembership() {
		RegistrationResult registration = registerUser();
		UUID foreignCompany = addCompany("Foreign Company");
		MockHttpServletRequest request = new MockHttpServletRequest();
		activeCompanySession.select(request, registration.companyId());
		var principal = new AuthenticatedUserPrincipal(registration.userId().value(), EMAIL);
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of()));
		SecurityCompanyContext context = new SecurityCompanyContext(
				request, activeCompanySession, companyAccessService);

		assertThat(context.requireCompanyId()).isEqualTo(registration.companyId());

		MockHttpServletRequest foreignRequest = new MockHttpServletRequest();
		activeCompanySession.select(foreignRequest, foreignCompany);
		SecurityCompanyContext foreignContext = new SecurityCompanyContext(
				foreignRequest, activeCompanySession, companyAccessService);
		org.assertj.core.api.Assertions.assertThatThrownBy(foreignContext::requireCompanyId)
				.isInstanceOf(CompanyAccessDeniedException.class);
		assertThat(activeCompanySession.find(foreignRequest)).isEmpty();
	}

	@Test
	void shouldRejectInvalidCredentialsWithoutRevealingWhichValueFailed() throws Exception {
		registerUser();
		CsrfState csrf = getCsrf(null);

		mockMvc.perform(post("/api/auth/login")
						.secure(true)
						.cookie(csrf.session(), csrf.csrf())
						.header("X-XSRF-TOKEN", csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"owner@example.com\",\"password\":\"wrong password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("Las credenciales no son válidas."));
	}

	@ParameterizedTest
	@MethodSource("inactiveStatuses")
	void shouldRejectLoginForInactiveUsers(String status) throws Exception {
		RegistrationResult registration = registerUser();
		jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", status, registration.userId().value());
		CsrfState csrf = getCsrf(null);

		mockMvc.perform(post("/api/auth/login")
						.secure(true)
						.cookie(csrf.session(), csrf.csrf())
						.header("X-XSRF-TOKEN", csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validLoginJson()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@ParameterizedTest
	@MethodSource("inactiveStatuses")
	void shouldInvalidateExistingSessionWhenUserBecomesInactive(String status) throws Exception {
		RegistrationResult registration = registerUser();
		AuthenticatedState authenticated = login();
		jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", status, registration.userId().value());

		mockMvc.perform(get("/api/auth/session")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isZero();
	}

	@Test
	void shouldRejectMissingOrInvalidCsrfIncludingMultipart() throws Exception {
		registerUser();
		AuthenticatedState authenticated = login();
		MockMultipartFile csv = new MockMultipartFile(
				"file", "clientes.csv", "text/csv", "id,name\n1,Ana".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/csv/preview")
						.file(csv)
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		mockMvc.perform(post("/api/auth/logout")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf())
						.header("X-XSRF-TOKEN", "invalid"))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldDenyAnonymousBusinessEndpointsAndAllowAuthorizedCompanyHistory() throws Exception {
		mockMvc.perform(get("/api/auth/session").secure(true))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
		mockMvc.perform(get("/api/etl/executions").secure(true))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/products").secure(true))
				.andExpect(status().isUnauthorized());

		registerUser();
		AuthenticatedState authenticated = login();
		mockMvc.perform(get("/api/etl/executions")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void shouldReadOnlyProductsForTheSelectedCompanyWithEveryCompanyRole() throws Exception {
		RegistrationResult registration = registerUser();
		UUID adminCompany = addCompanyMembership(registration, "Admin Company", "ADMIN");
		UUID inventoryCompany = addCompanyMembership(
				registration, "Inventory Company", "INVENTORY_MANAGER");
		UUID viewerCompany = addCompanyMembership(registration, "Viewer Company", "VIEWER");
		UUID ownerProduct = addProduct(registration.companyId(), "OWNER-1", "Owner product");
		UUID adminProduct = addProduct(adminCompany, "ADMIN-1", "Admin product");
		addProduct(inventoryCompany, "INVENTORY-1", "Inventory product");
		addProduct(viewerCompany, "VIEWER-1", "Viewer product");
		AuthenticatedState authenticated = login();

		for (UUID companyId : java.util.List.of(
				registration.companyId(), adminCompany, inventoryCompany, viewerCompany)) {
			selectCompany(authenticated, companyId).andExpect(status().isOk());
			mockMvc.perform(get("/api/products")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(1));
		}

		selectCompany(authenticated, registration.companyId()).andExpect(status().isOk());
		mockMvc.perform(get("/api/products/" + ownerProduct)
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Owner product"))
				.andExpect(jsonPath("$.price").value("10.0000"));
		mockMvc.perform(get("/api/products/" + adminProduct)
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void shouldCreateUpdateDeactivateAndReactivateAProduct() throws Exception {
		RegistrationResult registration = registerUser();
		AuthenticatedState authenticated = login();

		mockMvc.perform(post("/api/products")
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content(productJson("SKU-1", "Product", "10.2500")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.currency").value("EUR"))
				.andExpect(jsonPath("$.price").value("10.2500"))
				.andExpect(jsonPath("$.version").value(0));
		UUID productId = jdbcTemplate.queryForObject(
				"SELECT id FROM products WHERE company_id = ? AND normalized_sku = 'SKU-1'",
				UUID.class,
				registration.companyId());

		mockMvc.perform(post("/api/products")
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content(productJson(" sku-1 ", "Duplicate", "1.0000")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PRODUCT_SKU_ALREADY_EXISTS"));

		mockMvc.perform(post("/api/products")
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content(productJson("BAD-PRICE", "Invalid", "-1.00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PRODUCT_REQUEST"));

		mockMvc.perform(put("/api/products/" + productId)
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateProductJson(0, "SKU-2", "Updated product", "20.5000")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated product"))
				.andExpect(jsonPath("$.version").value(1));

		mockMvc.perform(put("/api/products/" + productId)
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateProductJson(0, "SKU-3", "Stale", "30.0000")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PRODUCT_VERSION_CONFLICT"));

		mockMvc.perform(patch("/api/products/" + productId + "/status")
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"active\":false,\"expectedVersion\":1}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false))
				.andExpect(jsonPath("$.version").value(2));

		mockMvc.perform(patch("/api/products/" + productId + "/status")
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"active\":true,\"expectedVersion\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.version").value(3));
	}

	@Test
	void shouldEnforceProductWriteRolesAndCompanyIsolation() throws Exception {
		RegistrationResult registration = registerUser();
		UUID adminCompany = addCompanyMembership(registration, "Admin Company", "ADMIN");
		UUID inventoryCompany = addCompanyMembership(
				registration, "Inventory Company", "INVENTORY_MANAGER");
		UUID viewerCompany = addCompanyMembership(registration, "Viewer Company", "VIEWER");
		UUID ownerProduct = addProduct(registration.companyId(), "OWNER-1", "Owner product");
		AuthenticatedState authenticated = login();

		selectCompany(authenticated, adminCompany).andExpect(status().isOk());
		mockMvc.perform(post("/api/products")
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content(productJson("ADMIN-1", "Admin product", "5.0000")))
				.andExpect(status().isCreated());
		mockMvc.perform(put("/api/products/" + ownerProduct)
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.header("X-XSRF-TOKEN", authenticated.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateProductJson(0, "OWNER-2", "Foreign update", "5.0000")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

		for (UUID readOnlyCompany : java.util.List.of(inventoryCompany, viewerCompany)) {
			selectCompany(authenticated, readOnlyCompany).andExpect(status().isOk());
			mockMvc.perform(post("/api/products")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf())
						.header("X-XSRF-TOKEN", authenticated.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson("DENIED-1", "Denied", "1.0000")))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("COMPANY_PERMISSION_DENIED"));
		}
	}

	@Test
	void shouldRejectProductWritesWithoutAuthenticationOrValidCsrf() throws Exception {
		CsrfState anonymous = getCsrf(null);
		mockMvc.perform(post("/api/products")
					.secure(true)
					.cookie(anonymous.session(), anonymous.csrf())
					.header("X-XSRF-TOKEN", anonymous.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content(productJson("SKU-1", "Product", "1.0000")))
				.andExpect(status().isUnauthorized());

		registerUser();
		AuthenticatedState authenticated = login();
		mockMvc.perform(post("/api/products")
					.secure(true)
					.cookie(authenticated.session(), authenticated.csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(productJson("SKU-1", "Product", "1.0000")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void shouldTreatExpiredJdbcSessionAsUnauthenticated() throws Exception {
		registerUser();
		AuthenticatedState authenticated = login();
		jdbcTemplate.update("UPDATE SPRING_SESSION SET LAST_ACCESS_TIME = 0, EXPIRY_TIME = 0");

		mockMvc.perform(get("/api/auth/session")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
				.andExpect(status().isUnauthorized());
	}

	private RegistrationResult registerUser() {
		return registrationService.register(new RegisterUserCommand(
				EMAIL, PASSWORD, "Security Test Company", new CurrencyCode("EUR")));
	}

	private AuthenticatedState login() throws Exception {
		return login(EMAIL, PASSWORD);
	}

	private AuthenticatedState login(String email, String password) throws Exception {
		CsrfState anonymous = getCsrf(null);
		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.secure(true)
						.cookie(anonymous.session(), anonymous.csrf())
						.header("X-XSRF-TOKEN", anonymous.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		Cookie session = requireCookie(login, "NEXO_SESSION");
		CsrfState renewed = getCsrf(session);
		return new AuthenticatedState(session, renewed.csrf(), renewed.token());
	}

	private void downloadCsv(AuthenticatedState authenticated, String fileName) throws Exception {
		MockMultipartFile csv = new MockMultipartFile(
				"file",
				fileName,
				"text/csv",
				"id,email\n1,ana@example.com".getBytes(StandardCharsets.UTF_8));
		mockMvc.perform(multipart("/api/csv/transform/remove-column/download")
						.file(csv)
						.param("column", "email")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf())
						.header("X-XSRF-TOKEN", authenticated.token()))
				.andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.ResultActions selectCompany(
			AuthenticatedState authenticated,
			UUID companyId) throws Exception {
		return mockMvc.perform(put("/api/auth/session/company")
				.secure(true)
				.cookie(authenticated.session(), authenticated.csrf())
				.header("X-XSRF-TOKEN", authenticated.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"companyId\":\"" + companyId + "\"}"));
	}

	private UUID addCompanyMembership(RegistrationResult registration, String name, String role) {
		UUID companyId = addCompany(name);
		jdbcTemplate.update("""
				INSERT INTO company_memberships
				(id, user_id, company_id, role, status, version, created_at, updated_at)
				VALUES (?, ?, ?, ?, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", UUID.randomUUID(), registration.userId().value(), companyId, role);
		return companyId;
	}

	private UUID addCompany(String name) {
		UUID companyId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO companies (id, name, default_currency, status, created_at, updated_at)
				VALUES (?, ?, 'EUR', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", companyId, name);
		return companyId;
	}

	private UUID addProduct(UUID companyId, String sku, String name) {
		UUID productId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO products
				(id, company_id, sku, normalized_sku, name, description, category, price,
				 currency, unit_of_measure, active, version, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, NULL, 'Test', 10.0000, 'EUR', 'UNIT', TRUE, 0,
				 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", productId, companyId, sku, sku.toUpperCase(java.util.Locale.ROOT), name);
		return productId;
	}

	private static String productJson(String sku, String name, String price) {
		return """
				{"sku":"%s","name":"%s","description":null,"category":"Test",
				 "price":"%s","unitOfMeasure":"UNIT"}
				""".formatted(sku, name, price);
	}

	private static String updateProductJson(long version, String sku, String name, String price) {
		return """
				{"expectedVersion":%d,"sku":"%s","name":"%s","description":null,
				 "category":"Test","price":"%s","unitOfMeasure":"UNIT"}
				""".formatted(version, sku, name, price);
	}

	private CsrfState getCsrf(Cookie session) throws Exception {
		var request = get("/api/auth/csrf").secure(true);
		if (session != null) {
			request.cookie(session);
		}
		MvcResult result = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
				.andReturn();
		Cookie resultSession = result.getResponse().getCookie("NEXO_SESSION");
		Cookie csrf = result.getResponse().getCookie("XSRF-TOKEN");
		assertThat(csrf).isNotNull();
		return new CsrfState(resultSession == null ? session : resultSession, csrf, csrf.getValue());
	}

	private static Cookie requireCookie(MvcResult result, String name) {
		Cookie cookie = result.getResponse().getCookie(name);
		assertThat(cookie).as("response cookie " + name).isNotNull();
		return cookie;
	}

	private static Cookie requireLatestNonEmptyCookie(MvcResult result, String name) {
		Cookie cookie = Arrays.stream(result.getResponse().getCookies())
				.filter(candidate -> candidate.getName().equals(name))
				.filter(candidate -> candidate.getValue() != null && !candidate.getValue().isBlank())
				.reduce((first, second) -> second)
				.orElse(null);
		assertThat(cookie).as("non-empty response cookie " + name).isNotNull();
		return cookie;
	}

	private static String validLoginJson() {
		return "{\"email\":\"owner@example.com\",\"password\":\"correct horse battery staple\"}";
	}

	private static String validRegistrationJson() {
		return """
				{"email":"owner@example.com","password":"correct horse battery staple",\
				"companyName":"Security Test Company","defaultCurrency":"EUR"}
				""";
	}

	private static Stream<String> inactiveStatuses() {
		return Stream.of("LOCKED", "DISABLED");
	}

	private record CsrfState(Cookie session, Cookie csrf, String token) {
	}

	private record AuthenticatedState(Cookie session, Cookie csrf, String token) {
	}
}
