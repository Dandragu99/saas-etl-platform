package com.dandragu.saasetl.execution.api;

import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;

public record EtlExecutionResponse(
		UUID id,
		EtlExecutionType type,
		EtlExecutionStatus status,
		Instant startedAt,
		Instant finishedAt,
		Long processedRecordCount,
		String errorMessage) {
}
