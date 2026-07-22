package com.dandragu.saasetl.csv.domain.transformation;

import java.util.List;

public class RemoveColumnTransformation {

	public RemoveColumnTransformationResult apply(
			List<String> columns,
			List<List<String>> rows,
			String column) {
		RemoveColumnPlan plan = RemoveColumnPlan.prepare(columns, column);
		List<List<String>> transformedRows = rows.stream()
				.map(plan::applyToRow)
				.toList();

		return new RemoveColumnTransformationResult(plan.columns(), transformedRows);
	}
}
