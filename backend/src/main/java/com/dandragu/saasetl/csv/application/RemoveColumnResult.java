package com.dandragu.saasetl.csv.application;

import java.util.List;

public record RemoveColumnResult(
		String fileName,
		List<String> columns,
		List<List<String>> rows,
		int previewRowCount,
		boolean truncated,
		String removedColumn) {

	public RemoveColumnResult {
		columns = List.copyOf(columns);
		rows = rows.stream()
				.map(List::copyOf)
				.toList();
	}
}
