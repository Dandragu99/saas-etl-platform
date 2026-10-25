package com.dandragu.saasetl.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.dandragu.saasetl.product.domain.UnitOfMeasure;

public record StockLevel(
		UUID productId,
		String productSku,
		String productName,
		UnitOfMeasure unitOfMeasure,
		BigDecimal quantity,
		Instant updatedAt) {

	public StockLevel {
		Objects.requireNonNull(productId);
		Objects.requireNonNull(productSku);
		Objects.requireNonNull(productName);
		Objects.requireNonNull(unitOfMeasure);
		Objects.requireNonNull(quantity);
		Objects.requireNonNull(updatedAt);
	}
}
