package com.dandragu.saasetl.csv.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.dandragu.saasetl.csv.infrastructure.parser.CsvParseResult;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParser;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserError;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserException;

class CsvPreviewServiceTest {

	private CsvParser csvParser;
	private CsvPreviewService csvPreviewService;

	@BeforeEach
	void setUp() {
		csvParser = mock(CsvParser.class);
		csvPreviewService = new CsvPreviewService(csvParser, new CsvFileValidator());
	}

	@Test
	void shouldRejectMissingCommand() {
		assertThatThrownBy(() -> csvPreviewService.preview(null))
				.isInstanceOfSatisfying(CsvPreviewException.class,
						exception -> assertThat(exception.getError()).isEqualTo(CsvPreviewError.FILE_REQUIRED));

		verifyNoInteractions(csvParser);
	}

	@Test
	void shouldRejectMissingContent() {
		assertPreviewError(
				new CsvPreviewCommand("data.csv", 10, null),
				CsvPreviewError.FILE_REQUIRED);

		verifyNoInteractions(csvParser);
	}

	@Test
	void shouldRejectZeroSizeAndCloseStream() {
		assertValidationErrorAndClosed("data.csv", 0, CsvPreviewError.FILE_EMPTY);
	}

	@Test
	void shouldRejectNegativeSizeAndCloseStream() {
		assertValidationErrorAndClosed("data.csv", -1, CsvPreviewError.FILE_EMPTY);
	}

	@Test
	void shouldRejectMissingFileNameAndCloseStream() {
		assertValidationErrorAndClosed(null, 10, CsvPreviewError.INVALID_EXTENSION);
	}

	@Test
	void shouldRejectEmptyFileNameAndCloseStream() {
		assertValidationErrorAndClosed("", 10, CsvPreviewError.INVALID_EXTENSION);
	}

	@Test
	void shouldRejectBlankFileNameAndCloseStream() {
		assertValidationErrorAndClosed("   ", 10, CsvPreviewError.INVALID_EXTENSION);
	}

	@Test
	void shouldRejectFileNameWithoutExtensionAndCloseStream() {
		assertValidationErrorAndClosed("data", 10, CsvPreviewError.INVALID_EXTENSION);
	}

	@Test
	void shouldRejectNonCsvExtensionAndCloseStream() {
		assertValidationErrorAndClosed("data.txt", 10, CsvPreviewError.INVALID_EXTENSION);
	}

	@Test
	void shouldRejectDisguisedCsvExtensionAndCloseStream() {
		assertValidationErrorAndClosed("data.csv.exe", 10, CsvPreviewError.INVALID_EXTENSION);
	}

	@Test
	void shouldRejectFileLargerThanLimitAndCloseStream() {
		assertValidationErrorAndClosed(
				"data.csv",
				CsvFileValidator.MAX_FILE_SIZE + 1,
				CsvPreviewError.FILE_TOO_LARGE);
	}

	@Test
	void shouldAcceptUppercaseCsvExtension() {
		TrackingInputStream inputStream = inputStream("id\n1");
		CsvParseResult parseResult = new CsvParseResult(List.of("id"), List.of(List.of("1")), false);
		when(csvParser.parse(same(inputStream))).thenReturn(parseResult);

		CsvPreviewResult result = csvPreviewService.preview(
				new CsvPreviewCommand("DATA.CSV", 4, inputStream));

		assertThat(result.fileName()).isEqualTo("DATA.CSV");
	}

	@Test
	void shouldAcceptFileAtExactSizeLimit() {
		TrackingInputStream inputStream = inputStream("id\n1");
		CsvParseResult parseResult = new CsvParseResult(List.of("id"), List.of(List.of("1")), false);
		when(csvParser.parse(same(inputStream))).thenReturn(parseResult);

		CsvPreviewResult result = csvPreviewService.preview(
				new CsvPreviewCommand("data.csv", CsvFileValidator.MAX_FILE_SIZE, inputStream));

		assertThat(result.previewRowCount()).isEqualTo(1);
	}

