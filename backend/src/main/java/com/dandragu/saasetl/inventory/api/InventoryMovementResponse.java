package com.dandragu.saasetl.inventory.api;

import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.inventory.domain.InventoryMovement;
import com.dandragu.saasetl.inventory.domain.InventoryMovementType;

public record InventoryMovementResponse(
		UUID id,
		UUID productId,
		String productSku,
		String productName,
		InventoryMovementType type,
		String quantity,
		UUID responsibleUserId,
		String responsibleUserEmail,
		Instant occurredAt) {

	static InventoryMovementResponse from(InventoryMovement movement) {
		return new InventoryMovementResponse(
				movement.id(), movement.productId(), movement.productSku(), movement.productName(),
				movement.type(), movement.quantity().toPlainString(), movement.responsibleUserId(),
				movement.responsibleUserEmail(), movement.occurredAt());
	}
}
