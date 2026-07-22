package com.dandragu.saasetl.csv.infrastructure.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.dandragu.saasetl.csv.infrastructure.csv.CsvRecordConsumer;
import com.dandragu.saasetl.csv.infrastructure.csv.CsvStreamReader;

@Component
public class CsvParser {

	private static final int PREVIEW_ROW_LIMIT = 20;

	private final CsvStreamReader csvStreamReader;

	public CsvParser(CsvStreamReader csvStreamReader) {
		this.csvStreamReader = csvStreamReader;
	}

	public CsvParseResult parse(InputStream inputStream) {
		PreviewConsumer consumer = new PreviewConsumer();
		csvStreamReader.read(inputStream, consumer);
		return consumer.toResult();
	}

	private static final class PreviewConsumer implements CsvRecordConsumer {

		private List<String> columns = List.of();
		private final List<List<String>> previewRows = new ArrayList<>();
		private boolean truncated;

		@Override
		public void acceptHeader(List<String> columns) {
			this.columns = columns;
		}

		@Override
		public void acceptRow(List<String> row) {
			if (previewRows.size() < PREVIEW_ROW_LIMIT) {
				previewRows.add(row);
			}
			else {
				truncated = true;
			}
		}

		private CsvParseResult toResult() {
			return new CsvParseResult(columns, previewRows, truncated);
		}
	}
}
