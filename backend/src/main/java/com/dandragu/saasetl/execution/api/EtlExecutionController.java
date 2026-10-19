package com.dandragu.saasetl.execution.api;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.execution.application.EtlExecutionHistoryService;
import com.dandragu.saasetl.execution.application.EtlExecutionPage;
import com.dandragu.saasetl.execution.application.EtlExecutionQuery;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;

@RestController
@RequestMapping("/api/etl/executions")
public class EtlExecutionController {

	private final EtlExecutionHistoryService historyService;

	public EtlExecutionController(EtlExecutionHistoryService historyService) {
		this.historyService = historyService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<EtlExecutionResponse>> getExecutions(
			@RequestParam(required = false) EtlExecutionStatus status,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size) {
		EtlExecutionQuery query;
		try {
			query = new EtlExecutionQuery(status, startedFrom, startedTo, page, size);
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidEtlExecutionQueryException(exception.getMessage());
		}

		EtlExecutionPage result = historyService.search(query);
		List<EtlExecutionResponse> response = result.content().stream()
				.map(this::toResponse)
				.toList();

		return ResponseEntity.ok()
				.header("X-Total-Count", Long.toString(result.totalElements()))
				.header("X-Page", Integer.toString(result.page()))
				.header("X-Page-Size", Integer.toString(result.size()))
				.header("X-Total-Pages", Integer.toString(result.totalPages()))
				.body(response);
	}

	@GetMapping(path = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
	public EtlExecutionSummaryResponse getSummary() {
		var summary = historyService.summarize();
		return new EtlExecutionSummaryResponse(
				summary.total(),
				summary.successful(),
				summary.failed(),
				summary.inProgress());
	}

	private EtlExecutionResponse toResponse(EtlExecution execution) {
		return new EtlExecutionResponse(
				execution.id(),
				execution.type(),
				execution.status(),
				execution.startedAt(),
				execution.finishedAt(),
				execution.processedRecordCount(),
				execution.errorMessage(),
				execution.sourceFileName(),
				execution.resultFileName());
	}
}
