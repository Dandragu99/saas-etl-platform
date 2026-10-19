package com.dandragu.saasetl.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.csv.application.RemoveColumnDownloadCommand;
import com.dandragu.saasetl.csv.application.RemoveColumnDownloadResult;
import com.dandragu.saasetl.csv.application.RemoveColumnDownloadService;
import com.dandragu.saasetl.csv.application.CsvPreviewCommand;
import com.dandragu.saasetl.company.application.CompanyAccess;
import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.identity.domain.MembershipRole;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;
import com.dandragu.saasetl.execution.infrastructure.InMemoryEtlExecutionRepository;

class RemoveColumnExecutionServiceTest {

	private final RemoveColumnDownloadService downloadService = mock(RemoveColumnDownloadService.class);
	private final InMemoryEtlExecutionRepository repository = new InMemoryEtlExecutionRepository();
	private final UUID companyId = UUID.randomUUID();
	private final RemoveColumnExecutionService service = new RemoveColumnExecutionService(
			downloadService,
			new EtlExecutionHistoryService(repository, authorization(companyId)));

	@Test
	void shouldRecordSuccessfulDownloadWithProcessedRecordCount() {
		RemoveColumnDownloadResult expected = new RemoveColumnDownloadResult(
				"clientes-sin-email.csv",
				"id\n1\n".getBytes(StandardCharsets.UTF_8),
				1);
		when(downloadService.download(any())).thenReturn(expected);

		RemoveColumnDownloadResult result = service.download(command("C:\\fakepath\\clientes.csv"));

		assertThat(result).isSameAs(expected);
		EtlExecution execution = repository.findAllByCompanyId(companyId).getFirst();
		assertThat(execution.status()).isEqualTo(EtlExecutionStatus.SUCCESS);
		assertThat(execution.processedRecordCount()).isEqualTo(1);
		assertThat(execution.errorMessage()).isNull();
		assertThat(execution.sourceFileName()).isEqualTo("clientes.csv");
		assertThat(execution.resultFileName()).isEqualTo("clientes-sin-email.csv");
	}

	@Test
	void shouldRecordFailedWithoutExposingExceptionDetails() {
		when(downloadService.download(any()))
				.thenThrow(new IllegalStateException("sensitive stack trace detail"));

		assertThatThrownBy(() -> service.download(command("clientes.csv")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("sensitive stack trace detail");

		EtlExecution execution = repository.findAllByCompanyId(companyId).getFirst();
		assertThat(execution.status()).isEqualTo(EtlExecutionStatus.FAILED);
		assertThat(execution.processedRecordCount()).isNull();
		assertThat(execution.errorMessage())
				.isEqualTo(EtlExecutionHistoryService.SAFE_FAILURE_MESSAGE)
				.doesNotContain("sensitive");
		assertThat(execution.resultFileName()).isNull();
	}

	private RemoveColumnDownloadCommand command(String fileName) {
		return new RemoveColumnDownloadCommand(
				new CsvPreviewCommand(fileName, 1, new ByteArrayInputStream(new byte[] { 1 })),
				"email");
	}

	private static CompanyAuthorizationService authorization(UUID companyId) {
		Company company = mock(Company.class);
		when(company.id()).thenReturn(companyId);
		CompanyAuthorizationService authorization = mock(CompanyAuthorizationService.class);
		when(authorization.require(any())).thenReturn(new CompanyAccess(company, MembershipRole.OWNER));
		return authorization;
	}
}
