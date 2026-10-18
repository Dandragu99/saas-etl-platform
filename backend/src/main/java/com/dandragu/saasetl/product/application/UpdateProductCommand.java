package com.dandragu.saasetl.product.application;

import java.math.BigDecimal;

import com.dandragu.saasetl.product.domain.ProductId;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

public record UpdateProductCommand(
		ProductId productId,
		long expectedVersion,
		String sku,
		String name,
		String description,
		String category,
		BigDecimal price,
		UnitOfMeasure unitOfMeasure) {
}
