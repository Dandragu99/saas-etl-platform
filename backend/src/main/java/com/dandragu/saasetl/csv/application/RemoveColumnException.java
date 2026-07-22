package com.dandragu.saasetl.csv.application;

public class RemoveColumnException extends RuntimeException {

	private final RemoveColumnError error;

	public RemoveColumnException(RemoveColumnError error, String message) {
		super(message);
		this.error = error;
	}

	public RemoveColumnException(RemoveColumnError error, String message, Throwable cause) {
		super(message, cause);
		this.error = error;
	}

	public RemoveColumnError getError() {
		return error;
	}
}
