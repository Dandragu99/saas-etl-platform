package com.dandragu.saasetl.csv.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.dandragu.saasetl.common.api.error.ApiExceptionHandler;
import com.dandragu.saasetl.csv.application.CsvPreviewCommand;
import com.dandragu.saasetl.csv.application.CsvPreviewError;
import com.dandragu.saasetl.csv.application.CsvPreviewException;
import com.dandragu.saasetl.csv.application.CsvFileValidator;
import com.dandragu.saasetl.csv.application.RemoveColumnCommand;
import com.dandragu.saasetl.csv.application.RemoveColumnDownloadResult;
import com.dandragu.saasetl.csv.application.RemoveColumnError;
import com.dandragu.saasetl.csv.application.RemoveColumnException;
import com.dandragu.saasetl.csv.application.RemoveColumnDownloadService;
import com.dandragu.saasetl.csv.application.RemoveColumnResult;
import com.dandragu.saasetl.csv.application.RemoveColumnService;
import com.dandragu.saasetl.csv.infrastructure.csv.CsvStreamReader;
import com.dandragu.saasetl.execution.application.EtlExecutionHistoryService;
import com.dandragu.saasetl.execution.application.RemoveColumnExecutionService;
import com.dandragu.saasetl.execution.infrastructure.InMemoryEtlExecutionRepository;

class CsvRemoveColumnControllerTest {

	private static final String PATH = "/api/csv/transform/remove-column";
	private static final String DOWNLOAD_PATH = PATH + "/download";

