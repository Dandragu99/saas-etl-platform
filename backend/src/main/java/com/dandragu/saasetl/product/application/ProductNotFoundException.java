package com.dandragu.saasetl.product.application;

public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException() {
		super("The product does not exist in the current company.");
	}
}
