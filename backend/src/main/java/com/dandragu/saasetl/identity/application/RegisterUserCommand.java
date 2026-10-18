package com.dandragu.saasetl.identity.application;

import com.dandragu.saasetl.company.domain.CurrencyCode;

public record RegisterUserCommand(
		String email,
		String password,
		String companyName,
		CurrencyCode defaultCurrency) {
}
