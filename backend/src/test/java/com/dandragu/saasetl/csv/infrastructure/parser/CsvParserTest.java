package com.dandragu.saasetl.csv.infrastructure.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.csv.infrastructure.csv.CsvStreamReader;

class CsvParserTest {

	private static final byte[] UTF_8_BOM = {
			(byte) 0xEF,
			(byte) 0xBB,
			(byte) 0xBF
	};

	private final CsvParser csvParser = new CsvParser(new CsvStreamReader());

	@Test
	void shouldParseSimpleCsv() {
		CsvParseResult result = parse("id,name\n1,Ana\n2,Luis");

		assertThat(result.columns()).containsExactly("id", "name");
		assertThat(result.rows()).containsExactly(
				List.of("1", "Ana"),
				List.of("2", "Luis"));
		assertThat(result.truncated()).isFalse();
	}

	@Test
	void shouldPreserveHeaderAndValueWhitespace() {
		CsvParseResult result = parse(" id , name \n 1 , Ana ");

		assertThat(result.columns()).containsExactly(" id ", " name ");
		assertThat(result.rows()).containsExactly(List.of(" 1 ", " Ana "));
	}

	@Test
	void shouldPreserveUtf8Characters() {
		CsvParseResult result = parse("ciudad,descripción\nMálaga,Información");

		assertThat(result.rows()).containsExactly(List.of("Málaga", "Información"));
	}

	@Test
	void shouldAcceptAndRemoveUtf8Bom() {
		byte[] csvBytes = "id,name\n1,Ana".getBytes(StandardCharsets.UTF_8);
		byte[] content = new byte[UTF_8_BOM.length + csvBytes.length];
		System.arraycopy(UTF_8_BOM, 0, content, 0, UTF_8_BOM.length);
		System.arraycopy(csvBytes, 0, content, UTF_8_BOM.length, csvBytes.length);

		CsvParseResult result = csvParser.parse(new ByteArrayInputStream(content));

		assertThat(result.columns()).containsExactly("id", "name");
	}

	@Test
	void shouldParseCommaInsideQuotedField() {
		CsvParseResult result = parse("id,description\n1,\"Madrid, España\"");

		assertThat(result.rows()).containsExactly(List.of("1", "Madrid, España"));
	}

	@Test
	void shouldParseEscapedQuotes() {
		CsvParseResult result = parse("id,quote\n1,\"Dijo \"\"hola\"\"\"");

		assertThat(result.rows()).containsExactly(List.of("1", "Dijo \"hola\""));
	}

	@Test
	void shouldParseLineBreakInsideQuotedField() {
		CsvParseResult result = parse("id,notes\n1,\"first line\nsecond line\"");

		assertThat(result.rows()).containsExactly(List.of("1", "first line\nsecond line"));
	}

	@Test
	void shouldRepresentEmptyFieldsAsEmptyStrings() {
		CsvParseResult result = parse("id,name,email\n1,,");

		assertThat(result.rows()).containsExactly(List.of("1", "", ""));
	}

	@Test
	void shouldIgnoreCompletelyEmptyLines() {
		CsvParseResult result = parse("\n\nid,name\n\n1,Ana\n\n2,Luis\n");

		assertThat(result.rows()).containsExactly(
				List.of("1", "Ana"),
				List.of("2", "Luis"));
	}

	@Test
	void shouldCountCommaOnlyRecordAsValidRow() {
		CsvParseResult result = parse("first,second,third\n,,");

		assertThat(result.rows()).containsExactly(List.of("", "", ""));
	}

	@Test
	void shouldAllowHeaderWithoutDataRows() {
		CsvParseResult result = parse("id,name");

		assertThat(result.columns()).containsExactly("id", "name");
		assertThat(result.rows()).isEmpty();
		assertThat(result.truncated()).isFalse();
	}

	@Test
	void shouldTreatHeaderNamesWithDifferentCaseAsDistinct() {
		CsvParseResult result = parse("id,ID\n1,2");

		assertThat(result.columns()).containsExactly("id", "ID");
	}

	@Test
	void shouldKeepTwentyRowsWithoutTruncating() {
		CsvParseResult result = parse(csvWithRows(20));

		assertThat(result.rows()).hasSize(20);
		assertThat(result.truncated()).isFalse();
	}

	@Test
	void shouldKeepOnlyFirstTwentyRowsAndMarkResultAsTruncated() {
		CsvParseResult result = parse(csvWithRows(21));

		assertThat(result.rows()).hasSize(20);
		assertThat(result.rows().getLast()).containsExactly("20");
		assertThat(result.truncated()).isTrue();
	}

