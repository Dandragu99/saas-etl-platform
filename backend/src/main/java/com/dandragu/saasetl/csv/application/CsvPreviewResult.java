package com.dandragu.saasetl.csv.application;

import java.util.List;

public record CsvPreviewResult(
		String fileName,
		List<String> columns,
		List<List<String>> rows,
		int previewRowCount,
		boolean truncated) {

	public CsvPreviewResult {
		columns = List.copyOf(columns);
		rows = rows.stream()
				.map(List::copyOf)
				.toList();
	}
}
