package com.dandragu.saasetl.execution.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EtlExecution(
		UUID id,
		EtlExecutionType type,
		EtlExecutionStatus status,
		Instant startedAt,
		Instant finishedAt,
		Long processedRecordCount,
		String errorMessage) {

	public EtlExecution {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(startedAt, "startedAt must not be null");
		if (processedRecordCount != null && processedRecordCount < 0) {
			throw new IllegalArgumentException("processedRecordCount must not be negative");
		}
	}

	public static EtlExecution pending(UUID id, EtlExecutionType type, Instant startedAt) {
		return new EtlExecution(
				id,
				type,
				EtlExecutionStatus.PENDING,
				startedAt,
				null,
				null,
				null);
	}

	public EtlExecution start() {
		requireStatus(EtlExecutionStatus.PENDING);
		return new EtlExecution(
				id,
				type,
				EtlExecutionStatus.RUNNING,
				startedAt,
				null,
				null,
				null);
	}

	public EtlExecution succeed(Instant finishedAt, long processedRecordCount) {
		requireStatus(EtlExecutionStatus.RUNNING);
		Objects.requireNonNull(finishedAt, "finishedAt must not be null");
		return new EtlExecution(
				id,
				type,
				EtlExecutionStatus.SUCCESS,
				startedAt,
				finishedAt,
				processedRecordCount,
				null);
	}

	public EtlExecution fail(Instant finishedAt, String errorMessage) {
		requireStatus(EtlExecutionStatus.RUNNING);
		Objects.requireNonNull(finishedAt, "finishedAt must not be null");
		Objects.requireNonNull(errorMessage, "errorMessage must not be null");
		return new EtlExecution(
				id,
				type,
				EtlExecutionStatus.FAILED,
				startedAt,
				finishedAt,
				null,
				errorMessage);
	}

	private void requireStatus(EtlExecutionStatus expectedStatus) {
		if (status != expectedStatus) {
			throw new IllegalStateException(
					"Execution must be " + expectedStatus + " but was " + status);
		}
	}
}
