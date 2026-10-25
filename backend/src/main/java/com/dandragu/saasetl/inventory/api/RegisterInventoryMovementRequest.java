package com.dandragu.saasetl.inventory.api;

import com.dandragu.saasetl.inventory.domain.InventoryMovementType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterInventoryMovementRequest(
		@NotBlank String productId,
		@NotNull InventoryMovementType type,
		@NotBlank String quantity) {
}
