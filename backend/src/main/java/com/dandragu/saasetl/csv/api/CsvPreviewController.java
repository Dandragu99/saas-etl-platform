package com.dandragu.saasetl.csv.api;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dandragu.saasetl.csv.application.CsvPreviewCommand;
import com.dandragu.saasetl.csv.application.CsvPreviewResult;
import com.dandragu.saasetl.csv.application.CsvPreviewService;

@RestController
@RequestMapping("/api/csv")
public class CsvPreviewController {

	private final CsvPreviewService csvPreviewService;

	public CsvPreviewController(CsvPreviewService csvPreviewService) {
		this.csvPreviewService = csvPreviewService;
	}

	@PostMapping(
			path = "/preview",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public CsvPreviewResponse preview(@RequestPart("file") MultipartFile file) throws IOException {
		CsvPreviewCommand command = new CsvPreviewCommand(
				file.getOriginalFilename(),
				file.getSize(),
				file.getInputStream());

		return toResponse(csvPreviewService.preview(command));
	}

	private CsvPreviewResponse toResponse(CsvPreviewResult result) {
		List<Map<String, String>> rows = result.rows().stream()
				.map(row -> toOrderedRow(result.columns(), row))
				.toList();

		return new CsvPreviewResponse(
				result.fileName(),
				result.columns(),
				rows,
				result.previewRowCount(),
				result.truncated());
	}

	private Map<String, String> toOrderedRow(List<String> columns, List<String> values) {
		Map<String, String> row = new LinkedHashMap<>();
		for (int index = 0; index < columns.size(); index++) {
			row.put(columns.get(index), values.get(index));
		}
		return Collections.unmodifiableMap(row);
	}
}
