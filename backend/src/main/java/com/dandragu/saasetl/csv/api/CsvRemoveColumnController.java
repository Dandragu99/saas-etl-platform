package com.dandragu.saasetl.csv.api;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dandragu.saasetl.csv.application.CsvPreviewCommand;
import com.dandragu.saasetl.csv.application.RemoveColumnCommand;
import com.dandragu.saasetl.csv.application.RemoveColumnResult;
import com.dandragu.saasetl.csv.application.RemoveColumnService;

@RestController
@RequestMapping("/api/csv/transform")
public class CsvRemoveColumnController {

	private static final String TRANSFORMATION_TYPE = "REMOVE_COLUMN";

	private final RemoveColumnService removeColumnService;

	public CsvRemoveColumnController(RemoveColumnService removeColumnService) {
		this.removeColumnService = removeColumnService;
	}

	@PostMapping(
			path = "/remove-column",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public CsvRemoveColumnResponse removeColumn(
			@RequestPart("file") MultipartFile file,
			@RequestParam(name = "column", required = false) String column) throws IOException {
		CsvPreviewCommand fileCommand = new CsvPreviewCommand(
				file.getOriginalFilename(),
				file.getSize(),
				file.getInputStream());

		return toResponse(removeColumnService.remove(new RemoveColumnCommand(fileCommand, column)));
	}

	private CsvRemoveColumnResponse toResponse(RemoveColumnResult result) {
		List<Map<String, String>> rows = result.rows().stream()
				.map(row -> toOrderedRow(result.columns(), row))
				.toList();

		return new CsvRemoveColumnResponse(
				result.fileName(),
				result.columns(),
				rows,
				result.previewRowCount(),
				result.truncated(),
				new RemoveColumnTransformationResponse(
						TRANSFORMATION_TYPE,
						result.removedColumn()));
	}

	private Map<String, String> toOrderedRow(List<String> columns, List<String> values) {
		Map<String, String> row = new LinkedHashMap<>();
		for (int index = 0; index < columns.size(); index++) {
			row.put(columns.get(index), values.get(index));
		}
		return Collections.unmodifiableMap(row);
	}
}
