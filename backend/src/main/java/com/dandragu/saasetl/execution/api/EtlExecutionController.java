package com.dandragu.saasetl.execution.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.execution.application.EtlExecutionHistoryService;
import com.dandragu.saasetl.execution.domain.EtlExecution;

@RestController
@RequestMapping("/api/etl/executions")
public class EtlExecutionController {

	private final EtlExecutionHistoryService historyService;

	public EtlExecutionController(EtlExecutionHistoryService historyService) {
		this.historyService = historyService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<EtlExecutionResponse> getExecutions() {
		return historyService.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	private EtlExecutionResponse toResponse(EtlExecution execution) {
		return new EtlExecutionResponse(
				execution.id(),
				execution.type(),
				execution.status(),
				execution.startedAt(),
				execution.finishedAt(),
				execution.processedRecordCount(),
				execution.errorMessage());
	}
}
