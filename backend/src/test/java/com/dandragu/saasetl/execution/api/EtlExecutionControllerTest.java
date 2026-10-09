package com.dandragu.saasetl.execution.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dandragu.saasetl.execution.application.EtlExecutionHistoryService;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;
import com.dandragu.saasetl.execution.infrastructure.InMemoryEtlExecutionRepository;

class EtlExecutionControllerTest {

	private static final String PATH = "/api/etl/executions";

	private EtlExecutionHistoryService historyService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		historyService = new EtlExecutionHistoryService(new InMemoryEtlExecutionRepository());
		mockMvc = MockMvcBuilders.standaloneSetup(new EtlExecutionController(historyService)).build();
	}

	@Test
	void shouldReturnExecutionHistoryWithoutCsvOrPersonalData() throws Exception {
		EtlExecution successful = historyService.createPending(EtlExecutionType.REMOVE_COLUMN);
		historyService.markRunning(successful.id());
		historyService.markSuccess(successful.id(), 21);

		EtlExecution failed = historyService.createPending(EtlExecutionType.REMOVE_COLUMN);
		historyService.markRunning(failed.id());
		historyService.markFailed(failed.id());

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
				.andExpect(jsonPath("$[0].fileName").doesNotExist())
				.andExpect(jsonPath("$[0].content").doesNotExist())
				.andExpect(jsonPath("$[1].status").value("SUCCESS"))
				.andExpect(jsonPath("$[1].processedRecordCount").value(21))
				.andExpect(jsonPath("$[1].errorMessage").doesNotExist());
	}

	@Test
	void shouldReturnEmptyArrayWhenNoExecutionsExist() throws Exception {
		mockMvc.perform(get(PATH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}
}
