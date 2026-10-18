package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.dandragu.saasetl.common.api.error.ApiErrorResponse;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityJsonResponseWriter {

	private final ObjectMapper objectMapper;

	public SecurityJsonResponseWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void writeError(
			HttpServletRequest request,
			HttpServletResponse response,
			int status,
			String code,
			String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
				code, message, status, request.getRequestURI(), Instant.now()));
	}

	public void write(HttpServletResponse response, int status, Object body) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
