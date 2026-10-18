package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import com.dandragu.saasetl.identity.api.AuthSessionResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JsonAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final SecurityJsonResponseWriter writer;
	private final CsrfTokenRepository csrfTokenRepository;

	public JsonAuthenticationSuccessHandler(
			SecurityJsonResponseWriter writer,
			CsrfTokenRepository csrfTokenRepository) {
		this.writer = writer;
		this.csrfTokenRepository = csrfTokenRepository;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		CsrfToken token = csrfTokenRepository.generateToken(request);
		csrfTokenRepository.saveToken(token, request, response);
		AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
		writer.write(response, HttpStatus.OK.value(),
				new AuthSessionResponse(principal.userId(), principal.email()));
	}
}
