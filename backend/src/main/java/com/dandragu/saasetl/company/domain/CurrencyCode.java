package com.dandragu.saasetl.company.domain;

import java.util.Currency;
import java.util.Locale;

public record CurrencyCode(String value) {

	public CurrencyCode {
		if (value == null) {
			throw new IllegalArgumentException("currency code must not be null");
		}

		value = value.trim().toUpperCase(Locale.ROOT);
		if (value.length() != 3) {
			throw new IllegalArgumentException("currency code must contain three letters");
		}

		try {
			Currency.getInstance(value);
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("currency code must be an ISO 4217 code", exception);
		}
	}
}
