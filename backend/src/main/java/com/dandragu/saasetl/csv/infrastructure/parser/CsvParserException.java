package com.dandragu.saasetl.csv.infrastructure.parser;

public class CsvParserException extends RuntimeException {

	private final CsvParserError error;

	public CsvParserException(CsvParserError error, String message) {
		super(message);
		this.error = error;
	}

	public CsvParserException(CsvParserError error, String message, Throwable cause) {
		super(message, cause);
		this.error = error;
	}

	public CsvParserError getError() {
		return error;
	}
}
