package com.dandragu.saasetl.execution.application;

public record EtlExecutionSummary(
		long total,
		long successful,
		long failed,
		long inProgress) {
}
