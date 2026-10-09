package com.dandragu.saasetl.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.csv.application.RemoveColumnDownloadCommand;
import com.dandragu.saasetl.csv.application.RemoveColumnDownloadResult;
import com.dandragu.saasetl.csv.application.RemoveColumnDownloadService;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;
import com.dandragu.saasetl.execution.infrastructure.InMemoryEtlExecutionRepository;

class RemoveColumnExecutionServiceTest {

	private final RemoveColumnDownloadService downloadService = mock(RemoveColumnDownloadService.class);
	private final InMemoryEtlExecutionRepository repository = new InMemoryEtlExecutionRepository();
	private final RemoveColumnExecutionService service = new RemoveColumnExecutionService(
			downloadService,
			new EtlExecutionHistoryService(repository));

	@Test
	void shouldRecordSuccessfulDownloadWithProcessedRecordCount() {
		RemoveColumnDownloadResult expected = new RemoveColumnDownloadResult(
				"clientes-sin-email.csv",
				"id\n1\n".getBytes(StandardCharsets.UTF_8),
				1);
		when(downloadService.download(any())).thenReturn(expected);

		RemoveColumnDownloadResult result = service.download(mock(RemoveColumnDownloadCommand.class));

		assertThat(result).isSameAs(expected);
		EtlExecution execution = repository.findAll().getFirst();
		assertThat(execution.status()).isEqualTo(EtlExecutionStatus.SUCCESS);
		assertThat(execution.processedRecordCount()).isEqualTo(1);
		assertThat(execution.errorMessage()).isNull();
	}

	@Test
	void shouldRecordFailedWithoutExposingExceptionDetails() {
		when(downloadService.download(any()))
				.thenThrow(new IllegalStateException("sensitive stack trace detail"));

		assertThatThrownBy(() -> service.download(mock(RemoveColumnDownloadCommand.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("sensitive stack trace detail");

		EtlExecution execution = repository.findAll().getFirst();
		assertThat(execution.status()).isEqualTo(EtlExecutionStatus.FAILED);
		assertThat(execution.processedRecordCount()).isNull();
		assertThat(execution.errorMessage())
				.isEqualTo(EtlExecutionHistoryService.SAFE_FAILURE_MESSAGE)
				.doesNotContain("sensitive");
	}
}
