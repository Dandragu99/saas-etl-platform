package com.dandragu.saasetl.csv.application;

public class CsvPreviewException extends RuntimeException {

	private final CsvPreviewError error;

	public CsvPreviewException(CsvPreviewError error, String message) {
		super(message);
		this.error = error;
	}

	public CsvPreviewException(CsvPreviewError error, String message, Throwable cause) {
		super(message, cause);
		this.error = error;
	}

	public CsvPreviewError getError() {
		return error;
	}
}
