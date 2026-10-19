package com.dandragu.saasetl.execution.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.dandragu.saasetl.execution.domain.EtlExecution;

public interface EtlExecutionRepository {

	EtlExecution save(EtlExecution execution);

	Optional<EtlExecution> findByCompanyIdAndId(UUID companyId, UUID id);

	Optional<EtlExecution> update(
			UUID companyId,
			UUID id,
			UnaryOperator<EtlExecution> updateOperation);

	List<EtlExecution> findAllByCompanyId(UUID companyId);

	EtlExecutionPage search(UUID companyId, EtlExecutionQuery query);

	EtlExecutionSummary summarize(UUID companyId);
}
