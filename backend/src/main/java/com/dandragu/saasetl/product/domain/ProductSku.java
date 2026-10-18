package com.dandragu.saasetl.product.domain;

import java.util.Locale;

public record ProductSku(String value, String normalizedValue) {

	public static final int MAX_LENGTH = 80;

	public ProductSku(String value) {
		this(normalizeDisplayValue(value), normalize(value));
	}

	public ProductSku {
		String expectedValue = normalizeDisplayValue(value);
		String expectedNormalizedValue = normalize(expectedValue);
		if (!expectedValue.equals(value) || !expectedNormalizedValue.equals(normalizedValue)) {
			throw new IllegalArgumentException("SKU values must be normalized consistently");
		}
	}

	private static String normalizeDisplayValue(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("SKU must not be blank");
		}
		String trimmed = value.trim();
		if (trimmed.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("SKU must not exceed " + MAX_LENGTH + " characters");
		}
		return trimmed;
	}

	private static String normalize(String value) {
		return normalizeDisplayValue(value).toUpperCase(Locale.ROOT);
	}
}
