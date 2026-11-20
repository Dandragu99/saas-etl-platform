package com.dandragu.saasetl.business.infrastructure;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.dandragu.saasetl.business.application.BusinessModels;
import com.dandragu.saasetl.business.application.BusinessModels.CalculatedDocument;
import com.dandragu.saasetl.business.application.BusinessModels.Customer;
import com.dandragu.saasetl.business.application.BusinessModels.CustomerInput;
import com.dandragu.saasetl.business.application.BusinessModels.Dashboard;
import com.dandragu.saasetl.business.application.BusinessModels.DocumentLine;
import com.dandragu.saasetl.business.application.BusinessModels.Invoice;
import com.dandragu.saasetl.business.application.BusinessModels.Quote;
import com.dandragu.saasetl.business.application.BusinessModels.QuoteInput;
import com.dandragu.saasetl.business.application.BusinessRepository;

@Repository
public class JdbcBusinessRepository implements BusinessRepository {
	private final JdbcTemplate jdbc;

	public JdbcBusinessRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<Customer> customers(UUID companyId) {
		return jdbc.query("""
				SELECT id, name, tax_id, email, phone, address, notes, created_at, updated_at
				FROM customers WHERE company_id = ? ORDER BY LOWER(name), id
				""", this::customer, companyId);
	}

	@Override
	public Optional<Customer> customer(UUID companyId, UUID id) {
		return jdbc.query("""
				SELECT id, name, tax_id, email, phone, address, notes, created_at, updated_at
				FROM customers WHERE company_id = ? AND id = ?
				""", this::customer, companyId, id).stream().findFirst();
	}

	@Override
	public Customer saveCustomer(UUID companyId, UUID id, CustomerInput input) {
		Instant now = Instant.now();
		int updated = jdbc.update("""
				UPDATE customers SET name=?, tax_id=?, email=?, phone=?, address=?, notes=?, updated_at=?
				WHERE company_id=? AND id=?
				""", input.name(), input.taxId(), input.email(), input.phone(), input.address(),
				input.notes(), Timestamp.from(now), companyId, id);
		if (updated == 0) {
			jdbc.update("""
					INSERT INTO customers
					(id, company_id, name, tax_id, email, phone, address, notes, created_at, updated_at)
					VALUES (?,?,?,?,?,?,?,?,?,?)
					""", id, companyId, input.name(), input.taxId(), input.email(), input.phone(),
					input.address(), input.notes(), Timestamp.from(now), Timestamp.from(now));
		}
		return customer(companyId, id).orElseThrow();
	}

	@Override
	public List<Quote> quotes(UUID companyId) {
		return jdbc.query("""
				SELECT q.*, c.name AS customer_name FROM quotes q
				JOIN customers c ON c.id=q.customer_id AND c.company_id=q.company_id
				WHERE q.company_id=? ORDER BY q.issue_date DESC, q.quote_number DESC
				""", (rs, row) -> quote(rs), companyId);
	}

	@Override
	public Optional<Quote> quote(UUID companyId, UUID id) {
		return jdbc.query("""
				SELECT q.*, c.name AS customer_name FROM quotes q
				JOIN customers c ON c.id=q.customer_id AND c.company_id=q.company_id
				WHERE q.company_id=? AND q.id=?
				""", (rs, row) -> quote(rs), companyId, id).stream().findFirst();
	}

	@Override
	public Optional<Quote> lockQuote(UUID companyId, UUID id) {
		return jdbc.query("""
				SELECT q.*, c.name AS customer_name FROM quotes q
				JOIN customers c ON c.id=q.customer_id AND c.company_id=q.company_id
				WHERE q.company_id=? AND q.id=? FOR UPDATE
				""", (rs, row) -> quote(rs), companyId, id).stream().findFirst();
	}

	@Override
	public Quote saveQuote(UUID companyId, UUID id, String number, QuoteInput input,
			CalculatedDocument calculated) {
		Instant now = Instant.now();
		jdbc.update("""
				INSERT INTO quotes
				(id, company_id, customer_id, quote_number, status, issue_date, valid_until, notes,
				 subtotal, tax_total, total, created_at, updated_at)
				VALUES (?,?,?,?,'DRAFT',?,?,?,?,?,?,?,?)
				""", id, companyId, input.customerId(), number, input.issueDate(), input.validUntil(),
				input.notes(), calculated.subtotal(), calculated.taxTotal(), calculated.total(),
				Timestamp.from(now), Timestamp.from(now));
		insertLines("quote_lines", "quote_id", id, calculated.lines());
		return quote(companyId, id).orElseThrow();
	}

	@Override
	public List<Invoice> invoices(UUID companyId) {
		return jdbc.query("""
				SELECT i.*, c.name AS customer_name FROM invoices i
				JOIN customers c ON c.id=i.customer_id AND c.company_id=i.company_id
				WHERE i.company_id=? ORDER BY i.issue_date DESC, i.invoice_number DESC
				""", (rs, row) -> invoice(rs), companyId);
	}

	@Override
	public Optional<Invoice> invoice(UUID companyId, UUID id) {
		return jdbc.query("""
				SELECT i.*, c.name AS customer_name FROM invoices i
				JOIN customers c ON c.id=i.customer_id AND c.company_id=i.company_id
				WHERE i.company_id=? AND i.id=?
				""", (rs, row) -> invoice(rs), companyId, id).stream().findFirst();
	}

	@Override
	public Optional<Invoice> invoiceByQuote(UUID companyId, UUID quoteId) {
		return jdbc.query("""
				SELECT i.*, c.name AS customer_name FROM invoices i
				JOIN customers c ON c.id=i.customer_id AND c.company_id=i.company_id
				WHERE i.company_id=? AND i.quote_id=?
				""", (rs, row) -> invoice(rs), companyId, quoteId).stream().findFirst();
	}

