package com.dandragu.saasetl.inventory.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.dandragu.saasetl.inventory.domain.InventoryMovementType;

public record RegisterInventoryMovementCommand(
		UUID productId,
		InventoryMovementType type,
		BigDecimal quantity,
		UUID responsibleUserId,
		String responsibleUserEmail) {
}
