package com.dandragu.saasetl.business.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.business.application.BusinessModels.CustomerInput;
import com.dandragu.saasetl.business.application.BusinessModels.LineInput;
import com.dandragu.saasetl.business.application.BusinessModels.QuoteInput;
import com.dandragu.saasetl.business.application.BusinessService;
import com.dandragu.saasetl.business.infrastructure.BusinessPdfService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping(path = "/api/business", produces = MediaType.APPLICATION_JSON_VALUE)
public class BusinessController {
	private final BusinessService service;
	private final BusinessPdfService pdf;

	public BusinessController(BusinessService service, BusinessPdfService pdf) {
		this.service = service;
		this.pdf = pdf;
	}

	@GetMapping("/dashboard")
	public Object dashboard() { return service.dashboard(); }

	@GetMapping("/customers")
	public Object customers() { return service.customers(); }

	@PostMapping(path = "/customers", consumes = MediaType.APPLICATION_JSON_VALUE)
	public Object createCustomer(@Valid @RequestBody CustomerRequest request) {
		return service.createCustomer(request.toInput());
	}

	@PutMapping(path = "/customers/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public Object updateCustomer(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
		return service.updateCustomer(id, request.toInput());
	}

	@GetMapping("/quotes")
	public Object quotes() { return service.quotes(); }

	@PostMapping(path = "/quotes", consumes = MediaType.APPLICATION_JSON_VALUE)
	public Object createQuote(@Valid @RequestBody QuoteRequest request) {
		return service.createQuote(request.toInput());
	}

	@PostMapping("/quotes/{id}/invoice")
	public Object invoiceFromQuote(@PathVariable UUID id) { return service.convertToInvoice(id); }

	@GetMapping("/invoices")
	public Object invoices() { return service.invoices(); }

	@PostMapping("/demo")
	public ResponseEntity<Void> seedDemo() {
		service.seedDemo();
		return ResponseEntity.noContent().build();
	}

	@GetMapping(path = "/quotes/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> quotePdf(@PathVariable UUID id) {
		var quote = service.quote(id);
		return pdf(pdf.quote(quote), quote.number() + ".pdf");
	}

	@GetMapping(path = "/invoices/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> invoicePdf(@PathVariable UUID id) {
		var invoice = service.invoice(id);
		return pdf(pdf.invoice(invoice), invoice.number() + ".pdf");
	}

	private ResponseEntity<byte[]> pdf(byte[] body, String fileName) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(fileName).build().toString())
				.contentType(MediaType.APPLICATION_PDF)
				.body(body);
	}

	public record CustomerRequest(
			@NotBlank @Size(max = 160) String name,
			@Size(max = 30) String taxId,
			@Email @Size(max = 320) String email,
			@Size(max = 40) String phone,
			@Size(max = 500) String address,
			@Size(max = 2000) String notes) {
		CustomerInput toInput() { return new CustomerInput(name, taxId, email, phone, address, notes); }
	}

	public record QuoteRequest(
			@NotNull UUID customerId,
			@NotNull LocalDate issueDate,
			@NotNull LocalDate validUntil,
			@Size(max = 2000) String notes,
			@NotEmpty @Size(max = 100) List<@Valid LineRequest> lines) {
		QuoteInput toInput() {
			return new QuoteInput(customerId, issueDate, validUntil, notes,
					lines.stream().map(LineRequest::toInput).toList());
		}
	}

	public record LineRequest(
			@NotBlank @Size(max = 500) String description,
			@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
			BigDecimal quantity,
			@NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
			@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
			BigDecimal taxRate) {
		LineInput toInput() { return new LineInput(description, quantity, unitPrice, taxRate); }
	}
}
