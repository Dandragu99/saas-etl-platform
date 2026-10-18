package com.dandragu.saasetl.product.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.dandragu.saasetl.company.domain.CurrencyCode;

public record Product(
		ProductId id,
		UUID companyId,
		ProductSku sku,
		String name,
		String description,
		String category,
		BigDecimal price,
		CurrencyCode currency,
		UnitOfMeasure unitOfMeasure,
		boolean active,
		long version,
		Instant createdAt,
		Instant updatedAt) {

	public static final int MAX_NAME_LENGTH = 160;
	public static final int MAX_DESCRIPTION_LENGTH = 2000;
	public static final int MAX_CATEGORY_LENGTH = 100;
	public static final int PRICE_SCALE = 4;
	public static final int PRICE_INTEGER_DIGITS = 15;

	public Product {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(companyId, "companyId must not be null");
		Objects.requireNonNull(sku, "sku must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		Objects.requireNonNull(unitOfMeasure, "unitOfMeasure must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");

		name = requiredText(name, "name", MAX_NAME_LENGTH);
		description = optionalText(description, MAX_DESCRIPTION_LENGTH);
		category = requiredText(category, "category", MAX_CATEGORY_LENGTH);
		price = normalizePrice(price);
		if (version < 0) {
			throw new IllegalArgumentException("version must not be negative");
		}
		if (updatedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("updatedAt must not be before createdAt");
		}
	}

	public static Product create(
			ProductId id,
			UUID companyId,
			ProductSku sku,
			String name,
			String description,
			String category,
			BigDecimal price,
			CurrencyCode currency,
			UnitOfMeasure unitOfMeasure,
			Instant now) {
		return new Product(
				id, companyId, sku, name, description, category, price, currency,
				unitOfMeasure, true, 0, now, now);
	}

	public Product update(
			ProductSku newSku,
			String newName,
			String newDescription,
			String newCategory,
			BigDecimal newPrice,
			UnitOfMeasure newUnitOfMeasure,
			Instant now) {
		return new Product(
				id, companyId, newSku, newName, newDescription, newCategory, newPrice, currency,
				newUnitOfMeasure, active, version, createdAt, now);
	}

	public Product changeActive(boolean newActive, Instant now) {
		return new Product(
				id, companyId, sku, name, description, category, price, currency,
				unitOfMeasure, newActive, version, createdAt, now);
	}

	private static String requiredText(String value, String field, int maximumLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		String trimmed = value.trim();
		if (trimmed.length() > maximumLength) {
			throw new IllegalArgumentException(field + " must not exceed " + maximumLength + " characters");
		}
		return trimmed;
	}

	private static String optionalText(String value, int maximumLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() > maximumLength) {
			throw new IllegalArgumentException(
					"description must not exceed " + maximumLength + " characters");
		}
		return trimmed;
	}

	private static BigDecimal normalizePrice(BigDecimal value) {
		if (value == null) {
			throw new IllegalArgumentException("price must not be null");
		}
		if (value.signum() < 0) {
			throw new IllegalArgumentException("price must not be negative");
		}
		if (value.scale() > PRICE_SCALE) {
			throw new IllegalArgumentException("price must not exceed " + PRICE_SCALE + " decimal places");
		}
		int integerDigits = Math.max(0, value.precision() - value.scale());
		if (integerDigits > PRICE_INTEGER_DIGITS) {
			throw new IllegalArgumentException(
					"price must not exceed " + PRICE_INTEGER_DIGITS + " integer digits");
		}
		return value.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
	}
}
