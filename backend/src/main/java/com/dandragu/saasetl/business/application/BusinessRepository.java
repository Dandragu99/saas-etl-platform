package com.dandragu.saasetl.business.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dandragu.saasetl.business.application.BusinessModels.CalculatedDocument;
import com.dandragu.saasetl.business.application.BusinessModels.Customer;
import com.dandragu.saasetl.business.application.BusinessModels.CustomerInput;
import com.dandragu.saasetl.business.application.BusinessModels.Dashboard;
import com.dandragu.saasetl.business.application.BusinessModels.Invoice;
import com.dandragu.saasetl.business.application.BusinessModels.Quote;

public interface BusinessRepository {
	List<Customer> customers(UUID companyId);
	Optional<Customer> customer(UUID companyId, UUID id);
	Customer saveCustomer(UUID companyId, UUID id, CustomerInput input);
	List<Quote> quotes(UUID companyId);
	Optional<Quote> quote(UUID companyId, UUID id);
	Optional<Quote> lockQuote(UUID companyId, UUID id);
	Quote saveQuote(UUID companyId, UUID id, String number, BusinessModels.QuoteInput input,
			CalculatedDocument calculated);
	List<Invoice> invoices(UUID companyId);
	Optional<Invoice> invoice(UUID companyId, UUID id);
	Optional<Invoice> invoiceByQuote(UUID companyId, UUID quoteId);
	Invoice convertQuote(UUID companyId, Quote quote, String number, LocalDate issueDate,
			LocalDate dueDate);
	Dashboard dashboard(UUID companyId);
	String nextNumber(UUID companyId, String documentType, int year);
	boolean hasBusinessData(UUID companyId);
	void seedDemo(UUID companyId);
}
