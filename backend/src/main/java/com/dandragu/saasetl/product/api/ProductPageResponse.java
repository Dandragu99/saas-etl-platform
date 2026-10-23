package com.dandragu.saasetl.product.api;

import java.util.List;

import com.dandragu.saasetl.product.application.ProductPage;

public record ProductPageResponse(
		List<ProductResponse> content,
		long totalElements,
		int totalPages,
		int page,
		int size) {

	public ProductPageResponse {
		content = List.copyOf(content);
	}

	public static ProductPageResponse from(ProductPage result) {
		return new ProductPageResponse(
				result.content().stream().map(ProductResponse::from).toList(),
				result.totalElements(),
				result.totalPages(),
				result.page(),
				result.size());
	}
}
