package com.dandragu.saasetl.business.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.business.application.BusinessModels.CalculatedDocument;
import com.dandragu.saasetl.business.application.BusinessModels.Customer;
import com.dandragu.saasetl.business.application.BusinessModels.CustomerInput;
import com.dandragu.saasetl.business.application.BusinessModels.Dashboard;
import com.dandragu.saasetl.business.application.BusinessModels.DocumentLine;
import com.dandragu.saasetl.business.application.BusinessModels.Invoice;
import com.dandragu.saasetl.business.application.BusinessModels.LineInput;
import com.dandragu.saasetl.business.application.BusinessModels.Quote;
import com.dandragu.saasetl.business.application.BusinessModels.QuoteInput;
import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.application.CompanyPermission;

@Service
public class BusinessService {
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private final BusinessRepository repository;
	private final CompanyAuthorizationService authorization;

	public BusinessService(BusinessRepository repository, CompanyAuthorizationService authorization) {
		this.repository = repository;
		this.authorization = authorization;
	}

	@Transactional(readOnly = true)
	public Dashboard dashboard() {
		return repository.dashboard(companyId(CompanyPermission.BUSINESS_READ));
	}

	@Transactional(readOnly = true)
	public List<Customer> customers() {
		return repository.customers(companyId(CompanyPermission.BUSINESS_READ));
	}

	@Transactional
	public Customer createCustomer(CustomerInput input) {
		UUID companyId = companyId(CompanyPermission.BUSINESS_WRITE);
		return repository.saveCustomer(companyId, UUID.randomUUID(), validateCustomer(input));
	}

	@Transactional
	public Customer updateCustomer(UUID id, CustomerInput input) {
		UUID companyId = companyId(CompanyPermission.BUSINESS_WRITE);
		repository.customer(companyId, id).orElseThrow(BusinessNotFoundException::new);
		return repository.saveCustomer(companyId, id, validateCustomer(input));
	}

	@Transactional(readOnly = true)
	public List<Quote> quotes() {
		return repository.quotes(companyId(CompanyPermission.BUSINESS_READ));
	}

	@Transactional
	public Quote createQuote(QuoteInput input) {
		UUID companyId = companyId(CompanyPermission.BUSINESS_WRITE);
		if (input == null || input.customerId() == null || input.issueDate() == null
				|| input.validUntil() == null || input.validUntil().isBefore(input.issueDate())) {
			throw new BusinessValidationException("Invalid quote dates");
		}
		repository.customer(companyId, input.customerId()).orElseThrow(BusinessNotFoundException::new);
		CalculatedDocument calculated = calculate(input.lines());
		String number = repository.nextNumber(companyId, "PRES", input.issueDate().getYear());
		return repository.saveQuote(companyId, UUID.randomUUID(), number, input, calculated);
	}

	@Transactional(readOnly = true)
	public List<Invoice> invoices() {
		return repository.invoices(companyId(CompanyPermission.BUSINESS_READ));
	}

	@Transactional
	public Invoice convertToInvoice(UUID quoteId) {
		UUID companyId = companyId(CompanyPermission.BUSINESS_WRITE);
		Quote quote = repository.lockQuote(companyId, quoteId).orElseThrow(BusinessNotFoundException::new);
		Optional<Invoice> existing = repository.invoiceByQuote(companyId, quoteId);
		if (existing.isPresent()) return existing.orElseThrow();
		LocalDate issueDate = LocalDate.now();
		String number = repository.nextNumber(companyId, "FAC", issueDate.getYear());
		return repository.convertQuote(companyId, quote, number, issueDate, issueDate.plusDays(30));
	}

	@Transactional
	public void seedDemo() {
		UUID companyId = companyId(CompanyPermission.BUSINESS_WRITE);
		if (!repository.hasBusinessData(companyId)) repository.seedDemo(companyId);
	}

	@Transactional(readOnly = true)
	public Quote quote(UUID id) {
		return repository.quote(companyId(CompanyPermission.BUSINESS_READ), id)
				.orElseThrow(BusinessNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public Invoice invoice(UUID id) {
		return repository.invoice(companyId(CompanyPermission.BUSINESS_READ), id)
				.orElseThrow(BusinessNotFoundException::new);
	}

	public static CalculatedDocument calculate(List<LineInput> inputs) {
		if (inputs == null || inputs.isEmpty() || inputs.size() > 100) {
			throw new BusinessValidationException("A document needs between 1 and 100 lines");
		}
		var lines = new java.util.ArrayList<DocumentLine>();
		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal taxTotal = BigDecimal.ZERO;
		for (int index = 0; index < inputs.size(); index++) {
			LineInput input = inputs.get(index);
			String description = required(input.description(), 500);
			BigDecimal quantity = decimal(input.quantity(), 3, true, "quantity");
			BigDecimal price = decimal(input.unitPrice(), 2, false, "unit price");
			BigDecimal rate = decimal(input.taxRate(), 2, false, "tax rate");
			if (rate.compareTo(ONE_HUNDRED) > 0) throw new BusinessValidationException("Invalid tax rate");
			BigDecimal lineSubtotal = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
			BigDecimal tax = lineSubtotal.multiply(rate).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
			BigDecimal total = lineSubtotal.add(tax);
			lines.add(new DocumentLine(index + 1, description, quantity, price, rate,
					lineSubtotal, tax, total));
			subtotal = subtotal.add(lineSubtotal);
			taxTotal = taxTotal.add(tax);
		}
		return new CalculatedDocument(subtotal, taxTotal, subtotal.add(taxTotal), List.copyOf(lines));
	}

	private CustomerInput validateCustomer(CustomerInput input) {
		if (input == null) throw new BusinessValidationException("Customer is required");
		return new CustomerInput(required(input.name(), 160), optional(input.taxId(), 30),
				optional(input.email(), 320), optional(input.phone(), 40),
				optional(input.address(), 500), optional(input.notes(), 2000));
	}

	private static BigDecimal decimal(BigDecimal value, int scale, boolean positive, String field) {
		if (value == null || value.scale() > scale || (positive ? value.signum() <= 0 : value.signum() < 0)
				|| value.precision() - value.scale() > 12) {
			throw new BusinessValidationException("Invalid " + field);
		}
		return value.setScale(scale, RoundingMode.UNNECESSARY);
	}

	private static String required(String value, int max) {
		String normalized = optional(value, max);
		if (normalized == null) throw new BusinessValidationException("Required text");
		return normalized;
	}

	private static String optional(String value, int max) {
		if (value == null || value.isBlank()) return null;
		String trimmed = value.trim();
		if (trimmed.length() > max) throw new BusinessValidationException("Text too long");
		return trimmed;
	}

	private UUID companyId(CompanyPermission permission) {
		return authorization.require(permission).company().id();
	}

}
