package com.dandragu.saasetl.csv.infrastructure.csv;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Component;

import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserError;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserException;

@Component
public class CsvStreamReader {

	private static final byte[] UTF_8_BOM = {
			(byte) 0xEF,
			(byte) 0xBB,
			(byte) 0xBF
	};

	private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
			.setDelimiter(',')
			.setQuote('"')
			.setIgnoreEmptyLines(true)
			.setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
			.setAllowMissingColumnNames(false)
			.setIgnoreSurroundingSpaces(false)
			.setTrim(false)
			.setHeader()
			.setSkipHeaderRecord(true)
			.get();

	public void read(InputStream inputStream, CsvRecordConsumer consumer) {
		try (Reader reader = createStrictUtf8Reader(inputStream);
				CSVParser csvParser = CSV_FORMAT.parse(reader)) {
			List<String> columns = List.copyOf(csvParser.getHeaderNames());
			validateHeader(columns);
			consumer.acceptHeader(columns);

			for (CSVRecord record : csvParser) {
				validateRecordSize(record, columns.size());
				consumer.acceptRow(List.of(record.values()));
			}
		}
		catch (IllegalArgumentException exception) {
			throw new CsvParserException(
					CsvParserError.INVALID_HEADER,
					"The CSV header is invalid.",
					exception);
		}
		catch (UncheckedIOException exception) {
			throw mapReadException(exception.getCause());
		}
		catch (IOException exception) {
			throw mapReadException(exception);
		}
	}

	private Reader createStrictUtf8Reader(InputStream inputStream) throws IOException {
		PushbackInputStream streamWithoutBom = removeUtf8Bom(withoutClosePropagation(inputStream));
		return new InputStreamReader(
				streamWithoutBom,
				StandardCharsets.UTF_8.newDecoder()
						.onMalformedInput(CodingErrorAction.REPORT)
						.onUnmappableCharacter(CodingErrorAction.REPORT));
	}

	private InputStream withoutClosePropagation(InputStream inputStream) {
		return new FilterInputStream(inputStream) {
			@Override
			public void close() {
				// The caller owns the original stream and is responsible for closing it.
			}
		};
	}

	private PushbackInputStream removeUtf8Bom(InputStream inputStream) throws IOException {
		PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, UTF_8_BOM.length);
		byte[] firstBytes = pushbackInputStream.readNBytes(UTF_8_BOM.length);

		if (!isUtf8Bom(firstBytes)) {
			pushbackInputStream.unread(firstBytes);
		}

		return pushbackInputStream;
	}

	private boolean isUtf8Bom(byte[] bytes) {
		if (bytes.length != UTF_8_BOM.length) {
			return false;
		}

		for (int index = 0; index < UTF_8_BOM.length; index++) {
			if (bytes[index] != UTF_8_BOM[index]) {
				return false;
			}
		}

		return true;
	}

	private void validateHeader(List<String> columns) {
		if (columns.isEmpty()) {
			throw new CsvParserException(
					CsvParserError.HEADER_MISSING,
					"The CSV header is missing.");
		}

		Set<String> uniqueColumns = new HashSet<>();
		for (String column : columns) {
			if (column.isBlank() || !uniqueColumns.add(column)) {
				throw new CsvParserException(
						CsvParserError.INVALID_HEADER,
						"The CSV header contains a missing or duplicate column name.");
			}
		}
	}

	private void validateRecordSize(CSVRecord record, int expectedSize) {
		if (record.size() != expectedSize) {
			throw new CsvParserException(
					CsvParserError.MALFORMED,
					"CSV record %d contains %d fields; expected %d."
							.formatted(record.getRecordNumber(), record.size(), expectedSize));
		}
	}

	private CsvParserException mapReadException(IOException exception) {
		if (isMalformedContent(exception)) {
			return new CsvParserException(
					CsvParserError.MALFORMED,
					"The CSV content is malformed or is not valid UTF-8.",
					exception);
		}

		return new CsvParserException(
				CsvParserError.READ_ERROR,
				"The CSV content could not be read.",
				exception);
	}

	private boolean isMalformedContent(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof CSVException || current instanceof CharacterCodingException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
