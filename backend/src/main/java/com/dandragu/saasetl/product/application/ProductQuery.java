package com.dandragu.saasetl.product.application;

public record ProductQuery(
		String search,
		String name,
		String sku,
		String category,
		Boolean active,
		int page,
		int size,
		ProductSort sort,
		ProductSortDirection direction) {

	public static final int DEFAULT_PAGE_SIZE = 20;
	public static final int MAX_PAGE_SIZE = 100;
	public static final ProductSort DEFAULT_SORT = ProductSort.NAME;
	public static final ProductSortDirection DEFAULT_DIRECTION = ProductSortDirection.ASC;

	public ProductQuery {
		search = normalizeFilter(search, "search", 160);
		name = normalizeFilter(name, "name", 160);
		sku = normalizeFilter(sku, "sku", 80);
		category = normalizeFilter(category, "category", 100);
		if (page < 0) {
			throw new IllegalArgumentException("page must not be negative");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		if (sort == null) {
			sort = DEFAULT_SORT;
		}
		if (direction == null) {
			direction = DEFAULT_DIRECTION;
		}
	}

	private static String normalizeFilter(String value, String field, int maximumLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() > maximumLength) {
			throw new IllegalArgumentException(
					field + " filter must not exceed " + maximumLength + " characters");
		}
		return trimmed;
	}
}
