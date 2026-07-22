package com.dandragu.saasetl.common.api.error;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.dandragu.saasetl.csv.application.CsvPreviewError;
import com.dandragu.saasetl.csv.application.CsvPreviewException;
import com.dandragu.saasetl.csv.application.RemoveColumnError;
import com.dandragu.saasetl.csv.application.RemoveColumnException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(CsvPreviewException.class)
	public ResponseEntity<ApiErrorResponse> handleCsvPreviewException(
			CsvPreviewException exception,
			HttpServletRequest request) {
		ErrorDetails details = mapCsvPreviewError(exception.getError());
		return createResponse(details, request);
	}

	@ExceptionHandler(RemoveColumnException.class)
	public ResponseEntity<ApiErrorResponse> handleRemoveColumnException(
			RemoveColumnException exception,
			HttpServletRequest request) {
		ErrorDetails details = mapRemoveColumnError(exception.getError());
		return createResponse(details, request);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingServletRequestPart(
			MissingServletRequestPartException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"CSV_FILE_REQUIRED",
						"El archivo CSV es obligatorio.",
						HttpStatus.BAD_REQUEST),
				request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(
			MaxUploadSizeExceededException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"CSV_FILE_TOO_LARGE",
						"El archivo CSV supera el tamaño máximo permitido de 5 MiB.",
						HttpStatus.PAYLOAD_TOO_LARGE),
				request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleHttpMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"UNSUPPORTED_MEDIA_TYPE",
						"La petición debe usar multipart/form-data.",
						HttpStatus.UNSUPPORTED_MEDIA_TYPE),
				request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"INTERNAL_ERROR",
						"Se ha producido un error interno.",
						HttpStatus.INTERNAL_SERVER_ERROR),
				request);
	}

	private ErrorDetails mapCsvPreviewError(CsvPreviewError error) {
		return switch (error) {
			case FILE_REQUIRED -> new ErrorDetails(
					"CSV_FILE_REQUIRED",
					"El archivo CSV es obligatorio.",
					HttpStatus.BAD_REQUEST);
			case FILE_EMPTY -> new ErrorDetails(
					"CSV_FILE_EMPTY",
					"El archivo CSV está vacío.",
					HttpStatus.BAD_REQUEST);
			case INVALID_EXTENSION -> new ErrorDetails(
					"CSV_INVALID_EXTENSION",
					"El archivo debe tener extensión .csv.",
					HttpStatus.BAD_REQUEST);
			case FILE_TOO_LARGE -> new ErrorDetails(
					"CSV_FILE_TOO_LARGE",
					"El archivo CSV supera el tamaño máximo permitido de 5 MiB.",
					HttpStatus.PAYLOAD_TOO_LARGE);
			case HEADER_MISSING -> new ErrorDetails(
					"CSV_HEADER_MISSING",
					"El archivo CSV no contiene un encabezado utilizable.",
					HttpStatus.UNPROCESSABLE_ENTITY);
			case INVALID_HEADER -> new ErrorDetails(
					"CSV_INVALID_HEADER",
					"El encabezado CSV contiene columnas vacías o duplicadas.",
					HttpStatus.UNPROCESSABLE_ENTITY);
			case MALFORMED -> new ErrorDetails(
					"CSV_MALFORMED",
					"El archivo CSV no tiene un formato válido.",
					HttpStatus.UNPROCESSABLE_ENTITY);
			case READ_ERROR -> new ErrorDetails(
					"INTERNAL_ERROR",
					"Se ha producido un error interno.",
					HttpStatus.INTERNAL_SERVER_ERROR);
		};
	}

	private ErrorDetails mapRemoveColumnError(RemoveColumnError error) {
		return switch (error) {
			case COLUMN_REQUIRED -> new ErrorDetails(
					"CSV_COLUMN_REQUIRED",
					"El nombre de la columna es obligatorio.",
					HttpStatus.BAD_REQUEST);
			case COLUMN_NOT_FOUND -> new ErrorDetails(
					"CSV_COLUMN_NOT_FOUND",
					"La columna indicada no existe en el archivo CSV.",
					HttpStatus.UNPROCESSABLE_ENTITY);
			case CANNOT_REMOVE_LAST_COLUMN -> new ErrorDetails(
					"CSV_CANNOT_REMOVE_LAST_COLUMN",
					"No se puede eliminar la única columna del archivo CSV.",
					HttpStatus.UNPROCESSABLE_ENTITY);
		};
	}

	private ResponseEntity<ApiErrorResponse> createResponse(
			ErrorDetails details,
			HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				details.code(),
				details.message(),
				details.status().value(),
				request.getRequestURI(),
				Instant.now());

		return ResponseEntity.status(details.status()).body(response);
	}

	private record ErrorDetails(String code, String message, HttpStatus status) {
	}
}
