package com.dandragu.saasetl.execution.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.execution.application.EtlExecutionQuery;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;

@SpringBootTest
@Transactional
class JpaEtlExecutionRepositoryTest {

	@Autowired
	private JpaEtlExecutionRepository repository;

	@Test
	void shouldPersistUpdateFilterAndSummarizeExecutions() {
		Instant start = Instant.parse("2026-10-14T10:00:00Z");
		EtlExecution pending = EtlExecution.pending(UUID.randomUUID(), EtlExecutionType.REMOVE_COLUMN, start);
		repository.save(pending);

		repository.update(pending.id(), EtlExecution::start);
		repository.update(
				pending.id(),
				execution -> execution.succeed(Instant.parse("2026-10-14T10:00:02Z"), 12));

		EtlExecution stored = repository.findById(pending.id()).orElseThrow();
		assertThat(stored.status()).isEqualTo(EtlExecutionStatus.SUCCESS);
		assertThat(stored.processedRecordCount()).isEqualTo(12);

		var page = repository.search(new EtlExecutionQuery(
				EtlExecutionStatus.SUCCESS,
				start.minusSeconds(1),
				start.plusSeconds(1),
				0,
				10));
		assertThat(page.content()).extracting(EtlExecution::id).contains(pending.id());
		assertThat(repository.summarize().successful()).isGreaterThanOrEqualTo(1);
	}
}
