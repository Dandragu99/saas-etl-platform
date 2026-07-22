package com.dandragu.saasetl.csv.infrastructure.parser;

import java.util.List;

public record CsvParseResult(List<String> columns, List<List<String>> rows, boolean truncated) {

	public CsvParseResult {
		columns = List.copyOf(columns);
		rows = rows.stream()
				.map(List::copyOf)
				.toList();
	}
}
