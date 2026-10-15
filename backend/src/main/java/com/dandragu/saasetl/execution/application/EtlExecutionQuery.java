package com.dandragu.saasetl.execution.application;

import java.time.Instant;

import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;

public record EtlExecutionQuery(
		EtlExecutionStatus status,
		Instant startedFrom,
		Instant startedTo,
		int page,
		int size) {

	public static final int MAX_PAGE_SIZE = 100;

	public EtlExecutionQuery {
		if (page < 0) {
			throw new IllegalArgumentException("page must not be negative");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		if (startedFrom != null && startedTo != null && startedFrom.isAfter(startedTo)) {
			throw new IllegalArgumentException("startedFrom must not be after startedTo");
		}
	}
}
