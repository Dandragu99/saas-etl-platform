package com.dandragu.saasetl.product.application;

public class ProductCompanyUnavailableException extends RuntimeException {

	public ProductCompanyUnavailableException() {
		super("The current company is not available for product operations.");
	}
}
