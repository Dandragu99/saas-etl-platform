package com.dandragu.saasetl.business.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BusinessModels {

	private BusinessModels() {
	}

	public record Customer(
			UUID id, String name, String taxId, String email, String phone, String address,
			String notes, Instant createdAt, Instant updatedAt) {
	}

	public record CustomerInput(
			String name, String taxId, String email, String phone, String address, String notes) {
	}

	public record LineInput(
			String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate) {
	}

	public record DocumentLine(
			int position, String description, BigDecimal quantity, BigDecimal unitPrice,
			BigDecimal taxRate, BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
	}

	public record QuoteInput(
			UUID customerId, LocalDate issueDate, LocalDate validUntil, String notes,
			List<LineInput> lines) {
	}

	public record Quote(
			UUID id, UUID customerId, String customerName, String number, String status,
			LocalDate issueDate, LocalDate validUntil, String notes, BigDecimal subtotal,
			BigDecimal taxTotal, BigDecimal total, List<DocumentLine> lines) {
	}

	public record Invoice(
			UUID id, UUID customerId, String customerName, UUID quoteId, String number,
			String status, LocalDate issueDate, LocalDate dueDate, String notes,
			BigDecimal subtotal, BigDecimal taxTotal, BigDecimal total,
			List<DocumentLine> lines) {
	}

	public record Dashboard(
			long customers, long activeWorks, long quotes, long invoices,
			BigDecimal pendingRevenue) {
	}

	public record CalculatedDocument(
			BigDecimal subtotal, BigDecimal taxTotal, BigDecimal total,
			List<DocumentLine> lines) {
	}
}
