package com.dandragu.saasetl.csv.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.csv.infrastructure.csv.CsvStreamReader;

class RemoveColumnDownloadServiceTest {

	private final RemoveColumnDownloadService service = new RemoveColumnDownloadService(
			new CsvFileValidator(),
			new CsvStreamReader());

	@Test
	void shouldDownloadEveryRowIncludingRowsAfterTwenty() {
		StringBuilder csv = new StringBuilder("id,nombre,email\n");
		for (int index = 1; index <= 25; index++) {
			csv.append(index)
					.append(",Nombre ")
					.append(index)
					.append(",email")
					.append(index)
					.append("@example.com\n");
		}

		RemoveColumnDownloadResult result = service.download(command(
				"clientes.csv",
				csv.toString(),
				"email"));
		String output = text(result);

		assertThat(output.lines()).hasSize(26);
		assertThat(output).startsWith("id,nombre\n1,Nombre 1\n");
		assertThat(output).endsWith("25,Nombre 25\n");
	}

	@Test
	void shouldPreserveEscapedValuesEmptyFieldsLineBreaksAndUtf8WithoutBom() {
		String input = "id,nota,email,vacío\n"
				+ "1,\"Ana, dijo \"\"hola\"\"\nsegunda línea\",ana@example.com,\n";

		RemoveColumnDownloadResult result = service.download(command(
				"clientes.csv",
				input,
				"email"));
		byte[] output = result.content();

		assertThat(startsWithUtf8Bom(output)).isFalse();
		assertThat(new String(output, StandardCharsets.UTF_8)).isEqualTo(
				"id,nota,vacío\n"
						+ "1,\"Ana, dijo \"\"hola\"\"\nsegunda línea\",\n");
		assertThat(new String(output, StandardCharsets.UTF_8)).doesNotContain("\r\n");
	}

	@Test
	void shouldRejectMalformedRecordAfterRowTwentyInsteadOfReturningPartialCsv() {
		assertFileError(
				command("clientes.csv", csvWithMalformedRowAfterTwenty(), "email"),
				CsvPreviewError.MALFORMED);
	}

	@Test
	void shouldGiveLateFileErrorPriorityOverInvalidColumn() {
		assertFileError(
				command("clientes.csv", csvWithMalformedRowAfterTwenty(), null),
				CsvPreviewError.MALFORMED);
		assertFileError(
				command("clientes.csv", csvWithMalformedRowAfterTwenty(), "missing"),
				CsvPreviewError.MALFORMED);
	}

	@Test
	void shouldNotAllocateOutputBufferWhenPlanCannotBePrepared() {
		RemoveColumnDownloadService.DownloadConsumer missingColumn =
				new RemoveColumnDownloadService.DownloadConsumer(null);
		missingColumn.acceptHeader(List.of("id", "email"));
		assertThat(missingColumn.hasOutputBuffer()).isFalse();

		RemoveColumnDownloadService.DownloadConsumer unknownColumn =
				new RemoveColumnDownloadService.DownloadConsumer("missing");
		unknownColumn.acceptHeader(List.of("id", "email"));
		assertThat(unknownColumn.hasOutputBuffer()).isFalse();

		RemoveColumnDownloadService.DownloadConsumer lastColumn =
				new RemoveColumnDownloadService.DownloadConsumer("id");
		lastColumn.acceptHeader(List.of("id"));
		assertThat(lastColumn.hasOutputBuffer()).isFalse();
	}

	@Test
	void shouldReturnExistingColumnErrorsAfterValidatingAValidFile() {
		assertColumnError(command("clientes.csv", validCsv(), null), RemoveColumnError.COLUMN_REQUIRED);
		assertColumnError(command("clientes.csv", validCsv(), "missing"), RemoveColumnError.COLUMN_NOT_FOUND);
		assertColumnError(
				command("single.csv", "id\n1", "id"),
				RemoveColumnError.CANNOT_REMOVE_LAST_COLUMN);
	}

	@Test
	void shouldCloseInputStreamAfterSuccessAndAfterFileError() {
		TrackingInputStream successfulStream = trackingStream("id,email\n1,a@example.com");
		service.download(command("clientes.csv", successfulStream, "email"));
		assertThat(successfulStream.isClosed()).isTrue();

		TrackingInputStream malformedStream = trackingStream("id,email\n1");
		assertFileError(
				command("clientes.csv", malformedStream, "email"),
				CsvPreviewError.MALFORMED);
		assertThat(malformedStream.isClosed()).isTrue();
	}

