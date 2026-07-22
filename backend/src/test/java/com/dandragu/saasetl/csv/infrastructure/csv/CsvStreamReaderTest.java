package com.dandragu.saasetl.csv.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserError;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserException;

class CsvStreamReaderTest {

	private final CsvStreamReader reader = new CsvStreamReader();

	@Test
	void shouldDeliverHeaderAndEveryRowWithoutAccumulatingThem() {
		StringBuilder csv = new StringBuilder("id,name\n");
		for (int index = 1; index <= 25; index++) {
			csv.append(index).append(",name-").append(index).append('\n');
		}
		CollectingConsumer consumer = new CollectingConsumer();

		reader.read(stream(csv.toString()), consumer);

		assertThat(consumer.columns).containsExactly("id", "name");
		assertThat(consumer.rows).hasSize(25);
		assertThat(consumer.rows.getLast()).containsExactly("25", "name-25");
	}

	@Test
	void shouldAcceptUtf8BomAndPreserveUtf8AndLiteralValues() {
		byte[] content = "id,nota\n1, café ".getBytes(StandardCharsets.UTF_8);
		byte[] withBom = new byte[content.length + 3];
		withBom[0] = (byte) 0xEF;
		withBom[1] = (byte) 0xBB;
		withBom[2] = (byte) 0xBF;
		System.arraycopy(content, 0, withBom, 3, content.length);
		CollectingConsumer consumer = new CollectingConsumer();

		reader.read(new ByteArrayInputStream(withBom), consumer);

		assertThat(consumer.columns).containsExactly("id", "nota");
		assertThat(consumer.rows).containsExactly(List.of("1", " café "));
	}

	@Test
	void shouldUnderstandQuotedCommasQuotesAndLineBreaks() {
		CollectingConsumer consumer = new CollectingConsumer();

		reader.read(stream("id,nota\n1,\"Ana, dijo \"\"hola\"\"\nsegunda línea\""), consumer);

		assertThat(consumer.rows).containsExactly(
				List.of("1", "Ana, dijo \"hola\"\nsegunda línea"));
	}

	@Test
	void shouldIgnoreEmptyLinesButKeepACommaOnlyRow() {
		CollectingConsumer consumer = new CollectingConsumer();

		reader.read(stream("a,b,c\n\n,,\n"), consumer);

		assertThat(consumer.rows).containsExactly(List.of("", "", ""));
	}

	@Test
	void shouldRejectMalformedUtf8AndDifferentRecordSize() {
		assertReaderError(
				new ByteArrayInputStream(new byte[] { 'i', 'd', '\n', (byte) 0xC3, 0x28 }),
				CsvParserError.MALFORMED);
		assertReaderError(stream("id,name\n1"), CsvParserError.MALFORMED);
	}

	@Test
	void shouldRejectMissingAndInvalidHeaders() {
		assertReaderError(stream("\n\n"), CsvParserError.HEADER_MISSING);
		assertReaderError(stream("id,id\n1,2"), CsvParserError.INVALID_HEADER);
		assertReaderError(stream("id, \n1,2"), CsvParserError.INVALID_HEADER);
	}

	@Test
	void shouldLeaveOriginalInputStreamOpen() {
		TrackingInputStream inputStream = new TrackingInputStream(
				"id\n1".getBytes(StandardCharsets.UTF_8));

		reader.read(inputStream, new CollectingConsumer());

		assertThat(inputStream.isClosed()).isFalse();
	}

	private void assertReaderError(ByteArrayInputStream inputStream, CsvParserError expectedError) {
		assertThatThrownBy(() -> reader.read(inputStream, new CollectingConsumer()))
				.isInstanceOfSatisfying(
						CsvParserException.class,
						exception -> assertThat(exception.getError()).isEqualTo(expectedError));
	}

	private ByteArrayInputStream stream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	private static final class CollectingConsumer implements CsvRecordConsumer {

		private List<String> columns = List.of();
		private final List<List<String>> rows = new ArrayList<>();

		@Override
		public void acceptHeader(List<String> columns) {
			this.columns = columns;
		}

		@Override
		public void acceptRow(List<String> row) {
			rows.add(row);
		}
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
