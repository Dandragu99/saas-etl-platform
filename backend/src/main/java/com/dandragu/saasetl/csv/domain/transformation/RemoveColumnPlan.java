package com.dandragu.saasetl.csv.domain.transformation;

import java.util.ArrayList;
import java.util.List;

public final class RemoveColumnPlan {

	private final int removedIndex;
	private final List<String> columns;

	private RemoveColumnPlan(int removedIndex, List<String> columns) {
		this.removedIndex = removedIndex;
		this.columns = List.copyOf(columns);
	}

	public static RemoveColumnPlan prepare(List<String> columns, String column) {
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

		return new RemoveColumnPlan(columnIndex, withoutIndex(columns, columnIndex));
	}

	public List<String> columns() {
		return columns;
	}

	public List<String> applyToRow(List<String> row) {
		return List.copyOf(withoutIndex(row, removedIndex));
	}

	private static List<String> withoutIndex(List<String> values, int removedIndex) {
		List<String> result = new ArrayList<>(values.size() - 1);
		for (int index = 0; index < values.size(); index++) {
			if (index != removedIndex) {
				result.add(values.get(index));
			}
		}
		return result;
	}
}
