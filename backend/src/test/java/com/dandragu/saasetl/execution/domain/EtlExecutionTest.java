package com.dandragu.saasetl.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EtlExecutionTest {

	@Test
	void shouldMoveThroughSuccessfulLifecycle() {
		Instant startedAt = Instant.parse("2026-10-09T10:00:00Z");
		Instant finishedAt = Instant.parse("2026-10-09T10:00:02Z");
		EtlExecution pending = EtlExecution.pending(
				UUID.randomUUID(),
				EtlExecutionType.REMOVE_COLUMN,
				startedAt);

		EtlExecution success = pending.start().succeed(finishedAt, 25);

		assertThat(success.status()).isEqualTo(EtlExecutionStatus.SUCCESS);
		assertThat(success.startedAt()).isEqualTo(startedAt);
		assertThat(success.finishedAt()).isEqualTo(finishedAt);
		assertThat(success.processedRecordCount()).isEqualTo(25);
		assertThat(success.errorMessage()).isNull();
	}

	@Test
	void shouldMoveToFailedWithoutProcessedRecordCount() {
		EtlExecution running = EtlExecution.pending(
				UUID.randomUUID(),
				EtlExecutionType.REMOVE_COLUMN,
				Instant.parse("2026-10-09T10:00:00Z"))
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
				EtlExecutionType.REMOVE_COLUMN,
				Instant.now());

		assertThatThrownBy(() -> pending.succeed(Instant.now(), 1))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> pending.start().succeed(Instant.now(), -1))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
