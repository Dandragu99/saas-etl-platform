package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JsonAuthenticationFailureHandler implements AuthenticationFailureHandler {

	private final SecurityJsonResponseWriter writer;

	public JsonAuthenticationFailureHandler(SecurityJsonResponseWriter writer) {
		this.writer = writer;
	}

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		writer.writeError(request, response, HttpStatus.UNAUTHORIZED.value(),
				"INVALID_CREDENTIALS", "Las credenciales no son válidas.");
	}
}
