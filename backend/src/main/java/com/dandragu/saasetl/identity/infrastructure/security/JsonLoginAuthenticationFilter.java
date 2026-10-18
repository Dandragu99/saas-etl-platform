package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JsonLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	private final ObjectMapper objectMapper;

	public JsonLoginAuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
		super(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/login"), authenticationManager);
		this.objectMapper = objectMapper;
	}

	@Override
	public Authentication attemptAuthentication(
			HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException, IOException {
		if (!hasJsonContentType(request)) {
			throw new org.springframework.security.authentication.AuthenticationServiceException(
					"Login requires application/json");
		}

		try {
			LoginRequest login = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
			if (login == null) {
				throw new org.springframework.security.authentication.AuthenticationServiceException(
						"Invalid login request");
			}
			String email = login.email() == null ? "" : login.email();
			String password = login.password() == null ? "" : login.password();
			UsernamePasswordAuthenticationToken authentication =
					UsernamePasswordAuthenticationToken.unauthenticated(email, password);
			authentication.setDetails(authenticationDetailsSource.buildDetails(request));
			return getAuthenticationManager().authenticate(authentication);
		} catch (JacksonException exception) {
			throw new org.springframework.security.authentication.AuthenticationServiceException(
					"Invalid login request", exception);
		}
	}

	private boolean hasJsonContentType(HttpServletRequest request) {
		try {
			return request.getContentType() != null
					&& MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(request.getContentType()));
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}
}
