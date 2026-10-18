package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dandragu.saasetl.identity.application.UserRepository;
import com.dandragu.saasetl.identity.domain.UserId;
import com.dandragu.saasetl.identity.domain.UserStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ActiveUserValidationFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;
	private final JsonAuthenticationEntryPoint authenticationEntryPoint;
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	public ActiveUserValidationFilter(
			UserRepository userRepository,
			JsonAuthenticationEntryPoint authenticationEntryPoint) {
		this.userRepository = userRepository;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.isAuthenticated()
				&& authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal
				&& !isActive(principal)) {
			logoutHandler.logout(request, response, authentication);
			authenticationEntryPoint.commence(
					request,
					response,
					new org.springframework.security.authentication.CredentialsExpiredException(
							"Authenticated user is no longer active"));
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isActive(AuthenticatedUserPrincipal principal) {
		return userRepository.findById(new UserId(principal.userId()))
				.map(user -> user.status() == UserStatus.ACTIVE)
				.orElse(false);
	}
}
