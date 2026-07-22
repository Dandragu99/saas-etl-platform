package com.dandragu.saasetl.csv.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

class CsvFileValidatorTest {

	private final CsvFileValidator validator = new CsvFileValidator();

	@Test
	void shouldReturnSafeBaseNameAndAcceptUppercaseExtension() {
		assertThat(validator.validateAndGetSafeFileName(command("C:\\fakepath\\clientes.CSV", 1)))
				.isEqualTo("clientes.CSV");
	}

	@Test
	void shouldAcceptExactMaximumSize() {
		assertThat(validator.validateAndGetSafeFileName(
				command("clientes.csv", CsvFileValidator.MAX_FILE_SIZE)))
				.isEqualTo("clientes.csv");
	}

	@Test
	void shouldPreserveExistingFileValidationErrors() {
		assertError(null, CsvPreviewError.FILE_REQUIRED);
		assertError(new CsvPreviewCommand("clientes.csv", 1, null), CsvPreviewError.FILE_REQUIRED);
		assertError(command("clientes.csv", 0), CsvPreviewError.FILE_EMPTY);
		assertError(
				command("clientes.csv", CsvFileValidator.MAX_FILE_SIZE + 1),
				CsvPreviewError.FILE_TOO_LARGE);
		assertError(command("clientes.txt", 1), CsvPreviewError.INVALID_EXTENSION);
		assertError(command(" ", 1), CsvPreviewError.INVALID_EXTENSION);
	}

	private CsvPreviewCommand command(String name, long size) {
		return new CsvPreviewCommand(name, size, InputStream.nullInputStream());
	}

	private void assertError(CsvPreviewCommand command, CsvPreviewError expectedError) {
		assertThatThrownBy(() -> validator.validateAndGetSafeFileName(command))
				.isInstanceOfSatisfying(
						CsvPreviewException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}
}
