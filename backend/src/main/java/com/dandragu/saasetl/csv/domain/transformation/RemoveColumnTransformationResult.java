package com.dandragu.saasetl.csv.domain.transformation;

import java.util.List;

public record RemoveColumnTransformationResult(
		List<String> columns,
		List<List<String>> rows) {

	public RemoveColumnTransformationResult {
		columns = List.copyOf(columns);
		rows = rows.stream()
				.map(List::copyOf)
				.toList();
	}
}
