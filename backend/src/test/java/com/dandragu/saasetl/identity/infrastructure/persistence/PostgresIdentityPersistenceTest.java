package com.dandragu.saasetl.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.identity.application.RegisterUserCommand;
import com.dandragu.saasetl.identity.application.RegistrationResult;
import com.dandragu.saasetl.identity.application.RegistrationService;
import com.dandragu.saasetl.identity.domain.CompanyMembership;
import com.dandragu.saasetl.identity.domain.EmailAddress;
import com.dandragu.saasetl.identity.domain.MembershipId;
import com.dandragu.saasetl.identity.domain.User;
import com.dandragu.saasetl.identity.domain.UserId;

@SpringBootTest
@Testcontainers
@Transactional
class PostgresIdentityPersistenceTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private JpaUserRepository userRepository;

	@Autowired
	private JpaCompanyMembershipRepository membershipRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private RegistrationService registrationService;

	@Test
	void shouldRegisterUserCompanyAndOwnerMembershipInOneTransaction() {
		RegistrationResult result = registrationService.register(new RegisterUserCommand(
				"owner@example.com",
				"correct horse battery staple",
				"Registered Company",
				new CurrencyCode("EUR")));

		User user = userRepository.findById(result.userId()).orElseThrow();
		Company company = companyRepository.findById(result.companyId()).orElseThrow();
		CompanyMembership membership = membershipRepository.findById(result.membershipId()).orElseThrow();

		assertThat(user.email()).isEqualTo(new EmailAddress("owner@example.com"));
		assertThat(user.passwordHash()).startsWith("$2");
		assertThat(company.name()).isEqualTo("Registered Company");
		assertThat(membership.userId()).isEqualTo(result.userId());
		assertThat(membership.companyId()).isEqualTo(result.companyId());
	}

	@Test
	void shouldPersistIdentityAndKeepMembershipsIsolatedByUserAndCompany() {
		Instant now = Instant.parse("2026-10-18T12:00:00Z");
		UserId firstUserId = new UserId(UUID.randomUUID());
		UserId secondUserId = new UserId(UUID.randomUUID());
		UUID firstCompanyId = UUID.randomUUID();
		UUID secondCompanyId = UUID.randomUUID();

		userRepository.save(User.register(firstUserId, new EmailAddress("first@example.com"), "hash-1", now));
		userRepository.save(User.register(secondUserId, new EmailAddress("second@example.com"), "hash-2", now));
		companyRepository.save(Company.create(firstCompanyId, "First Company", new CurrencyCode("EUR"), now));
		companyRepository.save(Company.create(secondCompanyId, "Second Company", new CurrencyCode("USD"), now));
		membershipRepository.save(CompanyMembership.owner(
				new MembershipId(UUID.randomUUID()), firstUserId, firstCompanyId, now));
		membershipRepository.save(CompanyMembership.owner(
				new MembershipId(UUID.randomUUID()), firstUserId, secondCompanyId, now));
		membershipRepository.save(CompanyMembership.owner(
				new MembershipId(UUID.randomUUID()), secondUserId, secondCompanyId, now));

		assertThat(userRepository.findByEmail(new EmailAddress(" FIRST@EXAMPLE.COM ")))
				.map(User::id)
				.contains(firstUserId);
		assertThat(membershipRepository.findByUserId(firstUserId)).hasSize(2);
		assertThat(membershipRepository.findByUserId(secondUserId)).hasSize(1);
		assertThat(membershipRepository.findByUserIdAndCompanyId(firstUserId, firstCompanyId)).isPresent();
		assertThat(membershipRepository.findByUserIdAndCompanyId(secondUserId, firstCompanyId)).isEmpty();
	}
}
