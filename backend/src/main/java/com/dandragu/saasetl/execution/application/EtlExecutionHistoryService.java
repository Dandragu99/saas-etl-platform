package com.dandragu.saasetl.execution.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;

@Service
public class EtlExecutionHistoryService {

	public static final String SAFE_FAILURE_MESSAGE = "La ejecución no pudo completarse.";

	private final EtlExecutionRepository repository;

	public EtlExecutionHistoryService(EtlExecutionRepository repository) {
		this.repository = repository;
	}

	public EtlExecution createPending(EtlExecutionType type) {
		return repository.save(EtlExecution.pending(UUID.randomUUID(), type, Instant.now()));
	}

	public void markRunning(UUID executionId) {
		repository.update(executionId, EtlExecution::start);
	}

	public void markSuccess(UUID executionId, long processedRecordCount) {
		repository.update(
				executionId,
				execution -> execution.succeed(Instant.now(), processedRecordCount));
	}

	public void markFailed(UUID executionId) {
		repository.update(
				executionId,
				execution -> execution.fail(Instant.now(), SAFE_FAILURE_MESSAGE));
	}

	public List<EtlExecution> findAll() {
		return repository.findAll();
	}
}
