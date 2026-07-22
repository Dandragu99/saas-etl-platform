package com.dandragu.saasetl.csv.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvWriterTest {

	@Test
	void shouldWriteUtf8WithLfAndCorrectCsvEscapingWithoutBom() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try (CsvWriter writer = new CsvWriter(output)) {
			writer.writeRecord(List.of("id", "nota", "vacío", "nombre"));
			writer.writeRecord(List.of("1", "Ana, dijo \"hola\"\nsegunda línea", "", " José "));
		}

		byte[] bytes = output.toByteArray();
		assertThat(startsWithUtf8Bom(bytes)).isFalse();
		assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(
				"id,nota,vacío,nombre\n"
						+ "1,\"Ana, dijo \"\"hola\"\"\nsegunda línea\",,\" José \"\n");
		assertThat(new String(bytes, StandardCharsets.UTF_8)).doesNotContain("\r\n");
	}

	private boolean startsWithUtf8Bom(byte[] content) {
		return content.length >= 3
				&& content[0] == (byte) 0xEF
				&& content[1] == (byte) 0xBB
				&& content[2] == (byte) 0xBF;
	}
}