	private RemoveColumnService removeColumnService;
	private RemoveColumnExecutionService removeColumnExecutionService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		removeColumnService = org.mockito.Mockito.mock(RemoveColumnService.class);
		removeColumnExecutionService = org.mockito.Mockito.mock(RemoveColumnExecutionService.class);
		CsvRemoveColumnController controller = new CsvRemoveColumnController(
				removeColumnService,
				removeColumnExecutionService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void shouldReturnTransformedPreviewAndPreserveOrder() throws Exception {
		when(removeColumnService.remove(any())).thenReturn(successResult(false));

		MvcResult mvcResult = mockMvc.perform(multipart(PATH)
						.file(csvFile("clientes.csv", "id,nombre,email\n1,Ana,ana@example.com"))
						.param("column", "email"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.fileName").value("clientes.csv"))
				.andExpect(jsonPath("$.columns[0]").value("id"))
				.andExpect(jsonPath("$.columns[1]").value("nombre"))
				.andExpect(jsonPath("$.rows[0].id").value("1"))
				.andExpect(jsonPath("$.rows[0].nombre").value("Ana"))
				.andExpect(jsonPath("$.previewRowCount").value(1))
				.andExpect(jsonPath("$.truncated").value(false))
				.andExpect(jsonPath("$.transformation.type").value("REMOVE_COLUMN"))
				.andExpect(jsonPath("$.transformation.removedColumn").value("email"))
				.andReturn();

		String response = mvcResult.getResponse().getContentAsString();
		String rowJson = response.substring(response.indexOf("\"rows\":[{"));
		assertThat(rowJson.indexOf("\"id\"")).isLessThan(rowJson.indexOf("\"nombre\""));
	}

	@Test
	void shouldConvertMultipartInputToApplicationCommand() throws Exception {
		byte[] content = "id,email\n1,ana@example.com".getBytes(StandardCharsets.UTF_8);
		when(removeColumnService.remove(any())).thenReturn(successResult(false));

		mockMvc.perform(multipart(PATH)
						.file(new MockMultipartFile("file", "clientes.csv", "text/csv", content))
						.param("column", "email"))
				.andExpect(status().isOk());

		ArgumentCaptor<RemoveColumnCommand> captor = ArgumentCaptor.forClass(RemoveColumnCommand.class);
		org.mockito.Mockito.verify(removeColumnService).remove(captor.capture());
		RemoveColumnCommand command = captor.getValue();
		CsvPreviewCommand file = command.file();
		assertThat(file.originalFileName()).isEqualTo("clientes.csv");
		assertThat(file.size()).isEqualTo(content.length);
		assertThat(file.content()).isNotNull();
		assertThat(command.column()).isEqualTo("email");
	}

	@Test
	void shouldReturnHeaderOnlyTransformedPreview() throws Exception {
		when(removeColumnService.remove(any())).thenReturn(new RemoveColumnResult(
				"clientes.csv", List.of("id"), List.of(), 0, false, "email"));

		mockMvc.perform(multipart(PATH)
						.file(csvFile("clientes.csv", "id,email"))
						.param("column", "email"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rows").isEmpty())
				.andExpect(jsonPath("$.previewRowCount").value(0))
				.andExpect(jsonPath("$.truncated").value(false));
	}

	@Test
	void shouldReturnFileRequiredWhenFilePartIsMissing() throws Exception {
		mockMvc.perform(multipart(PATH).param("column", "email"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CSV_FILE_REQUIRED"))
				.andExpect(jsonPath("$.path").value(PATH));

		verifyNoInteractions(removeColumnService);
	}

	@Test
	void shouldPassMissingColumnToApplication() throws Exception {
		when(removeColumnService.remove(any())).thenThrow(new RemoveColumnException(
				RemoveColumnError.COLUMN_REQUIRED,
				"Technical detail"));

		mockMvc.perform(multipart(PATH).file(csvFile("clientes.csv", "id,email")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CSV_COLUMN_REQUIRED"))
				.andExpect(jsonPath("$.message").value("El nombre de la columna es obligatorio."))
				.andExpect(jsonPath("$.path").value(PATH));

		ArgumentCaptor<RemoveColumnCommand> captor = ArgumentCaptor.forClass(RemoveColumnCommand.class);
		org.mockito.Mockito.verify(removeColumnService).remove(captor.capture());
		assertThat(captor.getValue().column()).isNull();
	}

	@ParameterizedTest
	@MethodSource("removeColumnErrors")
	void shouldMapRemoveColumnErrors(
			RemoveColumnError error,
			String code,
			int statusCode,
			String message) throws Exception {
		when(removeColumnService.remove(any()))
				.thenThrow(new RemoveColumnException(error, "Sensitive technical detail"));

		mockMvc.perform(multipart(PATH)
						.file(csvFile("clientes.csv", "id,email"))
						.param("column", error == RemoveColumnError.COLUMN_REQUIRED ? " " : "missing"))
				.andExpect(status().is(statusCode))
				.andExpect(jsonPath("$.code").value(code))
				.andExpect(jsonPath("$.message").value(message))
				.andExpect(jsonPath("$.status").value(statusCode))
				.andExpect(jsonPath("$.path").value(PATH))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Sensitive technical detail"))));
	}

	@ParameterizedTest
	@MethodSource("fileErrors")
	void shouldPreserveExistingFileErrorMappings(
			CsvPreviewError error,
			String code,
			int statusCode) throws Exception {
		when(removeColumnService.remove(any()))
				.thenThrow(new CsvPreviewException(error, "Sensitive technical detail"));

		mockMvc.perform(multipart(PATH)
						.file(csvFile("clientes.csv", "content"))
						.param("column", "email"))
				.andExpect(status().is(statusCode))
				.andExpect(jsonPath("$.code").value(code))
				.andExpect(jsonPath("$.path").value(PATH));
	}

	@ParameterizedTest
	@MethodSource("unsupportedMediaTypes")
	void shouldRejectNonMultipartRequest(MediaType mediaType) throws Exception {
		mockMvc.perform(post(PATH).contentType(mediaType).content("{}"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
				.andExpect(jsonPath("$.path").value(PATH));
	}

	@Test
	void shouldMapMultipartSizeError() throws Exception {
		when(removeColumnService.remove(any()))
				.thenThrow(new MaxUploadSizeExceededException(5_242_880L));

		mockMvc.perform(multipart(PATH)
						.file(csvFile("clientes.csv", "content"))
						.param("column", "email"))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.code").value("CSV_FILE_TOO_LARGE"));
	}

	@Test
	void shouldHideUnexpectedErrorDetails() throws Exception {
		when(removeColumnService.remove(any()))
				.thenThrow(new IllegalStateException("Sensitive internal detail"));

		mockMvc.perform(multipart(PATH)
						.file(csvFile("clientes.csv", "content"))
						.param("column", "email"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("Se ha producido un error interno."))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Sensitive internal detail"))));
	}

	@Test
	void shouldReturnCompleteCsvDownloadWithRequiredHeaders() throws Exception {
		byte[] transformed = "id,nombre\n1,Ana\n".getBytes(StandardCharsets.UTF_8);
		when(removeColumnExecutionService.download(any())).thenReturn(
				new RemoveColumnDownloadResult("clientes-sin-email.csv", transformed, 1));

		MvcResult result = mockMvc.perform(multipart(DOWNLOAD_PATH)
						.file(csvFile("clientes.csv", "id,nombre,email\n1,Ana,ana@example.com"))
						.param("column", "email"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/csv;charset=UTF-8"))
				.andExpect(content().bytes(transformed))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.longValue(HttpHeaders.CONTENT_LENGTH, transformed.length))
				.andReturn();

		String header = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
		ContentDisposition disposition = ContentDisposition.parse(header);
		assertThat(disposition.getType()).isEqualTo("attachment");
		assertThat(disposition.getFilename()).isEqualTo("clientes-sin-email.csv");
	}

	@Test
	void shouldReturnJsonErrorWithoutPartialCsvForMalformedRowAfterTwenty() throws Exception {
		RemoveColumnDownloadService actualDownloadService = new RemoveColumnDownloadService(
				new CsvFileValidator(),
				new CsvStreamReader());
		RemoveColumnExecutionService actualExecutionService = new RemoveColumnExecutionService(
				actualDownloadService,
				new EtlExecutionHistoryService(new InMemoryEtlExecutionRepository()));
		MockMvc integrationMockMvc = MockMvcBuilders.standaloneSetup(
					new CsvRemoveColumnController(removeColumnService, actualExecutionService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
		StringBuilder csv = new StringBuilder("id,email\n");
		for (int index = 1; index <= 20; index++) {
			csv.append(index).append(",email@example.com\n");
		}
		csv.append("21");

		integrationMockMvc.perform(multipart(DOWNLOAD_PATH)
						.file(csvFile("clientes.csv", csv.toString()))
						.param("column", "email"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("CSV_MALFORMED"))
				.andExpect(jsonPath("$.path").value(DOWNLOAD_PATH))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("id,email"))));
	}

	private RemoveColumnResult successResult(boolean truncated) {
		return new RemoveColumnResult(
				"clientes.csv",
				List.of("id", "nombre"),
				List.of(List.of("1", "Ana")),
				1,
				truncated,
				"email");
	}

	private MockMultipartFile csvFile(String fileName, String content) {
		return new MockMultipartFile(
				"file", fileName, "text/csv", content.getBytes(StandardCharsets.UTF_8));
	}

	private static Stream<Arguments> removeColumnErrors() {
		return Stream.of(
				Arguments.of(
						RemoveColumnError.COLUMN_REQUIRED,
						"CSV_COLUMN_REQUIRED",
						400,
						"El nombre de la columna es obligatorio."),
				Arguments.of(
						RemoveColumnError.COLUMN_NOT_FOUND,
						"CSV_COLUMN_NOT_FOUND",
						422,
						"La columna indicada no existe en el archivo CSV."),
				Arguments.of(
						RemoveColumnError.CANNOT_REMOVE_LAST_COLUMN,
						"CSV_CANNOT_REMOVE_LAST_COLUMN",
						422,
						"No se puede eliminar la única columna del archivo CSV."));
	}

	private static Stream<Arguments> fileErrors() {
		return Stream.of(
				Arguments.of(CsvPreviewError.FILE_REQUIRED, "CSV_FILE_REQUIRED", 400),
				Arguments.of(CsvPreviewError.FILE_EMPTY, "CSV_FILE_EMPTY", 400),
				Arguments.of(CsvPreviewError.INVALID_EXTENSION, "CSV_INVALID_EXTENSION", 400),
				Arguments.of(CsvPreviewError.FILE_TOO_LARGE, "CSV_FILE_TOO_LARGE", 413),
				Arguments.of(CsvPreviewError.HEADER_MISSING, "CSV_HEADER_MISSING", 422),
				Arguments.of(CsvPreviewError.INVALID_HEADER, "CSV_INVALID_HEADER", 422),
				Arguments.of(CsvPreviewError.MALFORMED, "CSV_MALFORMED", 422),
				Arguments.of(CsvPreviewError.READ_ERROR, "INTERNAL_ERROR", 500));
	}

	private static Stream<MediaType> unsupportedMediaTypes() {
		return Stream.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
	}
}
