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
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.company.infrastructure.persistence.JpaCompanyRepository;

@SpringBootTest
@Transactional
class JpaEtlExecutionRepositoryTest {

	@Autowired
	private JpaEtlExecutionRepository repository;

	@Autowired
	private JpaCompanyRepository companyRepository;

	@Test
	void shouldPersistUpdateFilterAndSummarizeExecutions() {
		Instant start = Instant.parse("2026-10-14T10:00:00Z");
		UUID companyId = UUID.randomUUID();
		companyRepository.save(Company.create(companyId, "Nexo Test", new CurrencyCode("EUR"), start));
		EtlExecution pending = EtlExecution.pending(
				UUID.randomUUID(), companyId, EtlExecutionType.REMOVE_COLUMN, start, "clientes.csv");
		repository.save(pending);

		repository.update(companyId, pending.id(), EtlExecution::start);
		repository.update(
				companyId,
				pending.id(),
				execution -> execution.succeed(
						Instant.parse("2026-10-14T10:00:02Z"), 12, "clientes-sin-email.csv"));

		EtlExecution stored = repository.findByCompanyIdAndId(companyId, pending.id()).orElseThrow();
		assertThat(stored.status()).isEqualTo(EtlExecutionStatus.SUCCESS);
		assertThat(stored.processedRecordCount()).isEqualTo(12);

		var page = repository.search(companyId, new EtlExecutionQuery(
				EtlExecutionStatus.SUCCESS,
				start.minusSeconds(1),
				start.plusSeconds(1),
				0,
				10));
		assertThat(page.content()).extracting(EtlExecution::id).contains(pending.id());
		assertThat(repository.summarize(companyId).successful()).isEqualTo(1);
		assertThat(repository.search(UUID.randomUUID(), new EtlExecutionQuery(null, null, null, 0, 10))
				.content()).isEmpty();
	}
}
