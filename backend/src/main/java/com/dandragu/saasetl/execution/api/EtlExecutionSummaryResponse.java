package com.dandragu.saasetl.execution.api;

public record EtlExecutionSummaryResponse(
		long total,
		long successful,
		long failed,
		long inProgress) {
}
