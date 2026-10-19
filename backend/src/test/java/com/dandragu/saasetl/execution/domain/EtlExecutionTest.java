package com.dandragu.saasetl.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EtlExecutionTest {
	private static final UUID COMPANY_ID = UUID.randomUUID();

	@Test
	void shouldMoveThroughSuccessfulLifecycle() {
		Instant startedAt = Instant.parse("2026-10-09T10:00:00Z");
		Instant finishedAt = Instant.parse("2026-10-09T10:00:02Z");
		EtlExecution pending = EtlExecution.pending(
				UUID.randomUUID(),
				COMPANY_ID,
				EtlExecutionType.REMOVE_COLUMN,
				startedAt,
				"clientes.csv");

		EtlExecution success = pending.start().succeed(finishedAt, 25, "clientes-sin-email.csv");

		assertThat(success.status()).isEqualTo(EtlExecutionStatus.SUCCESS);
		assertThat(success.startedAt()).isEqualTo(startedAt);
		assertThat(success.finishedAt()).isEqualTo(finishedAt);
		assertThat(success.processedRecordCount()).isEqualTo(25);
		assertThat(success.errorMessage()).isNull();
		assertThat(success.companyId()).isEqualTo(COMPANY_ID);
		assertThat(success.sourceFileName()).isEqualTo("clientes.csv");
		assertThat(success.resultFileName()).isEqualTo("clientes-sin-email.csv");
	}

	@Test
	void shouldMoveToFailedWithoutProcessedRecordCount() {
		EtlExecution running = EtlExecution.pending(
				UUID.randomUUID(),
				COMPANY_ID,
				EtlExecutionType.REMOVE_COLUMN,
				Instant.parse("2026-10-09T10:00:00Z"),
				"clientes.csv")
				.start();

		EtlExecution failed = running.fail(
				Instant.parse("2026-10-09T10:00:01Z"),
				"La ejecución no pudo completarse.");

		assertThat(failed.status()).isEqualTo(EtlExecutionStatus.FAILED);
		assertThat(failed.processedRecordCount()).isNull();
		assertThat(failed.errorMessage()).isEqualTo("La ejecución no pudo completarse.");
	}

	@Test
	void shouldRejectInvalidTransitionsAndNegativeCounts() {
		EtlExecution pending = EtlExecution.pending(
				UUID.randomUUID(),
				COMPANY_ID,
				EtlExecutionType.REMOVE_COLUMN,
				Instant.now(),
				null);

		assertThatThrownBy(() -> pending.succeed(Instant.now(), 1, "result.csv"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> pending.start().succeed(Instant.now(), -1, "result.csv"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
