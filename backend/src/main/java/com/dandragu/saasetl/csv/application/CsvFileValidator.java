package com.dandragu.saasetl.csv.application;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class CsvFileValidator {

	public static final long MAX_FILE_SIZE = 5_242_880L;

	public String validateAndGetSafeFileName(CsvPreviewCommand command) {
		if (command == null || command.content() == null) {
			throw new CsvPreviewException(
					CsvPreviewError.FILE_REQUIRED,
					"A CSV file is required.");
		}

		validateSize(command.size());
		return validateAndGetSafeFileName(command.originalFileName());
	}

	private void validateSize(long size) {
		if (size <= 0) {
			throw new CsvPreviewException(
					CsvPreviewError.FILE_EMPTY,
					"The CSV file is empty.");
		}

		if (size > MAX_FILE_SIZE) {
			throw new CsvPreviewException(
					CsvPreviewError.FILE_TOO_LARGE,
					"The CSV file exceeds the maximum allowed size.");
		}
	}

	private String validateAndGetSafeFileName(String originalFileName) {
		if (originalFileName == null || originalFileName.isBlank()) {
			throw invalidExtension();
		}

		String safeFileName = getBaseName(originalFileName);
		if (safeFileName.isBlank()
				|| !safeFileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
			throw invalidExtension();
		}

		return safeFileName;
	}

	private String getBaseName(String originalFileName) {
		int lastForwardSlash = originalFileName.lastIndexOf('/');
		int lastBackwardSlash = originalFileName.lastIndexOf('\\');
		int lastSeparator = Math.max(lastForwardSlash, lastBackwardSlash);
		return originalFileName.substring(lastSeparator + 1);
	}

	private CsvPreviewException invalidExtension() {
		return new CsvPreviewException(
				CsvPreviewError.INVALID_EXTENSION,
				"The file must have a .csv extension.");
	}
}
