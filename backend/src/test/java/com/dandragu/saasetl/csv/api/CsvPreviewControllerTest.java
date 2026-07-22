package com.dandragu.saasetl.csv.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.dandragu.saasetl.common.api.error.ApiExceptionHandler;
import com.dandragu.saasetl.csv.application.CsvPreviewCommand;
import com.dandragu.saasetl.csv.application.CsvPreviewError;
import com.dandragu.saasetl.csv.application.CsvPreviewException;
import com.dandragu.saasetl.csv.application.CsvPreviewResult;
import com.dandragu.saasetl.csv.application.CsvPreviewService;

class CsvPreviewControllerTest {

	private CsvPreviewService csvPreviewService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		csvPreviewService = mock(CsvPreviewService.class);
		CsvPreviewController controller = new CsvPreviewController(csvPreviewService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void shouldReturnCsvPreviewAndPreserveColumnOrder() throws Exception {
		when(csvPreviewService.preview(any())).thenReturn(new CsvPreviewResult(
				"clientes.csv",
				List.of("id", "nombre", "email"),
				List.of(List.of("1", "Ana", "ana@example.com")),
				1,
				false));

		MvcResult mvcResult = mockMvc.perform(multipart("/api/csv/preview")
						.file(csvFile("clientes.csv", "id,nombre,email\n1,Ana,ana@example.com")))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.fileName").value("clientes.csv"))
				.andExpect(jsonPath("$.columns[0]").value("id"))
				.andExpect(jsonPath("$.columns[1]").value("nombre"))
				.andExpect(jsonPath("$.columns[2]").value("email"))
				.andExpect(jsonPath("$.rows[0].id").value("1"))
				.andExpect(jsonPath("$.rows[0].nombre").value("Ana"))
				.andExpect(jsonPath("$.rows[0].email").value("ana@example.com"))
				.andExpect(jsonPath("$.previewRowCount").value(1))
				.andExpect(jsonPath("$.truncated").value(false))
				.andReturn();

		String response = mvcResult.getResponse().getContentAsString();
		String rowJson = response.substring(response.indexOf("\"rows\":[{"));
		assertThat(rowJson.indexOf("\"id\""))
				.isLessThan(rowJson.indexOf("\"nombre\""));
		assertThat(rowJson.indexOf("\"nombre\""))
				.isLessThan(rowJson.indexOf("\"email\""));
	}

	@Test
	void shouldConvertMultipartFileToApplicationCommand() throws Exception {
		byte[] content = "id\n1".getBytes(StandardCharsets.UTF_8);
		when(csvPreviewService.preview(any())).thenReturn(new CsvPreviewResult(
				"clientes.csv",
				List.of("id"),
				List.of(List.of("1")),
				1,
				false));

		mockMvc.perform(multipart("/api/csv/preview")
						.file(new MockMultipartFile("file", "clientes.csv", "text/csv", content)))
				.andExpect(status().isOk());

		ArgumentCaptor<CsvPreviewCommand> commandCaptor = ArgumentCaptor.forClass(CsvPreviewCommand.class);
		org.mockito.Mockito.verify(csvPreviewService).preview(commandCaptor.capture());
		CsvPreviewCommand command = commandCaptor.getValue();
		assertThat(command.originalFileName()).isEqualTo("clientes.csv");
		assertThat(command.size()).isEqualTo(content.length);
		assertThat(command.content()).isNotNull();
	}

	@Test
	void shouldReturnEmptyRowsForHeaderOnlyCsv() throws Exception {
		when(csvPreviewService.preview(any())).thenReturn(new CsvPreviewResult(
				"clientes.csv",
				List.of("id", "nombre"),
				List.of(),
				0,
				false));

		mockMvc.perform(multipart("/api/csv/preview")
						.file(csvFile("clientes.csv", "id,nombre")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rows").isEmpty())
				.andExpect(jsonPath("$.previewRowCount").value(0))
				.andExpect(jsonPath("$.truncated").value(false));
	}

	@Test
	void shouldReturnFileRequiredWhenMultipartPartIsMissing() throws Exception {
		mockMvc.perform(multipart("/api/csv/preview"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CSV_FILE_REQUIRED"))
				.andExpect(jsonPath("$.message").value("El archivo CSV es obligatorio."))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/api/csv/preview"))
				.andExpect(jsonPath("$.timestamp").isString());

		verifyNoInteractions(csvPreviewService);
	}

	@ParameterizedTest
	@MethodSource("unsupportedMediaTypes")
	void shouldReturnUnsupportedMediaTypeForNonMultipartRequest(MediaType mediaType) throws Exception {
		mockMvc.perform(post("/api/csv/preview")
						.contentType(mediaType)
						.content("{}"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
				.andExpect(jsonPath("$.message").value("La petición debe usar multipart/form-data."))
				.andExpect(jsonPath("$.status").value(415))
				.andExpect(jsonPath("$.path").value("/api/csv/preview"))
				.andExpect(jsonPath("$.timestamp").isString());

		verifyNoInteractions(csvPreviewService);
	}

	@ParameterizedTest
	@MethodSource("applicationErrors")
	void shouldMapApplicationError(
			CsvPreviewError error,
			String expectedCode,
			int expectedStatus,
			String expectedMessage) throws Exception {
		when(csvPreviewService.preview(any()))
				.thenThrow(new CsvPreviewException(error, "Technical application detail"));

		mockMvc.perform(multipart("/api/csv/preview")
						.file(csvFile("clientes.csv", "content")))
				.andExpect(status().is(expectedStatus))
				.andExpect(jsonPath("$.code").value(expectedCode))
				.andExpect(jsonPath("$.message").value(expectedMessage))
				.andExpect(jsonPath("$.status").value(expectedStatus))
				.andExpect(jsonPath("$.path").value("/api/csv/preview"))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Technical application detail"))));
	}

	@Test
	void shouldMapMultipartSizeError() throws Exception {
		when(csvPreviewService.preview(any()))
				.thenThrow(new MaxUploadSizeExceededException(5_242_880L));

		mockMvc.perform(multipart("/api/csv/preview")
						.file(csvFile("clientes.csv", "content")))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.code").value("CSV_FILE_TOO_LARGE"))
				.andExpect(jsonPath("$.message")
						.value("El archivo CSV supera el tamaño máximo permitido de 5 MiB."))
				.andExpect(jsonPath("$.status").value(413));
	}

	@Test
	void shouldHideUnexpectedExceptionDetails() throws Exception {
		when(csvPreviewService.preview(any()))
				.thenThrow(new IllegalStateException("Sensitive internal detail"));

		mockMvc.perform(multipart("/api/csv/preview")
						.file(csvFile("clientes.csv", "content")))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("Se ha producido un error interno."))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.path").value("/api/csv/preview"))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Sensitive internal detail"))));
	}

	private MockMultipartFile csvFile(String fileName, String content) {
		return new MockMultipartFile(
				"file",
				fileName,
				"text/csv",
				content.getBytes(StandardCharsets.UTF_8));
	}

	private static Stream<MediaType> unsupportedMediaTypes() {
		return Stream.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
	}

	private static Stream<Arguments> applicationErrors() {
		return Stream.of(
				Arguments.of(
						CsvPreviewError.FILE_REQUIRED,
						"CSV_FILE_REQUIRED",
						400,
						"El archivo CSV es obligatorio."),
				Arguments.of(
						CsvPreviewError.FILE_EMPTY,
						"CSV_FILE_EMPTY",
						400,
						"El archivo CSV está vacío."),
				Arguments.of(
						CsvPreviewError.INVALID_EXTENSION,
						"CSV_INVALID_EXTENSION",
						400,
						"El archivo debe tener extensión .csv."),
				Arguments.of(
						CsvPreviewError.FILE_TOO_LARGE,
						"CSV_FILE_TOO_LARGE",
						413,
						"El archivo CSV supera el tamaño máximo permitido de 5 MiB."),
				Arguments.of(
						CsvPreviewError.HEADER_MISSING,
						"CSV_HEADER_MISSING",
						422,
						"El archivo CSV no contiene un encabezado utilizable."),
				Arguments.of(
						CsvPreviewError.INVALID_HEADER,
						"CSV_INVALID_HEADER",
						422,
						"El encabezado CSV contiene columnas vacías o duplicadas."),
				Arguments.of(
						CsvPreviewError.MALFORMED,
						"CSV_MALFORMED",
						422,
						"El archivo CSV no tiene un formato válido."),
				Arguments.of(
						CsvPreviewError.READ_ERROR,
						"INTERNAL_ERROR",
						500,
						"Se ha producido un error interno."));
	}
}
