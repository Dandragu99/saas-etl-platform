package com.dandragu.saasetl.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dandragu.saasetl.company.domain.CurrencyCode;
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
	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM SPRING_SESSION_ATTRIBUTES");
		jdbcTemplate.update("DELETE FROM SPRING_SESSION");
		jdbcTemplate.update("DELETE FROM company_memberships");
		jdbcTemplate.update("DELETE FROM products");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM companies");
	}

	@Test
	void shouldCompleteCsrfLoginMultipartSessionAndLogoutCycle() throws Exception {
		registerUser();
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
	void shouldDenyAnonymousBusinessEndpointsAndAllGlobalHistoryAccess() throws Exception {
		mockMvc.perform(get("/api/auth/session").secure(true))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
		mockMvc.perform(get("/api/etl/executions").secure(true))
				.andExpect(status().isUnauthorized());

		registerUser();
		AuthenticatedState authenticated = login();
		mockMvc.perform(get("/api/etl/executions")
						.secure(true)
						.cookie(authenticated.session(), authenticated.csrf()))
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
		CsrfState anonymous = getCsrf(null);
		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.secure(true)
						.cookie(anonymous.session(), anonymous.csrf())
						.header("X-XSRF-TOKEN", anonymous.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validLoginJson()))
				.andExpect(status().isOk())
				.andReturn();
		Cookie session = requireCookie(login, "NEXO_SESSION");
		CsrfState renewed = getCsrf(session);
		return new AuthenticatedState(session, renewed.csrf(), renewed.token());
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

	private static Stream<String> inactiveStatuses() {
		return Stream.of("LOCKED", "DISABLED");
	}

	private record CsrfState(Cookie session, Cookie csrf, String token) {
	}

	private record AuthenticatedState(Cookie session, Cookie csrf, String token) {
	}
}
