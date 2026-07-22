package com.dandragu.saasetl.csv.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class RemoveColumnServiceTest {

	private CsvPreviewService csvPreviewService;
	private RemoveColumnService removeColumnService;

	@BeforeEach
	void setUp() {
		csvPreviewService = mock(CsvPreviewService.class);
		removeColumnService = new RemoveColumnService(csvPreviewService);
	}

	@Test
	void shouldReusePreviewAndReturnTransformedResult() {
		InputStream content = new ByteArrayInputStream(new byte[] { 1 });
		CsvPreviewCommand file = new CsvPreviewCommand("clientes.csv", 1, content);
		when(csvPreviewService.preview(any())).thenReturn(previewResult(false));

		RemoveColumnResult result = removeColumnService.remove(
				new RemoveColumnCommand(file, "email"));

		ArgumentCaptor<CsvPreviewCommand> captor = ArgumentCaptor.forClass(CsvPreviewCommand.class);
		verify(csvPreviewService).preview(captor.capture());
		assertThat(captor.getValue()).isSameAs(file);
		assertThat(result.fileName()).isEqualTo("clientes.csv");
		assertThat(result.columns()).containsExactly("id", "nombre");
		assertThat(result.rows()).containsExactly(List.of("1", "Ana"));
		assertThat(result.previewRowCount()).isEqualTo(1);
		assertThat(result.truncated()).isFalse();
		assertThat(result.removedColumn()).isEqualTo("email");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t", " \r\n " })
	void shouldValidateColumnAfterProcessingFile(String column) {
		CsvPreviewCommand file = new CsvPreviewCommand(
				"clientes.csv",
				1,
				new ByteArrayInputStream(new byte[] { 1 }));
		when(csvPreviewService.preview(file)).thenReturn(previewResult(false));

		assertApplicationError(
				new RemoveColumnCommand(file, column),
				RemoveColumnError.COLUMN_REQUIRED);
		verify(csvPreviewService).preview(file);
	}

	@Test
	void shouldGiveFileErrorPriorityOverInvalidColumn() {
		CsvPreviewCommand file = new CsvPreviewCommand("wrong.txt", 1, InputStream.nullInputStream());
		CsvPreviewException fileError = new CsvPreviewException(
				CsvPreviewError.INVALID_EXTENSION,
				"Technical detail");
		when(csvPreviewService.preview(file)).thenThrow(fileError);

		assertThatThrownBy(() -> removeColumnService.remove(new RemoveColumnCommand(file, null)))
				.isSameAs(fileError);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Email", "mail", " email" })
	void shouldTranslateColumnNotFound(String column) {
		when(csvPreviewService.preview(any())).thenReturn(previewResult(false));

		assertApplicationError(command(column), RemoveColumnError.COLUMN_NOT_FOUND);
	}

	@Test
	void shouldTranslateCannotRemoveLastColumn() {
		when(csvPreviewService.preview(any())).thenReturn(new CsvPreviewResult(
				"single.csv", List.of("id"), List.of(List.of("1")), 1, false));

		assertApplicationError(command("id"), RemoveColumnError.CANNOT_REMOVE_LAST_COLUMN);
	}

	@Test
	void shouldPreservePreviewMetadata() {
		when(csvPreviewService.preview(any())).thenReturn(previewResult(true));

		RemoveColumnResult result = removeColumnService.remove(command("email"));

		assertThat(result.previewRowCount()).isEqualTo(1);
		assertThat(result.truncated()).isTrue();
	}

	@Test
	void shouldLeaveStreamLifecycleToCsvPreviewService() {
		TrackingInputStream content = new TrackingInputStream(new byte[] { 1 });
		CsvPreviewCommand file = new CsvPreviewCommand("clientes.csv", 1, content);
		when(csvPreviewService.preview(file)).thenReturn(previewResult(false));

		removeColumnService.remove(new RemoveColumnCommand(file, "email"));

		assertThat(content.isClosed()).isFalse();
	}

	@Test
	void shouldReturnImmutableDefensiveResult() {
		when(csvPreviewService.preview(any())).thenReturn(previewResult(false));
		RemoveColumnResult result = removeColumnService.remove(command("email"));

		assertThatThrownBy(() -> result.columns().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> result.rows().add(List.of("2", "Luis")))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> result.rows().getFirst().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private CsvPreviewResult previewResult(boolean truncated) {
		return new CsvPreviewResult(
				"clientes.csv",
				List.of("id", "nombre", "email"),
				List.of(List.of("1", "Ana", "ana@example.com")),
				1,
				truncated);
	}

	private RemoveColumnCommand command(String column) {
		return new RemoveColumnCommand(
				new CsvPreviewCommand("clientes.csv", 1, InputStream.nullInputStream()),
				column);
	}

	private void assertApplicationError(
			RemoveColumnCommand command,
			RemoveColumnError expectedError) {
		assertThatThrownBy(() -> removeColumnService.remove(command))
				.isInstanceOfSatisfying(
						RemoveColumnException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}

	private static final class TrackingInputStream extends ByteArrayInputStream {

		private boolean closed;

		private TrackingInputStream(byte[] content) {
			super(content);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}

		private boolean isClosed() {
			return closed;
		}
	}
}
