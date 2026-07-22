package com.dandragu.saasetl.csv.domain.transformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RemoveColumnTransformationTest {

	private final RemoveColumnTransformation transformation = new RemoveColumnTransformation();

	@ParameterizedTest
	@MethodSource("columnsToRemove")
	void shouldRemoveColumnAndPreserveRemainingOrder(
			String removedColumn,
			List<String> expectedColumns,
			List<String> expectedRow) {
		RemoveColumnTransformationResult result = transformation.apply(
				List.of("id", "nombre", "email"),
				List.of(List.of("1", "Ana", "ana@example.com")),
				removedColumn);

		assertThat(result.columns()).isEqualTo(expectedColumns);
		assertThat(result.rows()).containsExactly(expectedRow);
	}

	@Test
	void shouldPreserveLiteralValuesInEveryRow() {
		RemoveColumnTransformationResult result = transformation.apply(
				List.of("id", "nota", "nombre"),
				List.of(
						List.of("1", "", " Ana "),
						List.of("2", "línea\nnueva", "Luis")),
				"nota");

		assertThat(result.rows()).containsExactly(
				List.of("1", " Ana "),
				List.of("2", "Luis"));
	}

	@Test
	void shouldSupportHeaderWithoutRows() {
		RemoveColumnTransformationResult result = transformation.apply(
				List.of("id", "email"),
				List.of(),
				"email");

		assertThat(result.columns()).containsExactly("id");
		assertThat(result.rows()).isEmpty();
	}

	@Test
	void shouldMatchColumnExactlyAndCaseSensitively() {
		assertTransformationError(
				List.of("id", "Email"),
				"email",
				RemoveColumnTransformationError.COLUMN_NOT_FOUND);
		assertTransformationError(
				List.of("id", "email"),
				"mail",
				RemoveColumnTransformationError.COLUMN_NOT_FOUND);
	}

	@Test
	void shouldRejectRemovingTheOnlyColumn() {
		assertTransformationError(
				List.of("id"),
				"id",
				RemoveColumnTransformationError.CANNOT_REMOVE_LAST_COLUMN);
	}

	@Test
	void shouldNotModifyOriginalCollections() {
		List<String> columns = new ArrayList<>(List.of("id", "email"));
		List<String> row = new ArrayList<>(List.of("1", "ana@example.com"));
		List<List<String>> rows = new ArrayList<>();
		rows.add(row);

		transformation.apply(columns, rows, "email");

		assertThat(columns).containsExactly("id", "email");
		assertThat(rows).containsExactly(List.of("1", "ana@example.com"));
		assertThat(row).containsExactly("1", "ana@example.com");
	}

	@Test
	void shouldReturnImmutableDefensiveCopies() {
		List<String> columns = new ArrayList<>(List.of("id", "email"));
		List<String> row = new ArrayList<>(List.of("1", "ana@example.com"));
		List<List<String>> rows = new ArrayList<>();
		rows.add(row);

		RemoveColumnTransformationResult result = transformation.apply(columns, rows, "email");
		columns.set(0, "changed");
		row.set(0, "changed");

		assertThat(result.columns()).containsExactly("id");
		assertThat(result.rows()).containsExactly(List.of("1"));
		assertThatThrownBy(() -> result.columns().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> result.rows().add(List.of("2")))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> result.rows().getFirst().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private void assertTransformationError(
			List<String> columns,
			String column,
			RemoveColumnTransformationError expectedError) {
		assertThatThrownBy(() -> transformation.apply(columns, List.of(), column))
				.isInstanceOfSatisfying(
						RemoveColumnTransformationException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}

	private static List<Arguments> columnsToRemove() {
		return List.of(
				Arguments.of("id", List.of("nombre", "email"), List.of("Ana", "ana@example.com")),
				Arguments.of("nombre", List.of("id", "email"), List.of("1", "ana@example.com")),
				Arguments.of("email", List.of("id", "nombre"), List.of("1", "Ana")));
	}
}