	@Test
	void shouldCloseInputStreamWhenMetadataValidationFails() {
		TrackingInputStream stream = trackingStream("content");

		assertFileError(
				new RemoveColumnDownloadCommand(
						new CsvPreviewCommand("wrong.txt", 7, stream),
						"email"),
				CsvPreviewError.INVALID_EXTENSION);

		assertThat(stream.isClosed()).isTrue();
	}

	@Test
	void shouldBuildSafeNamesForUppercaseAndMultipleExtensions() {
		assertThat(service.download(command("clientes.csv", validCsv(), "email")).downloadFileName())
				.isEqualTo("clientes-sin-email.csv");
		assertThat(service.download(command("clientes.CSV", validCsv(), "email")).downloadFileName())
				.isEqualTo("clientes-sin-email.csv");
		assertThat(service.download(command("clientes.backup.csv", validCsv(), "email")).downloadFileName())
				.isEqualTo("clientes.backup-sin-email.csv");
	}

	@Test
	void shouldSanitizeOnlyDownloadNameAndLimitItByCodePoints() {
		String longBase = "a".repeat(220);
		String longColumn = "columna con / barras y " + "b".repeat(80);
		String csv = "id," + longColumn + "\n1,value";

		RemoveColumnDownloadResult result = service.download(command(
				longBase + ".csv",
				csv,
				longColumn));

		assertThat(result.downloadFileName().codePointCount(0, result.downloadFileName().length()))
				.isLessThanOrEqualTo(RemoveColumnDownloadService.MAX_DOWNLOAD_FILE_NAME_LENGTH);
		String columnFragment = result.downloadFileName()
				.substring(result.downloadFileName().lastIndexOf("-sin-") + 5, result.downloadFileName().length() - 4);
		assertThat(columnFragment.codePointCount(0, columnFragment.length()))
				.isLessThanOrEqualTo(RemoveColumnDownloadService.MAX_COLUMN_FILE_NAME_FRAGMENT_LENGTH);
		assertThat(text(result)).isEqualTo("id\n1\n");
	}

	@Test
	void shouldProtectContentWithDefensiveCopies() {
		byte[] original = "id\n1\n".getBytes(StandardCharsets.UTF_8);
		RemoveColumnDownloadResult result = new RemoveColumnDownloadResult("result.csv", original);
		original[0] = 'X';
		byte[] firstRead = result.content();
		firstRead[0] = 'Y';

		assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("id\n1\n");
	}

	private RemoveColumnDownloadCommand command(String fileName, String content, String column) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		return new RemoveColumnDownloadCommand(
				new CsvPreviewCommand(fileName, bytes.length, new ByteArrayInputStream(bytes)),
				column);
	}

	private RemoveColumnDownloadCommand command(
			String fileName,
			TrackingInputStream content,
			String column) {
		return new RemoveColumnDownloadCommand(
				new CsvPreviewCommand(fileName, content.available(), content),
				column);
	}

	private String validCsv() {
		return "id,email\n1,ana@example.com";
	}

	private String csvWithMalformedRowAfterTwenty() {
		StringBuilder csv = new StringBuilder("id,email\n");
		IntStream.rangeClosed(1, 20)
				.forEach(index -> csv.append(index).append(",email@example.com\n"));
		return csv.append("21").toString();
	}

	private String text(RemoveColumnDownloadResult result) {
		return new String(result.content(), StandardCharsets.UTF_8);
	}

	private void assertFileError(
			RemoveColumnDownloadCommand command,
			CsvPreviewError expectedError) {
		assertThatThrownBy(() -> service.download(command))
				.isInstanceOfSatisfying(
						CsvPreviewException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}

	private void assertColumnError(
			RemoveColumnDownloadCommand command,
			RemoveColumnError expectedError) {
		assertThatThrownBy(() -> service.download(command))
				.isInstanceOfSatisfying(
						RemoveColumnException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}

	private TrackingInputStream trackingStream(String content) {
		return new TrackingInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	private boolean startsWithUtf8Bom(byte[] content) {
		return content.length >= 3
				&& content[0] == (byte) 0xEF
				&& content[1] == (byte) 0xBB
				&& content[2] == (byte) 0xBF;
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
