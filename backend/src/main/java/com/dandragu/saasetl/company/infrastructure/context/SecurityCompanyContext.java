package com.dandragu.saasetl.company.infrastructure.context;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.dandragu.saasetl.company.application.CompanyAccess;
import com.dandragu.saasetl.company.application.CompanyAccessDeniedException;
import com.dandragu.saasetl.company.application.CompanyAccessService;
import com.dandragu.saasetl.company.application.CompanyContext;
import com.dandragu.saasetl.company.application.CompanyContextUnavailableException;
import com.dandragu.saasetl.identity.domain.UserId;
import com.dandragu.saasetl.identity.infrastructure.security.AuthenticatedUserPrincipal;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityCompanyContext implements CompanyContext {

	private static final String VALIDATED_ACCESS = SecurityCompanyContext.class.getName() + ".validatedAccess";

	private final HttpServletRequest request;
	private final ActiveCompanySession activeCompanySession;
	private final CompanyAccessService companyAccessService;

	public SecurityCompanyContext(
			HttpServletRequest request,
			ActiveCompanySession activeCompanySession,
			CompanyAccessService companyAccessService) {
		this.request = request;
		this.activeCompanySession = activeCompanySession;
		this.companyAccessService = companyAccessService;
	}

	@Override
	public CompanyAccess requireAccess() {
		Object cached = request.getAttribute(VALIDATED_ACCESS);
		if (cached instanceof CompanyAccess access) {
			return access;
		}

		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
			throw new CompanyAccessDeniedException();
		}
		var companyId = activeCompanySession.find(request)
				.orElseThrow(() -> new CompanyContextUnavailableException(
						"An active company must be selected."));
		try {
			CompanyAccess access = companyAccessService.requireActiveAccess(new UserId(principal.userId()), companyId);
			request.setAttribute(VALIDATED_ACCESS, access);
			return access;
		} catch (CompanyAccessDeniedException exception) {
			activeCompanySession.clear(request);
			throw exception;
		}
	}
}