	@Override
	public Invoice convertQuote(UUID companyId, Quote quote, String number, LocalDate issueDate,
			LocalDate dueDate) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				INSERT INTO invoices
				(id, company_id, customer_id, quote_id, invoice_number, status, issue_date, due_date,
				 notes, subtotal, tax_total, total, created_at)
				VALUES (?,?,?,?,?,'ISSUED',?,?,?,?,?,?,?)
				""", id, companyId, quote.customerId(), quote.id(), number, issueDate, dueDate,
				quote.notes(), quote.subtotal(), quote.taxTotal(), quote.total(), Timestamp.from(Instant.now()));
		insertLines("invoice_lines", "invoice_id", id, quote.lines());
		jdbc.update("UPDATE quotes SET status='INVOICED', updated_at=? WHERE company_id=? AND id=?",
				Timestamp.from(Instant.now()), companyId, quote.id());
		return invoice(companyId, id).orElseThrow();
	}

	@Override
	public Dashboard dashboard(UUID companyId) {
		return new Dashboard(count("customers", companyId),
				jdbc.queryForObject("SELECT COUNT(*) FROM works WHERE company_id=? AND status<>'COMPLETED'",
						Long.class, companyId),
				count("quotes", companyId), count("invoices", companyId),
				jdbc.queryForObject("""
						SELECT COALESCE(SUM(total),0) FROM invoices
						WHERE company_id=? AND status='ISSUED'
						""", BigDecimal.class, companyId));
	}

	@Override
	public String nextNumber(UUID companyId, String documentType, int year) {
		jdbc.queryForObject("SELECT id FROM companies WHERE id=? FOR UPDATE", UUID.class, companyId);
		String table = "FAC".equals(documentType) ? "invoices" : "quotes";
		String column = "FAC".equals(documentType) ? "invoice_number" : "quote_number";
		Long sequence = jdbc.queryForObject(
				"SELECT COUNT(*) + 1 FROM " + table + " WHERE company_id=? AND " + column + " LIKE ?",
				Long.class, companyId, documentType + "-" + year + "-%");
		return "%s-%d-%04d".formatted(documentType, year, sequence);
	}

	@Override
	public boolean hasBusinessData(UUID companyId) {
		return count("customers", companyId) > 0;
	}

	@Override
	public void seedDemo(UUID companyId) {
		Instant now = Instant.now();
		UUID customer = UUID.randomUUID();
		jdbc.update("""
				INSERT INTO customers
				(id,company_id,name,tax_id,email,phone,address,notes,created_at,updated_at)
				VALUES (?,?,?,?,?,?,?,?,?,?)
				""", customer, companyId, "Reformas Martínez", "B12345678",
				"obra@reformasmartinez.es", "600 123 456", "Calle Mayor 24, Madrid",
				"Cliente demo para presentación comercial", Timestamp.from(now), Timestamp.from(now));
		jdbc.update("""
				INSERT INTO works(id,company_id,customer_id,title,description,status,start_date,created_at)
				VALUES (?,?,?,?,?,'IN_PROGRESS',?,?)
				""", UUID.randomUUID(), companyId, customer, "Instalación eléctrica vivienda",
				"Renovación de cuadro, mecanismos e iluminación", LocalDate.now().minusDays(4),
				Timestamp.from(now));
	}

	private long count(String table, UUID companyId) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE company_id=?",
				Long.class, companyId);
	}

	private Customer customer(ResultSet rs, int row) throws SQLException {
		return new Customer(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("tax_id"),
				rs.getString("email"), rs.getString("phone"), rs.getString("address"), rs.getString("notes"),
				rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
	}

	private Quote quote(ResultSet rs) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		return new Quote(id, rs.getObject("customer_id", UUID.class), rs.getString("customer_name"),
				rs.getString("quote_number"), rs.getString("status"), rs.getObject("issue_date", LocalDate.class),
				rs.getObject("valid_until", LocalDate.class), rs.getString("notes"), rs.getBigDecimal("subtotal"),
				rs.getBigDecimal("tax_total"), rs.getBigDecimal("total"), lines("quote_lines", "quote_id", id));
	}

	private Invoice invoice(ResultSet rs) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		return new Invoice(id, rs.getObject("customer_id", UUID.class), rs.getString("customer_name"),
				rs.getObject("quote_id", UUID.class), rs.getString("invoice_number"), rs.getString("status"),
				rs.getObject("issue_date", LocalDate.class), rs.getObject("due_date", LocalDate.class),
				rs.getString("notes"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("tax_total"),
				rs.getBigDecimal("total"), lines("invoice_lines", "invoice_id", id));
	}

	private List<DocumentLine> lines(String table, String parentColumn, UUID parentId) {
		return jdbc.query("SELECT * FROM " + table + " WHERE " + parentColumn + "=? ORDER BY position",
				(rs, row) -> new DocumentLine(rs.getInt("position"), rs.getString("description"),
						rs.getBigDecimal("quantity"), rs.getBigDecimal("unit_price"),
						rs.getBigDecimal("tax_rate"), rs.getBigDecimal("line_subtotal"),
						rs.getBigDecimal("line_tax"), rs.getBigDecimal("line_total")), parentId);
	}

	private void insertLines(String table, String parentColumn, UUID parentId, List<DocumentLine> lines) {
		for (DocumentLine line : lines) {
			jdbc.update("INSERT INTO " + table + " (id," + parentColumn
					+ ",position,description,quantity,unit_price,tax_rate,line_subtotal,line_tax,line_total)"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), parentId, line.position(),
					line.description(), line.quantity(), line.unitPrice(), line.taxRate(), line.subtotal(),
					line.tax(), line.total());
		}
	}
}
