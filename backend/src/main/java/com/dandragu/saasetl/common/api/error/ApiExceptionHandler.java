package com.dandragu.saasetl.common.api.error;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.dandragu.saasetl.csv.application.CsvPreviewError;
import com.dandragu.saasetl.csv.application.CsvPreviewException;
import com.dandragu.saasetl.csv.application.RemoveColumnError;
import com.dandragu.saasetl.csv.application.RemoveColumnException;
import com.dandragu.saasetl.execution.api.InvalidEtlExecutionQueryException;
import com.dandragu.saasetl.company.application.CompanyAccessDeniedException;
import com.dandragu.saasetl.company.application.CompanyContextUnavailableException;
import com.dandragu.saasetl.company.application.CompanyPermissionDeniedException;
import com.dandragu.saasetl.identity.api.InvalidRegistrationRequestException;
import com.dandragu.saasetl.identity.application.DuplicateUserEmailException;
import com.dandragu.saasetl.product.api.InvalidProductQueryException;
import com.dandragu.saasetl.product.api.InvalidProductRequestException;
import com.dandragu.saasetl.product.application.DuplicateProductSkuException;
import com.dandragu.saasetl.product.application.ProductCompanyUnavailableException;
import com.dandragu.saasetl.product.application.ProductNotFoundException;
import com.dandragu.saasetl.product.application.ProductVersionConflictException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidRegistrationRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidRegistration(
			Exception exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"INVALID_REGISTRATION",
						"Los datos de registro no son válidos.",
						HttpStatus.BAD_REQUEST),
				request);
	}

	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class,
			InvalidProductRequestException.class})
	public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
			Exception exception,
			HttpServletRequest request) {
		boolean productRequest = request.getRequestURI().startsWith("/api/products");
		return createResponse(
				new ErrorDetails(
						productRequest ? "INVALID_PRODUCT_REQUEST" : "INVALID_REGISTRATION",
						productRequest
								? "Los datos del producto no son válidos."
								: "Los datos de registro no son válidos.",
						HttpStatus.BAD_REQUEST),
				request);
	}

	@ExceptionHandler(InvalidProductQueryException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidProductQuery(
			InvalidProductQueryException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"INVALID_PRODUCT_QUERY",
						"Los filtros, la ordenación o la paginación de productos no son válidos.",
						HttpStatus.BAD_REQUEST),
				request);
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleProductNotFound(
			ProductNotFoundException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"PRODUCT_NOT_FOUND",
						"El producto no existe.",
						HttpStatus.NOT_FOUND),
				request);
	}

	@ExceptionHandler({DuplicateProductSkuException.class, ProductVersionConflictException.class})
	public ResponseEntity<ApiErrorResponse> handleProductConflict(
			RuntimeException exception,
			HttpServletRequest request) {
		String code = exception instanceof DuplicateProductSkuException
				? "PRODUCT_SKU_ALREADY_EXISTS"
				: "PRODUCT_VERSION_CONFLICT";
		String message = exception instanceof DuplicateProductSkuException
				? "Ya existe un producto con ese SKU en la empresa activa."
				: "El producto ha sido modificado por otra operación.";
		return createResponse(new ErrorDetails(code, message, HttpStatus.CONFLICT), request);
	}

	@ExceptionHandler(ProductCompanyUnavailableException.class)
	public ResponseEntity<ApiErrorResponse> handleProductCompanyUnavailable(
			ProductCompanyUnavailableException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"PRODUCT_COMPANY_UNAVAILABLE",
						"La empresa activa no está disponible para operar con productos.",
						HttpStatus.CONFLICT),
				request);
	}

	@ExceptionHandler(DuplicateUserEmailException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateUserEmail(
			DuplicateUserEmailException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"EMAIL_ALREADY_REGISTERED",
						"Ya existe una cuenta asociada a ese correo electrónico.",
						HttpStatus.CONFLICT),
				request);
	}

	@ExceptionHandler(CompanyContextUnavailableException.class)
	public ResponseEntity<ApiErrorResponse> handleCompanyContextUnavailable(
			CompanyContextUnavailableException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"ACTIVE_COMPANY_REQUIRED",
						"Debe seleccionar una empresa activa.",
						HttpStatus.CONFLICT),
				request);
	}

	@ExceptionHandler(CompanyAccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleCompanyAccessDenied(
			CompanyAccessDeniedException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"COMPANY_ACCESS_DENIED",
						"No tiene acceso a la empresa solicitada.",
						HttpStatus.FORBIDDEN),
				request);
	}

	@ExceptionHandler(CompanyPermissionDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleCompanyPermissionDenied(
			CompanyPermissionDeniedException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"COMPANY_PERMISSION_DENIED",
						"Su rol no permite realizar esta operación.",
						HttpStatus.FORBIDDEN),
				request);
	}

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

	@ExceptionHandler(InvalidEtlExecutionQueryException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidEtlExecutionQuery(
			InvalidEtlExecutionQueryException exception,
			HttpServletRequest request) {
		return createResponse(
				new ErrorDetails(
						"INVALID_EXECUTION_QUERY",
						"Los filtros o la paginación del historial no son válidos.",
						HttpStatus.BAD_REQUEST),
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
