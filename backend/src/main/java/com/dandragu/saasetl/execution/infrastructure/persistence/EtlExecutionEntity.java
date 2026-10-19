package com.dandragu.saasetl.execution.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "etl_executions")
class EtlExecutionEntity {

	@Id
	private UUID id;

	@Column(name = "company_id")
	private UUID companyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private EtlExecutionType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EtlExecutionStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "processed_record_count")
	private Long processedRecordCount;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "source_file_name", length = 255)
	private String sourceFileName;

	@Column(name = "result_file_name", length = 255)
	private String resultFileName;

	protected EtlExecutionEntity() {
	}

	private EtlExecutionEntity(EtlExecution execution) {
		this.id = execution.id();
		updateFrom(execution);
	}

	static EtlExecutionEntity fromDomain(EtlExecution execution) {
		return new EtlExecutionEntity(execution);
	}

	void updateFrom(EtlExecution execution) {
		this.companyId = execution.companyId();
		this.type = execution.type();
		this.status = execution.status();
		this.startedAt = execution.startedAt();
		this.finishedAt = execution.finishedAt();
		this.processedRecordCount = execution.processedRecordCount();
		this.errorMessage = execution.errorMessage();
		this.sourceFileName = execution.sourceFileName();
		this.resultFileName = execution.resultFileName();
	}

	EtlExecution toDomain() {
		return new EtlExecution(
				id,
				companyId,
				type,
				status,
				startedAt,
				finishedAt,
				processedRecordCount,
				errorMessage,
				sourceFileName,
				resultFileName);
	}
}
