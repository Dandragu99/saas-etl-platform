package com.dandragu.saasetl.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class IdentityDomainTest {

	@Test
	void shouldNormalizeEmailAndCreateActiveUser() {
		Instant now = Instant.parse("2026-10-18T12:00:00Z");
		User user = User.register(
				new UserId(UUID.randomUUID()),
				new EmailAddress(" Owner@Example.COM "),
				"a-valid-password-hash",
				now);

		assertThat(user.email().value()).isEqualTo("owner@example.com");
		assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.version()).isZero();
	}

	@Test
	void shouldRejectInvalidEmail() {
		assertThatThrownBy(() -> new EmailAddress("not-an-email"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldCreateAnActiveOwnerMembership() {
		Instant now = Instant.parse("2026-10-18T12:00:00Z");
		CompanyMembership membership = CompanyMembership.owner(
				new MembershipId(UUID.randomUUID()),
				new UserId(UUID.randomUUID()),
				UUID.randomUUID(),
				now);

		assertThat(membership.role()).isEqualTo(MembershipRole.OWNER);
		assertThat(membership.status()).isEqualTo(MembershipStatus.ACTIVE);
	}
}
