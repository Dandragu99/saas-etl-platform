package com.dandragu.saasetl.identity.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.identity.infrastructure.security.AuthenticatedUserPrincipal;
import com.dandragu.saasetl.company.application.CompanyAccessDeniedException;
import com.dandragu.saasetl.company.application.CompanyAccessService;
import com.dandragu.saasetl.company.infrastructure.context.ActiveCompanySession;
import com.dandragu.saasetl.identity.domain.UserId;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth/session")
public class AuthSessionController {
	private final ActiveCompanySession activeCompanySession;
	private final CompanyAccessService companyAccessService;

	public AuthSessionController(
			ActiveCompanySession activeCompanySession,
			CompanyAccessService companyAccessService) {
		this.activeCompanySession = activeCompanySession;
		this.companyAccessService = companyAccessService;
	}

	@GetMapping
	public AuthSessionResponse getSession(Authentication authentication, HttpServletRequest request) {
		AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
		CompanyAccessResponse activeCompany = activeCompanySession.find(request)
				.map(companyId -> resolveActiveCompany(request, principal, companyId))
				.orElse(null);
		return new AuthSessionResponse(principal.userId(), principal.email(), activeCompany);
	}

	private CompanyAccessResponse resolveActiveCompany(
			HttpServletRequest request,
			AuthenticatedUserPrincipal principal,
			java.util.UUID companyId) {
		try {
			return CompanyAccessResponse.from(companyAccessService.requireActiveAccess(
					new UserId(principal.userId()), companyId));
		} catch (CompanyAccessDeniedException exception) {
			activeCompanySession.clear(request);
			return null;
		}
	}
}
