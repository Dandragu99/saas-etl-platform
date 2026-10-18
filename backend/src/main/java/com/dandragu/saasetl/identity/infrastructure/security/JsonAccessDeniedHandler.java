package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityJsonResponseWriter writer;

	public JsonAccessDeniedHandler(SecurityJsonResponseWriter writer) {
		this.writer = writer;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException exception) throws IOException, ServletException {
		writer.writeError(request, response, HttpStatus.FORBIDDEN.value(),
				"ACCESS_DENIED", "No tienes permiso para realizar esta operación.");
	}
}
