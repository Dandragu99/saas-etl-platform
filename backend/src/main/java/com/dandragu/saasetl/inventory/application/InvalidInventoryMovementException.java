package com.dandragu.saasetl.inventory.application;

public class InvalidInventoryMovementException extends RuntimeException {
	public InvalidInventoryMovementException(String message) {
		super(message);
	}
}
