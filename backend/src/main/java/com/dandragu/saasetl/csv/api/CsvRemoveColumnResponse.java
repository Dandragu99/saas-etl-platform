package com.dandragu.saasetl.csv.api;

import java.util.List;
import java.util.Map;

public record CsvRemoveColumnResponse(
		String fileName,
		List<String> columns,
		List<Map<String, String>> rows,
		int previewRowCount,
		boolean truncated,
		RemoveColumnTransformationResponse transformation) {

	public CsvRemoveColumnResponse {
		columns = List.copyOf(columns);
		rows = List.copyOf(rows);
	}
}
