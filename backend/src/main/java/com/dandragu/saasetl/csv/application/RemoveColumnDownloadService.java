package com.dandragu.saasetl.csv.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dandragu.saasetl.csv.domain.transformation.RemoveColumnPlan;
import com.dandragu.saasetl.csv.domain.transformation.RemoveColumnTransformationException;
import com.dandragu.saasetl.csv.infrastructure.csv.CsvRecordConsumer;
import com.dandragu.saasetl.csv.infrastructure.csv.CsvStreamReader;
import com.dandragu.saasetl.csv.infrastructure.csv.CsvWriter;
import com.dandragu.saasetl.csv.infrastructure.csv.CsvWriterException;
import com.dandragu.saasetl.csv.infrastructure.parser.CsvParserException;

@Service
public class RemoveColumnDownloadService {

	static final int MAX_DOWNLOAD_FILE_NAME_LENGTH = 180;
	static final int MAX_COLUMN_FILE_NAME_FRAGMENT_LENGTH = 50;

	private final CsvFileValidator csvFileValidator;
	private final CsvStreamReader csvStreamReader;

	public RemoveColumnDownloadService(
			CsvFileValidator csvFileValidator,
			CsvStreamReader csvStreamReader) {
		this.csvFileValidator = csvFileValidator;
		this.csvStreamReader = csvStreamReader;
	}

	public RemoveColumnDownloadResult download(RemoveColumnDownloadCommand command) {
		CsvPreviewCommand file = command == null ? null : command.file();
		InputStream inputStream = file == null ? null : file.content();
		if (inputStream == null) {
			csvFileValidator.validateAndGetSafeFileName(file);
		}

		try (InputStream content = inputStream;
				DownloadConsumer consumer = new DownloadConsumer(command.column())) {
			String safeFileName = csvFileValidator.validateAndGetSafeFileName(file);
			csvStreamReader.read(content, consumer);
			byte[] transformedContent = consumer.finish();

			return new RemoveColumnDownloadResult(
					createDownloadFileName(safeFileName, command.column()),
					transformedContent);
		}
		catch (CsvPreviewException | RemoveColumnException exception) {
			throw exception;
		}
		catch (CsvParserException exception) {
			throw new CsvPreviewException(
					CsvPreviewError.valueOf(exception.getError().name()),
					"The CSV file could not be processed.",
					exception);
		}
		catch (CsvWriterException | IOException exception) {
			throw new CsvPreviewException(
					CsvPreviewError.READ_ERROR,
					"The transformed CSV file could not be generated.",
					exception);
		}
	}

	private String createDownloadFileName(String safeFileName, String column) {
		String baseName = safeFileName.substring(0, safeFileName.length() - 4);
		String safeBaseName = sanitizeFileNameFragment(baseName, true);
		if (safeBaseName.isEmpty()) {
			safeBaseName = "archivo";
		}

		String safeColumn = sanitizeFileNameFragment(column, false);
		if (safeColumn.isEmpty()) {
			safeColumn = "columna";
		}
		safeColumn = truncateByCodePoints(safeColumn, MAX_COLUMN_FILE_NAME_FRAGMENT_LENGTH);

		String suffix = "-sin-" + safeColumn + ".csv";
		int maximumBaseLength = MAX_DOWNLOAD_FILE_NAME_LENGTH
				- suffix.codePointCount(0, suffix.length());
		safeBaseName = truncateByCodePoints(safeBaseName, maximumBaseLength);

		return safeBaseName + suffix;
	}

	private String sanitizeFileNameFragment(String value, boolean allowDot) {
		StringBuilder result = new StringBuilder();
		boolean previousWasSeparator = false;

		for (int offset = 0; offset < value.length();) {
			int codePoint = value.codePointAt(offset);
			offset += Character.charCount(codePoint);

			boolean allowed = Character.isLetterOrDigit(codePoint)
					|| codePoint == '_'
					|| codePoint == '-'
					|| (allowDot && codePoint == '.');
			if (allowed) {
				result.appendCodePoint(codePoint);
				previousWasSeparator = false;
			}
			else if (!previousWasSeparator && !result.isEmpty()) {
				result.append('-');
				previousWasSeparator = true;
			}
		}

		return removeEdgeSeparators(result.toString());
	}

	private String removeEdgeSeparators(String value) {
		int start = 0;
		int end = value.length();
		while (start < end && isEdgeSeparator(value.charAt(start))) {
			start++;
		}
		while (end > start && isEdgeSeparator(value.charAt(end - 1))) {
			end--;
		}
		return value.substring(start, end);
	}

	private boolean isEdgeSeparator(char character) {
		return character == '.' || character == '-' || character == '_';
	}

	private String truncateByCodePoints(String value, int maximumLength) {
		int codePointCount = value.codePointCount(0, value.length());
		if (codePointCount <= maximumLength) {
			return value;
		}
		int endIndex = value.offsetByCodePoints(0, maximumLength);
		return value.substring(0, endIndex);
	}

	static final class DownloadConsumer implements CsvRecordConsumer, AutoCloseable {

		private final String column;
		private RemoveColumnPlan plan;
		private RemoveColumnError pendingError;
		private ByteArrayOutputStream output;
		private CsvWriter writer;

		DownloadConsumer(String column) {
			this.column = column;
		}

		@Override
		public void acceptHeader(List<String> columns) {
			if (column == null || column.isBlank()) {
				pendingError = RemoveColumnError.COLUMN_REQUIRED;
				return;
			}

			try {
				plan = RemoveColumnPlan.prepare(columns, column);
			}
			catch (RemoveColumnTransformationException exception) {
				pendingError = RemoveColumnError.valueOf(exception.getError().name());
				return;
			}

			output = new ByteArrayOutputStream();
			writer = new CsvWriter(output);
			writer.writeRecord(plan.columns());
		}

		@Override
		public void acceptRow(List<String> row) {
			if (writer != null) {
				writer.writeRecord(plan.applyToRow(row));
			}
		}

		private byte[] finish() {
			if (pendingError != null) {
				throw new RemoveColumnException(
						pendingError,
						"The remove-column transformation could not be applied.");
			}

			close();
			return output.toByteArray();
		}

		boolean hasOutputBuffer() {
			return output != null;
		}

		@Override
		public void close() {
			if (writer != null) {
				CsvWriter currentWriter = writer;
				writer = null;
				currentWriter.close();
			}
		}
	}
}
