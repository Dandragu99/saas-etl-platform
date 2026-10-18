package com.dandragu.saasetl.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.identity.domain.CompanyMembership;
import com.dandragu.saasetl.identity.domain.EmailAddress;
import com.dandragu.saasetl.identity.domain.MembershipRole;
import com.dandragu.saasetl.identity.domain.User;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

	@Mock
	private UserRepository userRepository;
	@Mock
	private CompanyRepository companyRepository;
	@Mock
	private CompanyMembershipRepository membershipRepository;
	@Mock
	private PasswordHasher passwordHasher;

	private RegistrationService service;

	@BeforeEach
	void setUp() {
		service = new RegistrationService(
				userRepository,
				companyRepository,
				membershipRepository,
				passwordHasher,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void shouldCreateUserCompanyAndOwnerMembership() {
		when(passwordHasher.hash("correct horse battery staple")).thenReturn("bcrypt-hash");
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(companyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(membershipRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		RegistrationResult result = service.register(new RegisterUserCommand(
				" Owner@Example.com ",
				"correct horse battery staple",
				"Nexo Demo",
				new CurrencyCode("EUR")));

		ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<Company> company = ArgumentCaptor.forClass(Company.class);
		ArgumentCaptor<CompanyMembership> membership = ArgumentCaptor.forClass(CompanyMembership.class);
		verify(userRepository).save(user.capture());
		verify(companyRepository).save(company.capture());
		verify(membershipRepository).save(membership.capture());

		assertThat(user.getValue().email()).isEqualTo(new EmailAddress("owner@example.com"));
		assertThat(user.getValue().passwordHash()).isEqualTo("bcrypt-hash");
		assertThat(membership.getValue().role()).isEqualTo(MembershipRole.OWNER);
		assertThat(membership.getValue().userId()).isEqualTo(result.userId());
		assertThat(membership.getValue().companyId()).isEqualTo(result.companyId());
		assertThat(company.getValue().id()).isEqualTo(result.companyId());
	}

	@Test
	void shouldRejectDuplicateNormalizedEmailBeforeCreatingAnything() {
		when(userRepository.existsByEmail(new EmailAddress("owner@example.com"))).thenReturn(true);

		assertThatThrownBy(() -> service.register(new RegisterUserCommand(
				"OWNER@example.com", "correct horse battery staple", "Company", new CurrencyCode("EUR"))))
				.isInstanceOf(DuplicateUserEmailException.class);

		verify(passwordHasher, never()).hash(any());
		verify(companyRepository, never()).save(any());
		verify(membershipRepository, never()).save(any());
	}

	@Test
	void shouldRejectPasswordsThatBcryptCannotRepresentWithoutTruncation() {
		String password = "á".repeat(37);

		assertThatThrownBy(() -> service.register(new RegisterUserCommand(
				"owner@example.com", password, "Company", new CurrencyCode("EUR"))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("72 UTF-8 bytes");
	}
}
