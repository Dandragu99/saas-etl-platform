package com.dandragu.saasetl.execution.infrastructure.persistence;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;

import jakarta.persistence.LockModeType;

interface SpringDataEtlExecutionRepository extends JpaRepository<EtlExecutionEntity, UUID>,
		JpaSpecificationExecutor<EtlExecutionEntity> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select execution from EtlExecutionEntity execution
			where execution.companyId = :companyId and execution.id = :id
			""")
	Optional<EtlExecutionEntity> findByCompanyIdAndIdForUpdate(
			@Param("companyId") UUID companyId,
			@Param("id") UUID id);

	Optional<EtlExecutionEntity> findByCompanyIdAndId(UUID companyId, UUID id);

	long countByCompanyId(UUID companyId);

	long countByCompanyIdAndStatus(UUID companyId, EtlExecutionStatus status);

	long countByCompanyIdAndStatusIn(UUID companyId, Collection<EtlExecutionStatus> statuses);
}
