package com.dandragu.saasetl.identity.api;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.company.application.CompanyAccessDeniedException;
import com.dandragu.saasetl.company.application.CompanyAccessService;
import com.dandragu.saasetl.company.infrastructure.context.ActiveCompanySession;
import com.dandragu.saasetl.identity.domain.UserId;
import com.dandragu.saasetl.identity.infrastructure.security.AuthenticatedUserPrincipal;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthenticatedCompanyController {

	private final CompanyAccessService companyAccessService;
	private final ActiveCompanySession activeCompanySession;

	public AuthenticatedCompanyController(
			CompanyAccessService companyAccessService,
			ActiveCompanySession activeCompanySession) {
		this.companyAccessService = companyAccessService;
		this.activeCompanySession = activeCompanySession;
	}

	@GetMapping("/companies")
	public List<CompanyAccessResponse> findCompanies(Authentication authentication) {
		var principal = principal(authentication);
		return companyAccessService.findAccessibleCompanies(new UserId(principal.userId())).stream()
				.map(CompanyAccessResponse::from)
				.toList();
	}

	@PutMapping("/session/company")
	public CompanyAccessResponse selectCompany(
			@RequestBody SelectCompanyRequest selection,
			Authentication authentication,
			HttpServletRequest request) {
		if (selection == null || selection.companyId() == null) {
			throw new CompanyAccessDeniedException();
		}
		var principal = principal(authentication);
		var access = companyAccessService.requireActiveAccess(
				new UserId(principal.userId()), selection.companyId());
		activeCompanySession.select(request, access.company().id());
		return CompanyAccessResponse.from(access);
	}

	private AuthenticatedUserPrincipal principal(Authentication authentication) {
		if (authentication == null
				|| !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
			throw new CompanyAccessDeniedException();
		}
		return principal;
	}
}
