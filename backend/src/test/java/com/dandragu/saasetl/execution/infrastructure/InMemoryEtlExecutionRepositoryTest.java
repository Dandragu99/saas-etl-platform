package com.dandragu.saasetl.execution.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;

class InMemoryEtlExecutionRepositoryTest {

	@Test
	void shouldKeepOnlyLatestOneHundredExecutions() {
		InMemoryEtlExecutionRepository repository = new InMemoryEtlExecutionRepository();
		Instant firstStart = Instant.parse("2026-10-09T10:00:00Z");
		List<UUID> ids = new ArrayList<>();

		for (int index = 0; index < 105; index++) {
			UUID id = UUID.randomUUID();
			ids.add(id);
			repository.save(EtlExecution.pending(
					id,
					EtlExecutionType.REMOVE_COLUMN,
					firstStart.plusSeconds(index)));
		}

		List<EtlExecution> history = repository.findAll();
		assertThat(history).hasSize(InMemoryEtlExecutionRepository.MAX_HISTORY_SIZE);
		assertThat(history.getFirst().id()).isEqualTo(ids.get(104));
		assertThat(history.getLast().id()).isEqualTo(ids.get(5));
		assertThat(repository.findById(ids.getFirst())).isEmpty();
	}

	@Test
	void shouldRemainConsistentUnderConcurrentWritesAndReads() throws Exception {
		InMemoryEtlExecutionRepository repository = new InMemoryEtlExecutionRepository();
		int taskCount = 250;
		ExecutorService executor = Executors.newFixedThreadPool(12);
		CountDownLatch start = new CountDownLatch(1);
		List<Callable<Void>> tasks = new ArrayList<>();

		for (int index = 0; index < taskCount; index++) {
			int currentIndex = index;
			tasks.add(() -> {
				start.await();
				repository.save(EtlExecution.pending(
						UUID.randomUUID(),
						EtlExecutionType.REMOVE_COLUMN,
						Instant.parse("2026-10-09T10:00:00Z").plusMillis(currentIndex)));
				repository.findAll();
				return null;
			});
		}

		try {
			List<Future<Void>> futures = tasks.stream()
					.map(executor::submit)
					.toList();
			start.countDown();
			for (Future<Void> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}
		}
		finally {
			executor.shutdownNow();
			assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(repository.findAll())
				.hasSize(InMemoryEtlExecutionRepository.MAX_HISTORY_SIZE)
				.extracting(EtlExecution::id)
				.doesNotHaveDuplicates();
	}

	@Test
	void shouldUpdateAnExecutionAtomicallyWithoutChangingItsHistoryPosition() {
		InMemoryEtlExecutionRepository repository = new InMemoryEtlExecutionRepository();
		EtlExecution first = EtlExecution.pending(
				UUID.randomUUID(),
				EtlExecutionType.REMOVE_COLUMN,
				Instant.parse("2026-10-09T10:00:00Z"));
		EtlExecution second = EtlExecution.pending(
				UUID.randomUUID(),
				EtlExecutionType.REMOVE_COLUMN,
				Instant.parse("2026-10-09T10:00:01Z"));
		repository.save(first);
		repository.save(second);

		repository.update(first.id(), EtlExecution::start);

		assertThat(repository.findAll())
				.extracting(EtlExecution::id)
				.containsExactly(second.id(), first.id());
		assertThat(repository.findById(first.id()).orElseThrow().status())
				.isEqualTo(EtlExecutionStatus.RUNNING);
	}
}
