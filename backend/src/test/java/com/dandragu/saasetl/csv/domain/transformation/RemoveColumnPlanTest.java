package com.dandragu.saasetl.csv.domain.transformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RemoveColumnPlanTest {

	@Test
	void shouldPrepareOnceAndRemoveTheSameIndexFromRows() {
		RemoveColumnPlan plan = RemoveColumnPlan.prepare(
				List.of("id", "nombre", "email"),
				"email");

		assertThat(plan.columns()).containsExactly("id", "nombre");
		assertThat(plan.applyToRow(List.of("1", "Ana", "ana@example.com")))
				.containsExactly("1", "Ana");
		assertThat(plan.applyToRow(List.of("2", "Carlos", "")))
				.containsExactly("2", "Carlos");
	}

	@Test
	void shouldMatchExactlyAndCaseSensitively() {
		assertPlanError("email", List.of("id", "Email"), RemoveColumnTransformationError.COLUMN_NOT_FOUND);
		assertPlanError("mail", List.of("id", "email"), RemoveColumnTransformationError.COLUMN_NOT_FOUND);
		assertPlanError(" email", List.of("id", "email"), RemoveColumnTransformationError.COLUMN_NOT_FOUND);
	}

	@Test
	void shouldRejectRemovingOnlyColumn() {
		assertPlanError(
				"id",
				List.of("id"),
				RemoveColumnTransformationError.CANNOT_REMOVE_LAST_COLUMN);
	}

	@Test
	void shouldNotModifyInputsAndShouldReturnImmutableValues() {
		List<String> columns = new ArrayList<>(List.of("id", "email"));
		List<String> row = new ArrayList<>(List.of("1", "ana@example.com"));
		RemoveColumnPlan plan = RemoveColumnPlan.prepare(columns, "email");
		List<String> transformedRow = plan.applyToRow(row);

		columns.set(0, "changed");
		row.set(0, "changed");

		assertThat(plan.columns()).containsExactly("id");
		assertThat(transformedRow).containsExactly("1");
		assertThatThrownBy(() -> plan.columns().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> transformedRow.add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private void assertPlanError(
			String column,
			List<String> columns,
			RemoveColumnTransformationError expectedError) {
		assertThatThrownBy(() -> RemoveColumnPlan.prepare(columns, column))
				.isInstanceOfSatisfying(
						RemoveColumnTransformationException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}
}
