package com.dandragu.saasetl.company.infrastructure.context;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.dandragu.saasetl.company.application.CompanyContext;
import com.dandragu.saasetl.company.application.CompanyContextUnavailableException;

/**
 * Fail-closed context used until authentication and company memberships are implemented.
 */
@Component
public class NoAuthenticatedCompanyContext implements CompanyContext {

	@Override
	public UUID requireCompanyId() {
		throw new CompanyContextUnavailableException(
				"No authenticated company context is available.");
	}
}
