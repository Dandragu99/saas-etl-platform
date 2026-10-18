package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import com.dandragu.saasetl.identity.api.AuthSessionResponse;
import com.dandragu.saasetl.identity.api.CompanyAccessResponse;
import com.dandragu.saasetl.company.application.CompanyAccessService;
import com.dandragu.saasetl.company.infrastructure.context.ActiveCompanySession;
import com.dandragu.saasetl.identity.domain.UserId;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JsonAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final SecurityJsonResponseWriter writer;
	private final CsrfTokenRepository csrfTokenRepository;
	private final CompanyAccessService companyAccessService;
	private final ActiveCompanySession activeCompanySession;

	public JsonAuthenticationSuccessHandler(
			SecurityJsonResponseWriter writer,
			CsrfTokenRepository csrfTokenRepository,
			CompanyAccessService companyAccessService,
			ActiveCompanySession activeCompanySession) {
		this.writer = writer;
		this.csrfTokenRepository = csrfTokenRepository;
		this.companyAccessService = companyAccessService;
		this.activeCompanySession = activeCompanySession;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		CsrfToken token = csrfTokenRepository.generateToken(request);
		csrfTokenRepository.saveToken(token, request, response);
		AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
		var companies = companyAccessService.findAccessibleCompanies(new UserId(principal.userId()));
		CompanyAccessResponse activeCompany = null;
		if (companies.size() == 1) {
			var access = companies.getFirst();
			activeCompanySession.select(request, access.company().id());
			activeCompany = CompanyAccessResponse.from(access);
		} else {
			activeCompanySession.clear(request);
		}
		writer.write(response, HttpStatus.OK.value(),
				new AuthSessionResponse(principal.userId(), principal.email(), activeCompany));
	}
}
