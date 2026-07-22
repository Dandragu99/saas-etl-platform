package com.dandragu.saasetl.csv.infrastructure.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

public class CsvWriter implements AutoCloseable {

	private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
			.setDelimiter(',')
			.setQuote('"')
			.setQuoteMode(QuoteMode.MINIMAL)
			.setRecordSeparator("\n")
			.setIgnoreSurroundingSpaces(false)
			.setTrim(false)
			.get();

	private final CSVPrinter csvPrinter;

	public CsvWriter(OutputStream outputStream) {
		try {
			csvPrinter = new CSVPrinter(
					new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
					CSV_FORMAT);
		}
		catch (IOException exception) {
			throw new CsvWriterException("The CSV writer could not be created.", exception);
		}
	}

	public void writeRecord(List<String> values) {
		try {
			csvPrinter.printRecord(values);
		}
		catch (IOException exception) {
			throw new CsvWriterException("The CSV record could not be written.", exception);
		}
	}

	@Override
	public void close() {
		try {
			csvPrinter.close();
		}
		catch (IOException exception) {
			throw new CsvWriterException("The CSV writer could not be closed.", exception);
		}
	}
}
