package com.dandragu.saasetl.csv.api;

import java.util.List;
import java.util.Map;

public record CsvPreviewResponse(
		String fileName,
		List<String> columns,
		List<Map<String, String>> rows,
		int previewRowCount,
		boolean truncated) {

	public CsvPreviewResponse {
		columns = List.copyOf(columns);
		rows = List.copyOf(rows);
	}
}
