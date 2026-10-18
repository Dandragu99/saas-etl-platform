package com.dandragu.saasetl.company.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Company(
		UUID id,
		String name,
		CurrencyCode defaultCurrency,
		CompanyStatus status,
		Instant createdAt,
		Instant updatedAt) {

	public static final int MAX_NAME_LENGTH = 150;

	public Company {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(defaultCurrency, "defaultCurrency must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		name = name.trim();
		if (name.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("name must not exceed " + MAX_NAME_LENGTH + " characters");
		}
		if (updatedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("updatedAt must not be before createdAt");
		}
	}

	public static Company create(UUID id, String name, CurrencyCode defaultCurrency, Instant now) {
		return new Company(id, name, defaultCurrency, CompanyStatus.ACTIVE, now, now);
	}
}
