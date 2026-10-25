package com.dandragu.saasetl.inventory.api;

import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.inventory.domain.StockLevel;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

public record StockLevelResponse(
		UUID productId,
		String productSku,
		String productName,
		UnitOfMeasure unitOfMeasure,
		String quantity,
		Instant updatedAt) {

	static StockLevelResponse from(StockLevel stock) {
		return new StockLevelResponse(
				stock.productId(), stock.productSku(), stock.productName(), stock.unitOfMeasure(),
				stock.quantity().toPlainString(), stock.updatedAt());
	}
}
