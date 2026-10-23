package com.dandragu.saasetl.product.api;

public class InvalidProductQueryException extends RuntimeException {

	public InvalidProductQueryException(String message) {
		super(message);
	}
}
