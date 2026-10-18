package com.dandragu.saasetl.product.application;

public class ProductVersionConflictException extends RuntimeException {

	public ProductVersionConflictException() {
		super("The product was modified by another operation.");
	}

	public ProductVersionConflictException(Throwable cause) {
		super("The product was modified by another operation.", cause);
	}
}
