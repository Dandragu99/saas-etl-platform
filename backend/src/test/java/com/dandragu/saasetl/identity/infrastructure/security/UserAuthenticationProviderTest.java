package com.dandragu.saasetl.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.dandragu.saasetl.identity.application.PasswordHasher;
import com.dandragu.saasetl.identity.application.UserRepository;
import com.dandragu.saasetl.identity.domain.EmailAddress;
import com.dandragu.saasetl.identity.domain.User;
import com.dandragu.saasetl.identity.domain.UserId;
import com.dandragu.saasetl.identity.domain.UserStatus;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationProviderTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordHasher passwordHasher;

	private UserAuthenticationProvider provider;

	@BeforeEach
	void setUp() {
		provider = new UserAuthenticationProvider(userRepository, passwordHasher);
	}

	@Test
	void shouldReturnSafePrincipalForValidCredentials() {
		User user = activeUser();
		when(userRepository.findByEmail(new EmailAddress("owner@example.com"))).thenReturn(Optional.of(user));
		when(passwordHasher.matches("valid password", user.passwordHash())).thenReturn(true);

		var result = provider.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(" OWNER@example.com ", "valid password"));

		assertThat(result.isAuthenticated()).isTrue();
		assertThat(result.getCredentials()).isNull();
		assertThat(result.getPrincipal()).isEqualTo(
				new AuthenticatedUserPrincipal(user.id().value(), user.email().value()));
	}

	@Test
	void shouldUseSameGenericFailureForUnknownWrongOrInactiveUsers() {
		EmailAddress email = new EmailAddress("owner@example.com");
		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
		assertInvalid("owner@example.com", "password");

		User active = activeUser();
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(active));
		when(passwordHasher.matches("wrong password", active.passwordHash())).thenReturn(false);
		assertInvalid("owner@example.com", "wrong password");

		User disabled = new User(active.id(), active.email(), active.passwordHash(), UserStatus.DISABLED,
				active.version(), active.createdAt(), active.updatedAt(), active.lastLoginAt());
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(disabled));
		assertInvalid("owner@example.com", "password");
	}

	private void assertInvalid(String email, String password) {
		assertThatThrownBy(() -> provider.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(email, password)))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("Invalid credentials");
	}

	private User activeUser() {
		Instant now = Instant.parse("2026-10-18T12:00:00Z");
		return User.register(new UserId(UUID.randomUUID()), new EmailAddress("owner@example.com"), "hash", now);
	}
}
