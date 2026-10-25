package com.dandragu.saasetl.inventory.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.dandragu.saasetl.inventory.domain.InventoryMovement;
import com.dandragu.saasetl.inventory.domain.StockLevel;

public interface InventoryRepository {

	boolean lockActiveProduct(UUID companyId, UUID productId);

	List<StockLevel> findStock(UUID companyId);

	List<InventoryMovement> findMovements(UUID companyId);

	void addStock(UUID companyId, UUID productId, BigDecimal quantity, Instant occurredAt);

	boolean removeStock(UUID companyId, UUID productId, BigDecimal quantity, Instant occurredAt);

	InventoryMovement saveMovement(InventoryMovement movement);
}
