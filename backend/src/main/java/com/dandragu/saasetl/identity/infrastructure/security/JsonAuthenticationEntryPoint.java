package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityJsonResponseWriter writer;

	public JsonAuthenticationEntryPoint(SecurityJsonResponseWriter writer) {
		this.writer = writer;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		writer.writeError(request, response, HttpStatus.UNAUTHORIZED.value(),
				"AUTHENTICATION_REQUIRED", "Es necesario iniciar sesión.");
	}
}