	@Test
	void shouldKeepOnlyFirstTwentyRowsWhenManyRowsExist() {
		CsvParseResult result = parse(csvWithRows(50));

		assertThat(result.rows()).hasSize(20);
		assertThat(result.truncated()).isTrue();
	}

	@Test
	void shouldDetectMalformedContentAfterPreviewLimit() {
		String content = csvWithRows(20) + "\n\"unterminated";

		assertParserError(content, CsvParserError.MALFORMED);
	}

	@Test
	void shouldRejectEmptyContentWithoutHeader() {
		assertParserError("", CsvParserError.HEADER_MISSING);
	}

	@Test
	void shouldRejectContentWithOnlyEmptyLinesWithoutHeader() {
		assertParserError("\n\r\n\n", CsvParserError.HEADER_MISSING);
	}

	@Test
	void shouldRejectBomAndEmptyLinesWithoutHeader() {
		byte[] emptyLines = "\n\n".getBytes(StandardCharsets.UTF_8);
		byte[] content = new byte[UTF_8_BOM.length + emptyLines.length];
		System.arraycopy(UTF_8_BOM, 0, content, 0, UTF_8_BOM.length);
		System.arraycopy(emptyLines, 0, content, UTF_8_BOM.length, emptyLines.length);

		assertThatThrownBy(() -> csvParser.parse(new ByteArrayInputStream(content)))
				.isInstanceOfSatisfying(CsvParserException.class,
						exception -> assertThat(exception.getError()).isEqualTo(CsvParserError.HEADER_MISSING));
	}

	@Test
	void shouldRejectMissingColumnName() {
		assertParserError("id,,email\n1,Ana,a@example.com", CsvParserError.INVALID_HEADER);
	}

	@Test
	void shouldRejectBlankColumnName() {
		assertParserError("id,   ,email\n1,Ana,a@example.com", CsvParserError.INVALID_HEADER);
	}

	@Test
	void shouldRejectDuplicateColumnName() {
		assertParserError("id,name,id\n1,Ana,2", CsvParserError.INVALID_HEADER);
	}

	@Test
	void shouldRejectRecordWithFewerFieldsThanHeader() {
		assertParserError("id,name,email\n1,Ana", CsvParserError.MALFORMED);
	}

	@Test
	void shouldRejectRecordWithMoreFieldsThanHeader() {
		assertParserError("id,name\n1,Ana,extra", CsvParserError.MALFORMED);
	}

	@Test
	void shouldRejectUnclosedQuotedField() {
		assertParserError("id,name\n1,\"Ana", CsvParserError.MALFORMED);
	}

	@Test
	void shouldRejectInvalidUtf8() {
		byte[] header = "id\n".getBytes(StandardCharsets.UTF_8);
		byte[] content = new byte[header.length + 2];
		System.arraycopy(header, 0, content, 0, header.length);
		content[header.length] = (byte) 0xC3;
		content[header.length + 1] = (byte) 0x28;

		assertThatThrownBy(() -> csvParser.parse(new ByteArrayInputStream(content)))
				.isInstanceOfSatisfying(CsvParserException.class,
						exception -> assertThat(exception.getError()).isEqualTo(CsvParserError.MALFORMED));
	}

	@Test
	void shouldReportUnexpectedInputStreamFailure() {
		InputStream failingInputStream = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("Simulated read failure");
			}
		};

		assertThatThrownBy(() -> csvParser.parse(failingInputStream))
				.isInstanceOfSatisfying(CsvParserException.class,
						exception -> assertThat(exception.getError()).isEqualTo(CsvParserError.READ_ERROR));
	}

	@Test
	void shouldReturnDefensiveCopies() {
		CsvParseResult result = parse("id,name\n1,Ana");

		assertThatThrownBy(() -> result.columns().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> result.rows().add(List.of("2", "Luis")))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> result.rows().getFirst().add("other"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void shouldNotCloseOriginalInputStream() throws IOException {
		TrackingInputStream inputStream = new TrackingInputStream(
				"id,name\n1,Ana".getBytes(StandardCharsets.UTF_8));

		try {
			csvParser.parse(inputStream);

			assertThat(inputStream.isClosed()).isFalse();
		}
		finally {
			inputStream.close();
		}
	}

	private CsvParseResult parse(String content) {
		return csvParser.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
	}

	private void assertParserError(String content, CsvParserError expectedError) {
		assertThatThrownBy(() -> parse(content))
				.isInstanceOfSatisfying(CsvParserException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}

	private String csvWithRows(int rowCount) {
		StringBuilder csv = new StringBuilder("id");
		for (int row = 1; row <= rowCount; row++) {
			csv.append('\n').append(row);
		}
		return csv.toString();
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
