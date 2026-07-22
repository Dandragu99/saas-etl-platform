package com.dandragu.saasetl.csv.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.dandragu.saasetl.csv.infrastructure.parser.CsvParseResult;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParser;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserException;

@Service
public class CsvPreviewService {

	static final long MAX_FILE_SIZE = 5_242_880L;

	private final CsvParser csvParser;

	public CsvPreviewService(CsvParser csvParser) {
		this.csvParser = csvParser;
	}

	public CsvPreviewResult preview(CsvPreviewCommand command) {
		if (command == null || command.content() == null) {
			throw new CsvPreviewException(
					CsvPreviewError.FILE_REQUIRED,
					"A CSV file is required.");
		}

		try (InputStream content = command.content()) {
			validateSize(command.size());
			String safeFileName = validateAndGetSafeFileName(command.originalFileName());
			CsvParseResult parseResult = parse(content);

			return toPreviewResult(safeFileName, parseResult);
		}
		catch (CsvPreviewException exception) {
			throw exception;
		}
		catch (IOException exception) {
			throw new CsvPreviewException(
					CsvPreviewError.READ_ERROR,
					"The CSV file could not be closed.",
					exception);
		}
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

	private CsvParseResult parse(InputStream content) {
		try {
			return csvParser.parse(content);
		}
		catch (CsvParserException exception) {
			throw new CsvPreviewException(
					CsvPreviewError.valueOf(exception.getError().name()),
					"The CSV file could not be processed.",
					exception);
		}
	}

	private CsvPreviewResult toPreviewResult(String fileName, CsvParseResult parseResult) {
		List<List<String>> rows = parseResult.rows();
		return new CsvPreviewResult(
				fileName,
				parseResult.columns(),
				rows,
				rows.size(),
				parseResult.truncated());
	}
}
