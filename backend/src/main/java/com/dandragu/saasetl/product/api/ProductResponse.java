package com.dandragu.saasetl.product.api;

import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

public record ProductResponse(
		UUID id,
		String sku,
		String name,
		String description,
		String category,
		String price,
		String currency,
		UnitOfMeasure unitOfMeasure,
		boolean active,
		long version,
		Instant createdAt,
		Instant updatedAt) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.id().value(),
				product.sku().value(),
				product.name(),
				product.description(),
				product.category(),
				product.price().toPlainString(),
				product.currency().value(),
				product.unitOfMeasure(),
				product.active(),
				product.version(),
				product.createdAt(),
				product.updatedAt());
	}
}
