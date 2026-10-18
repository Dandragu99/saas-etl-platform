package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JsonLogoutSuccessHandler implements LogoutSuccessHandler {

	private final CsrfTokenRepository csrfTokenRepository;

	public JsonLogoutSuccessHandler(CsrfTokenRepository csrfTokenRepository) {
		this.csrfTokenRepository = csrfTokenRepository;
	}

	@Override
	public void onLogoutSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		CsrfToken token = csrfTokenRepository.generateToken(request);
		csrfTokenRepository.saveToken(token, request, response);
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
}
