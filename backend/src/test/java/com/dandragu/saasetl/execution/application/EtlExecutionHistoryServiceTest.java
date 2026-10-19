package com.dandragu.saasetl.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
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
import com.dandragu.saasetl.company.application.CompanyAccess;
import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.identity.domain.MembershipRole;

class EtlExecutionHistoryServiceTest {

	private final InMemoryEtlExecutionRepository repository = new InMemoryEtlExecutionRepository();
	private final UUID companyId = UUID.randomUUID();
	private final EtlExecutionHistoryService service = new EtlExecutionHistoryService(
			repository, authorization(companyId));

	@Test
	void shouldRegisterSuccessfulExecution() {
		EtlExecution execution = service.createPending(EtlExecutionType.REMOVE_COLUMN, "clientes.csv");
		service.markRunning(companyId, execution.id());

		service.markSuccess(companyId, execution.id(), 42, "clientes-sin-email.csv");
		EtlExecution success = repository.findByCompanyIdAndId(companyId, execution.id()).orElseThrow();

		assertThat(success.status()).isEqualTo(EtlExecutionStatus.SUCCESS);
		assertThat(success.finishedAt()).isNotNull();
		assertThat(success.processedRecordCount()).isEqualTo(42);
		assertThat(success.errorMessage()).isNull();
	}

	@Test
	void shouldRegisterOnlySafeFailureMessage() {
		EtlExecution execution = service.createPending(EtlExecutionType.REMOVE_COLUMN, "clientes.csv");
		service.markRunning(companyId, execution.id());

		service.markFailed(companyId, execution.id());
		EtlExecution failed = repository.findByCompanyIdAndId(companyId, execution.id()).orElseThrow();

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
						EtlExecution execution = service.createPending(EtlExecutionType.REMOVE_COLUMN, null);
						allExecutionsCreated.countDown();
						allowTransitions.await();
						service.markRunning(companyId, execution.id());
						service.markSuccess(companyId, execution.id(), index, "result.csv");
						return null;
					}))
					.toList();

			assertThat(allExecutionsCreated.await(10, TimeUnit.SECONDS)).isTrue();
			allowTransitions.countDown();
			for (Future<Void> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}
		}

		assertThat(repository.findAllByCompanyId(companyId))
				.hasSize(InMemoryEtlExecutionRepository.MAX_HISTORY_SIZE)
				.allMatch(execution -> execution.status() == EtlExecutionStatus.SUCCESS);
	}

	private static CompanyAuthorizationService authorization(UUID companyId) {
		Company company = mock(Company.class);
		when(company.id()).thenReturn(companyId);
		CompanyAuthorizationService authorization = mock(CompanyAuthorizationService.class);
		when(authorization.require(any())).thenReturn(new CompanyAccess(company, MembershipRole.OWNER));
		return authorization;
	}
}
