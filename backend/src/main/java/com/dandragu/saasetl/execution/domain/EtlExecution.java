package com.dandragu.saasetl.execution.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EtlExecution(
		UUID id,
		UUID companyId,
		EtlExecutionType type,
		EtlExecutionStatus status,
		Instant startedAt,
		Instant finishedAt,
		Long processedRecordCount,
		String errorMessage,
		String sourceFileName,
		String resultFileName) {

	public EtlExecution {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(startedAt, "startedAt must not be null");
		validateFileName(sourceFileName, "sourceFileName");
		validateFileName(resultFileName, "resultFileName");
		if (processedRecordCount != null && processedRecordCount < 0) {
			throw new IllegalArgumentException("processedRecordCount must not be negative");
		}
	}

	public static EtlExecution pending(
			UUID id,
			UUID companyId,
			EtlExecutionType type,
			Instant startedAt,
			String sourceFileName) {
		Objects.requireNonNull(companyId, "companyId must not be null for a new execution");
		return new EtlExecution(
				id,
				companyId,
				type,
				EtlExecutionStatus.PENDING,
				startedAt,
				null,
				null,
				null,
				sourceFileName,
				null);
	}

	public EtlExecution start() {
		requireStatus(EtlExecutionStatus.PENDING);
		return new EtlExecution(
				id,
				companyId,
				type,
				EtlExecutionStatus.RUNNING,
				startedAt,
				null,
				null,
				null,
				sourceFileName,
				null);
	}

	public EtlExecution succeed(
			Instant finishedAt,
			long processedRecordCount,
			String resultFileName) {
		requireStatus(EtlExecutionStatus.RUNNING);
		Objects.requireNonNull(finishedAt, "finishedAt must not be null");
		return new EtlExecution(
				id,
				companyId,
				type,
				EtlExecutionStatus.SUCCESS,
				startedAt,
				finishedAt,
				processedRecordCount,
				null,
				sourceFileName,
				resultFileName);
	}

	public EtlExecution fail(Instant finishedAt, String errorMessage) {
		requireStatus(EtlExecutionStatus.RUNNING);
		Objects.requireNonNull(finishedAt, "finishedAt must not be null");
		Objects.requireNonNull(errorMessage, "errorMessage must not be null");
		return new EtlExecution(
				id,
				companyId,
				type,
				EtlExecutionStatus.FAILED,
				startedAt,
				finishedAt,
				null,
				errorMessage,
				sourceFileName,
				null);
	}

	public boolean isLegacy() {
		return companyId == null;
	}

	private static void validateFileName(String fileName, String fieldName) {
		if (fileName != null
				&& (fileName.isBlank() || fileName.codePointCount(0, fileName.length()) > 255)) {
			throw new IllegalArgumentException(fieldName + " must contain between 1 and 255 characters");
		}
	}

	private void requireStatus(EtlExecutionStatus expectedStatus) {
		if (status != expectedStatus) {
			throw new IllegalStateException(
					"Execution must be " + expectedStatus + " but was " + status);
		}
	}
}