	@Test
	void shouldDelegateSameInputStreamToParserAndCloseItAfterSuccess() {
		TrackingInputStream inputStream = inputStream("id,name\n1,Ana");
		CsvParseResult parseResult = new CsvParseResult(
				List.of("id", "name"),
				List.of(List.of("1", "Ana")),
				false);
		when(csvParser.parse(same(inputStream))).thenReturn(parseResult);

		CsvPreviewResult result = csvPreviewService.preview(
				new CsvPreviewCommand("data.csv", 13, inputStream));

		verify(csvParser).parse(same(inputStream));
		assertThat(inputStream.isClosed()).isTrue();
		assertThat(result.columns()).containsExactly("id", "name");
		assertThat(result.rows()).containsExactly(List.of("1", "Ana"));
		assertThat(result.previewRowCount()).isEqualTo(1);
		assertThat(result.truncated()).isFalse();
	}

	@Test
	void shouldReturnZeroPreviewRowCountForHeaderOnlyCsv() {
		TrackingInputStream inputStream = inputStream("id,name");
		when(csvParser.parse(same(inputStream)))
				.thenReturn(new CsvParseResult(List.of("id", "name"), List.of(), false));

		CsvPreviewResult result = csvPreviewService.preview(
				new CsvPreviewCommand("data.csv", 7, inputStream));

		assertThat(result.rows()).isEmpty();
		assertThat(result.previewRowCount()).isZero();
		assertThat(result.truncated()).isFalse();
	}

	@Test
	void shouldPreserveTruncatedFlag() {
		TrackingInputStream inputStream = inputStream("id\n1");
		when(csvParser.parse(same(inputStream)))
				.thenReturn(new CsvParseResult(List.of("id"), List.of(List.of("1")), true));

		CsvPreviewResult result = csvPreviewService.preview(
				new CsvPreviewCommand("data.csv", 4, inputStream));

		assertThat(result.truncated()).isTrue();
	}

	@Test
	void shouldReturnSafeBaseNameForPathLikeFileName() {
		TrackingInputStream inputStream = inputStream("id\n1");
		when(csvParser.parse(same(inputStream)))
				.thenReturn(new CsvParseResult(List.of("id"), List.of(List.of("1")), false));

		CsvPreviewResult result = csvPreviewService.preview(
				new CsvPreviewCommand("../uploads\\data.csv", 4, inputStream));

		assertThat(result.fileName()).isEqualTo("data.csv");
	}

	@ParameterizedTest
	@EnumSource(CsvParserError.class)
	void shouldTranslateParserErrorsAndCloseStream(CsvParserError parserError) {
		TrackingInputStream inputStream = inputStream("invalid");
		CsvParserException parserException = new CsvParserException(parserError, "Parser failure");
		when(csvParser.parse(same(inputStream))).thenThrow(parserException);

		assertThatThrownBy(() -> csvPreviewService.preview(
				new CsvPreviewCommand("data.csv", 7, inputStream)))
				.isInstanceOfSatisfying(CsvPreviewException.class, exception -> {
					assertThat(exception.getError())
							.isEqualTo(CsvPreviewError.valueOf(parserError.name()));
					assertThat(exception.getCause()).isSameAs(parserException);
				});

		assertThat(inputStream.isClosed()).isTrue();
	}

	@Test
	void shouldReturnDefensiveCopies() {
		TrackingInputStream inputStream = inputStream("id,name\n1,Ana");
		List<String> columns = new ArrayList<>(List.of("id", "name"));
		List<String> row = new ArrayList<>(List.of("1", "Ana"));
		List<List<String>> rows = new ArrayList<>();
		rows.add(row);
		when(csvParser.parse(same(inputStream)))
				.thenReturn(new CsvParseResult(columns, rows, false));

		CsvPreviewResult result = csvPreviewService.preview(
				new CsvPreviewCommand("data.csv", 13, inputStream));

		assertThatThrownBy(() -> result.columns().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> result.rows().add(List.of("2", "Luis")))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> result.rows().getFirst().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private void assertValidationErrorAndClosed(
			String fileName,
			long size,
			CsvPreviewError expectedError) {
		TrackingInputStream inputStream = inputStream("content");

		assertPreviewError(
				new CsvPreviewCommand(fileName, size, inputStream),
				expectedError);

		assertThat(inputStream.isClosed()).isTrue();
		verifyNoInteractions(csvParser);
	}

	private void assertPreviewError(CsvPreviewCommand command, CsvPreviewError expectedError) {
		assertThatThrownBy(() -> csvPreviewService.preview(command))
				.isInstanceOfSatisfying(CsvPreviewException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}

	private TrackingInputStream inputStream(String content) {
		return new TrackingInputStream(content.getBytes(StandardCharsets.UTF_8));
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
