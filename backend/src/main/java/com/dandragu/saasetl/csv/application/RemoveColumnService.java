package com.dandragu.saasetl.csv.application;

import org.springframework.stereotype.Service;

import com.dandragu.saasetl.csv.domain.transformation.RemoveColumnTransformation;
import com.dandragu.saasetl.csv.domain.transformation.RemoveColumnTransformationException;
import com.dandragu.saasetl.csv.domain.transformation.RemoveColumnTransformationResult;

@Service
public class RemoveColumnService {

	private final CsvPreviewService csvPreviewService;
	private final RemoveColumnTransformation transformation = new RemoveColumnTransformation();

	public RemoveColumnService(CsvPreviewService csvPreviewService) {
		this.csvPreviewService = csvPreviewService;
	}

	public RemoveColumnResult remove(RemoveColumnCommand command) {
		CsvPreviewResult preview = csvPreviewService.preview(command == null ? null : command.file());
		String column = command.column();
		validateColumn(column);

		RemoveColumnTransformationResult transformed = applyTransformation(preview, column);
		return new RemoveColumnResult(
				preview.fileName(),
				transformed.columns(),
				transformed.rows(),
				preview.previewRowCount(),
				preview.truncated(),
				column);
	}

	private void validateColumn(String column) {
		if (column == null || column.isBlank()) {
			throw new RemoveColumnException(
					RemoveColumnError.COLUMN_REQUIRED,
					"A column name is required.");
		}
	}

	private RemoveColumnTransformationResult applyTransformation(
			CsvPreviewResult preview,
			String column) {
		try {
			return transformation.apply(preview.columns(), preview.rows(), column);
		}
		catch (RemoveColumnTransformationException exception) {
			throw new RemoveColumnException(
					RemoveColumnError.valueOf(exception.getError().name()),
					"The remove-column transformation could not be applied.",
					exception);
		}
	}
}
