package com.dandragu.saasetl.csv.domain.transformation;

import java.util.ArrayList;
import java.util.List;

public class RemoveColumnTransformation {

	public RemoveColumnTransformationResult apply(
			List<String> columns,
			List<List<String>> rows,
			String column) {
		int columnIndex = columns.indexOf(column);
		if (columnIndex < 0) {
			throw new RemoveColumnTransformationException(
					RemoveColumnTransformationError.COLUMN_NOT_FOUND,
					"The requested column does not exist.");
		}

		if (columns.size() == 1) {
			throw new RemoveColumnTransformationException(
					RemoveColumnTransformationError.CANNOT_REMOVE_LAST_COLUMN,
					"The only CSV column cannot be removed.");
		}

		List<String> transformedColumns = withoutIndex(columns, columnIndex);
		List<List<String>> transformedRows = rows.stream()
				.map(row -> withoutIndex(row, columnIndex))
				.toList();

		return new RemoveColumnTransformationResult(transformedColumns, transformedRows);
	}

	private List<String> withoutIndex(List<String> values, int removedIndex) {
		List<String> result = new ArrayList<>(values.size() - 1);
		for (int index = 0; index < values.size(); index++) {
			if (index != removedIndex) {
				result.add(values.get(index));
			}
		}
		return result;
	}
}
