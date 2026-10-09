package com.dandragu.saasetl.execution.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;
import com.dandragu.saasetl.execution.infrastructure.InMemoryEtlExecutionRepository;

class EtlExecutionHistoryServiceTest {

	private final InMemoryEtlExecutionRepository repository = new InMemoryEtlExecutionRepository();
	private final EtlExecutionHistoryService service = new EtlExecutionHistoryService(repository);

	@Test
	void shouldRegisterSuccessfulExecution() {
		EtlExecution execution = service.createPending(EtlExecutionType.REMOVE_COLUMN);
		service.markRunning(execution.id());

		service.markSuccess(execution.id(), 42);
		EtlExecution success = repository.findById(execution.id()).orElseThrow();

		assertThat(success.status()).isEqualTo(EtlExecutionStatus.SUCCESS);
		assertThat(success.finishedAt()).isNotNull();
		assertThat(success.processedRecordCount()).isEqualTo(42);
		assertThat(success.errorMessage()).isNull();
	}

	@Test
	void shouldRegisterOnlySafeFailureMessage() {
		EtlExecution execution = service.createPending(EtlExecutionType.REMOVE_COLUMN);
		service.markRunning(execution.id());

		service.markFailed(execution.id());
		EtlExecution failed = repository.findById(execution.id()).orElseThrow();

		assertThat(failed.status()).isEqualTo(EtlExecutionStatus.FAILED);
		assertThat(failed.finishedAt()).isNotNull();
		assertThat(failed.processedRecordCount()).isNull();
		assertThat(failed.errorMessage()).isEqualTo(EtlExecutionHistoryService.SAFE_FAILURE_MESSAGE);
	}

	@Test
	void shouldNotBreakConcurrentExecutionsWhenTheHistoryLimitIsExceeded() throws Exception {
		int executionCount = InMemoryEtlExecutionRepository.MAX_HISTORY_SIZE + 50;
		CountDownLatch allExecutionsCreated = new CountDownLatch(executionCount);
		CountDownLatch allowTransitions = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<Void>> futures = IntStream.range(0, executionCount)
					.mapToObj(index -> executor.<Void>submit(() -> {
						EtlExecution execution = service.createPending(EtlExecutionType.REMOVE_COLUMN);
						allExecutionsCreated.countDown();
						allowTransitions.await();
						service.markRunning(execution.id());
						service.markSuccess(execution.id(), index);
						return null;
					}))
					.toList();

			assertThat(allExecutionsCreated.await(10, TimeUnit.SECONDS)).isTrue();
			allowTransitions.countDown();
			for (Future<Void> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}
		}

		assertThat(repository.findAll())
				.hasSize(InMemoryEtlExecutionRepository.MAX_HISTORY_SIZE)
				.allMatch(execution -> execution.status() == EtlExecutionStatus.SUCCESS);
	}
}
