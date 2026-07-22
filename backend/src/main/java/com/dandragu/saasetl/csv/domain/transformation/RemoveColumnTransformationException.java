package com.dandragu.saasetl.csv.domain.transformation;

public class RemoveColumnTransformationException extends RuntimeException {

	private final RemoveColumnTransformationError error;

	public RemoveColumnTransformationException(
			RemoveColumnTransformationError error,
			String message) {
		super(message);
		this.error = error;
	}

	public RemoveColumnTransformationError getError() {
		return error;
	}
}
