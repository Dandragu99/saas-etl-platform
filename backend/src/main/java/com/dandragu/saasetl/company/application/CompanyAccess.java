package com.dandragu.saasetl.company.application;

import java.util.Objects;

import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.identity.domain.MembershipRole;

public record CompanyAccess(Company company, MembershipRole role) {

	public CompanyAccess {
		Objects.requireNonNull(company, "company must not be null");
		Objects.requireNonNull(role, "role must not be null");
	}
}
