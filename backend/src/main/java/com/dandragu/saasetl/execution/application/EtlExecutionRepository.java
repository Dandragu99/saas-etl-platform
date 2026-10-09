package com.dandragu.saasetl.execution.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.dandragu.saasetl.execution.domain.EtlExecution;

public interface EtlExecutionRepository {

	EtlExecution save(EtlExecution execution);

	Optional<EtlExecution> findById(UUID id);

	Optional<EtlExecution> update(UUID id, UnaryOperator<EtlExecution> updateOperation);

	List<EtlExecution> findAll();
}
