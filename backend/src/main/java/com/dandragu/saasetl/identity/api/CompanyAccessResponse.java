package com.dandragu.saasetl.identity.api;

import java.util.UUID;

import com.dandragu.saasetl.company.application.CompanyAccess;
import com.dandragu.saasetl.identity.domain.MembershipRole;

public record CompanyAccessResponse(
		UUID id,
		String name,
		String defaultCurrency,
		MembershipRole role) {

	public static CompanyAccessResponse from(CompanyAccess access) {
		return new CompanyAccessResponse(
				access.company().id(),
				access.company().name(),
				access.company().defaultCurrency().value(),
				access.role());
	}
}
