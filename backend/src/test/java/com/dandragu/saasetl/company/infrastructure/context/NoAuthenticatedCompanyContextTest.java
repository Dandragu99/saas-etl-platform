package com.dandragu.saasetl.company.infrastructure.context;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.company.application.CompanyContextUnavailableException;

class NoAuthenticatedCompanyContextTest {

	private final NoAuthenticatedCompanyContext context = new NoAuthenticatedCompanyContext();

	@Test
	void shouldFailClosedWhileNoAuthenticatedCompanyExists() {
		assertThatThrownBy(context::requireCompanyId)
				.isInstanceOf(CompanyContextUnavailableException.class)
				.hasMessage("No authenticated company context is available.");
	}
}
