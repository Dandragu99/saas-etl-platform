package com.dandragu.saasetl.product.application;

import java.util.List;

import com.dandragu.saasetl.product.domain.Product;

public record ProductPage(
		List<Product> content,
		long totalElements,
		int page,
		int size) {

	public ProductPage {
		content = List.copyOf(content);
	}

	public int totalPages() {
		return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
	}
}
