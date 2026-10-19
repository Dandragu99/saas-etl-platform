package com.dandragu.saasetl.execution.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;
import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.application.CompanyPermission;

@Service
public class EtlExecutionHistoryService {

	public static final String SAFE_FAILURE_MESSAGE = "La ejecución no pudo completarse.";

	private final EtlExecutionRepository repository;
	private final CompanyAuthorizationService authorizationService;

	public EtlExecutionHistoryService(
			EtlExecutionRepository repository,
			CompanyAuthorizationService authorizationService) {
		this.repository = repository;
		this.authorizationService = authorizationService;
	}

	public EtlExecution createPending(EtlExecutionType type, String sourceFileName) {
		var access = authorizationService.require(CompanyPermission.ETL_EXECUTE);
		return repository.save(EtlExecution.pending(
				UUID.randomUUID(),
				access.company().id(),
				type,
				Instant.now(),
				sourceFileName));
	}

	public void markRunning(UUID companyId, UUID executionId) {
		repository.update(companyId, executionId, EtlExecution::start);
	}

	public void markSuccess(
			UUID companyId,
			UUID executionId,
			long processedRecordCount,
			String resultFileName) {
		repository.update(
				companyId,
				executionId,
				execution -> execution.succeed(
						Instant.now(), processedRecordCount, resultFileName));
	}

	public void markFailed(UUID companyId, UUID executionId) {
		repository.update(
				companyId,
				executionId,
				execution -> execution.fail(Instant.now(), SAFE_FAILURE_MESSAGE));
	}

	public List<EtlExecution> findAll() {
		UUID companyId = requireReadCompanyId();
		return repository.findAllByCompanyId(companyId);
	}

	public EtlExecutionPage search(EtlExecutionQuery query) {
		return repository.search(requireReadCompanyId(), query);
	}

	public EtlExecutionSummary summarize() {
		return repository.summarize(requireReadCompanyId());
	}

	private UUID requireReadCompanyId() {
		return authorizationService.require(CompanyPermission.ETL_READ).company().id();
	}
}
