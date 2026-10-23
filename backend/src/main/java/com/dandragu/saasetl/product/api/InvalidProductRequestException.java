package com.dandragu.saasetl.product.api;

public class InvalidProductRequestException extends RuntimeException {

	public InvalidProductRequestException(String message, Throwable cause) {
		super(message, cause);
	}
}
