package com.dandragu.saasetl.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InventoryMovement(
		UUID id,
		UUID companyId,
		UUID productId,
		String productSku,
		String productName,
		InventoryMovementType type,
		BigDecimal quantity,
		UUID responsibleUserId,
		String responsibleUserEmail,
		Instant occurredAt) {

	public InventoryMovement {
		Objects.requireNonNull(id);
		Objects.requireNonNull(companyId);
		Objects.requireNonNull(productId);
		Objects.requireNonNull(type);
		Objects.requireNonNull(quantity);
		Objects.requireNonNull(responsibleUserId);
		Objects.requireNonNull(responsibleUserEmail);
		Objects.requireNonNull(occurredAt);
	}
}
