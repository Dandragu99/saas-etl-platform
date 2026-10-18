package com.dandragu.saasetl.identity.infrastructure.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.dandragu.saasetl.identity.application.PasswordHasher;
import com.dandragu.saasetl.identity.application.UserRepository;
import com.dandragu.saasetl.identity.domain.EmailAddress;
import com.dandragu.saasetl.identity.domain.User;
import com.dandragu.saasetl.identity.domain.UserStatus;

@Component
public class UserAuthenticationProvider implements AuthenticationProvider {

	private static final BadCredentialsException INVALID_CREDENTIALS =
			new BadCredentialsException("Invalid credentials");

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;
	private final String dummyPasswordHash;

	public UserAuthenticationProvider(UserRepository userRepository, PasswordHasher passwordHasher) {
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
		this.dummyPasswordHash = passwordHasher.hash("not-a-real-user-password");
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		EmailAddress email;
		try {
			email = new EmailAddress(authentication.getName());
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw invalidCredentials();
		}

		var user = userRepository.findByEmail(email);
		String rawPassword = authentication.getCredentials() instanceof String password ? password : "";
		String passwordHash = user.map(User::passwordHash).orElse(dummyPasswordHash);
		boolean passwordMatches = passwordHasher.matches(rawPassword, passwordHash);
		if (user.isEmpty() || user.get().status() != UserStatus.ACTIVE || !passwordMatches) {
			throw invalidCredentials();
		}
		User authenticatedUser = user.orElseThrow();

		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				authenticatedUser.id().value(), authenticatedUser.email().value());
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of());
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}

	private static BadCredentialsException invalidCredentials() {
		return new BadCredentialsException(INVALID_CREDENTIALS.getMessage());
	}
}
