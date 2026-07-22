package com.dandragu.saasetl.csv.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dandragu.saasetl.csv.infrastructure.parser.CsvParseResult;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParser;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserException;

@Service
public class CsvPreviewService {

	private final CsvParser csvParser;
	private final CsvFileValidator csvFileValidator;

	public CsvPreviewService(CsvParser csvParser, CsvFileValidator csvFileValidator) {
		this.csvParser = csvParser;
		this.csvFileValidator = csvFileValidator;
	}

	public CsvPreviewResult preview(CsvPreviewCommand command) {
		InputStream inputStream = command == null ? null : command.content();
		if (inputStream == null) {
			csvFileValidator.validateAndGetSafeFileName(command);
		}

		try (InputStream content = inputStream) {
			String safeFileName = csvFileValidator.validateAndGetSafeFileName(command);
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
