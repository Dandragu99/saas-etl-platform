package com.dandragu.saasetl.product.application;

public class DuplicateProductSkuException extends RuntimeException {

	public DuplicateProductSkuException() {
		super("A product with the same normalized SKU already exists in the current company.");
	}
}
