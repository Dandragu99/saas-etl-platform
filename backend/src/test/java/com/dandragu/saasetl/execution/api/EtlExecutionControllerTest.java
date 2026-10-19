package com.dandragu.saasetl.execution.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;

import com.dandragu.saasetl.company.application.CompanyAccess;
import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.identity.domain.MembershipRole;
import com.dandragu.saasetl.execution.application.EtlExecutionHistoryService;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;
import com.dandragu.saasetl.execution.infrastructure.InMemoryEtlExecutionRepository;

class EtlExecutionControllerTest {

	private static final String PATH = "/api/etl/executions";
	private static final UUID COMPANY_ID = UUID.randomUUID();

	private EtlExecutionHistoryService historyService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		historyService = new EtlExecutionHistoryService(
				new InMemoryEtlExecutionRepository(), authorization());
		mockMvc = MockMvcBuilders.standaloneSetup(new EtlExecutionController(historyService)).build();
	}

	@Test
	void shouldReturnExecutionHistoryWithoutCsvOrPersonalData() throws Exception {
		EtlExecution successful = historyService.createPending(
				EtlExecutionType.REMOVE_COLUMN, "clientes.csv");
		historyService.markRunning(COMPANY_ID, successful.id());
		historyService.markSuccess(
				COMPANY_ID, successful.id(), 21, "clientes-sin-email.csv");

		EtlExecution failed = historyService.createPending(EtlExecutionType.REMOVE_COLUMN, "fallo.csv");
		historyService.markRunning(COMPANY_ID, failed.id());
		historyService.markFailed(COMPANY_ID, failed.id());

		mockMvc.perform(get(PATH))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(failed.id().toString()))
				.andExpect(jsonPath("$[0].type").value("REMOVE_COLUMN"))
				.andExpect(jsonPath("$[0].status").value("FAILED"))
				.andExpect(jsonPath("$[0].startedAt").isString())
				.andExpect(jsonPath("$[0].finishedAt").isString())
				.andExpect(jsonPath("$[0].processedRecordCount").doesNotExist())
				.andExpect(jsonPath("$[0].errorMessage")
						.value(EtlExecutionHistoryService.SAFE_FAILURE_MESSAGE))
				.andExpect(jsonPath("$[0].sourceFileName").value("fallo.csv"))
				.andExpect(jsonPath("$[0].resultFileName").doesNotExist())
				.andExpect(jsonPath("$[0].content").doesNotExist())
				.andExpect(jsonPath("$[1].status").value("SUCCESS"))
				.andExpect(jsonPath("$[1].processedRecordCount").value(21))
				.andExpect(jsonPath("$[1].resultFileName").value("clientes-sin-email.csv"))
				.andExpect(jsonPath("$[1].errorMessage").doesNotExist());
	}

	@Test
	void shouldReturnEmptyArrayWhenNoExecutionsExist() throws Exception {
		mockMvc.perform(get(PATH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void shouldFilterAndExposePaginationHeadersWithoutChangingTheResponseBody() throws Exception {
		EtlExecution execution = historyService.createPending(EtlExecutionType.REMOVE_COLUMN, null);
		historyService.markRunning(COMPANY_ID, execution.id());
		historyService.markSuccess(COMPANY_ID, execution.id(), 3, "result.csv");

		mockMvc.perform(get(PATH).param("status", "SUCCESS").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(execution.id().toString()))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("X-Total-Count", "1"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("X-Total-Pages", "1"));
	}

	@Test
	void shouldReturnExecutionSummary() throws Exception {
		EtlExecution execution = historyService.createPending(EtlExecutionType.REMOVE_COLUMN, null);
		historyService.markRunning(COMPANY_ID, execution.id());

		mockMvc.perform(get(PATH + "/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.successful").value(0))
				.andExpect(jsonPath("$.failed").value(0))
				.andExpect(jsonPath("$.inProgress").value(1));
	}

	private static CompanyAuthorizationService authorization() {
		Company company = mock(Company.class);
		when(company.id()).thenReturn(COMPANY_ID);
		CompanyAuthorizationService authorization = mock(CompanyAuthorizationService.class);
		when(authorization.require(any())).thenReturn(new CompanyAccess(company, MembershipRole.OWNER));
		return authorization;
	}
}
