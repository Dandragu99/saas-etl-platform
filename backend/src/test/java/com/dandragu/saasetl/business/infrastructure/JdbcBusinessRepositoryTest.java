package com.dandragu.saasetl.business.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.business.application.BusinessModels.CalculatedDocument;
import com.dandragu.saasetl.business.application.BusinessModels.CustomerInput;
import com.dandragu.saasetl.business.application.BusinessModels.DocumentLine;
import com.dandragu.saasetl.business.application.BusinessModels.QuoteInput;
import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class JdbcBusinessRepositoryTest {
	@Autowired
	private JdbcBusinessRepository repository;
	@Autowired
	private CompanyRepository companies;
	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldIsolateCustomersAndCompleteQuoteToInvoicePersistence() {
		UUID first = company("Electricidad Norte");
		UUID second = company("Electricidad Sur");
		var firstCustomer = repository.saveCustomer(first, UUID.randomUUID(),
				new CustomerInput("Cliente Norte", null, null, null, null, null));
		repository.saveCustomer(second, UUID.randomUUID(),
				new CustomerInput("Cliente Sur", null, null, null, null, null));

		assertThat(repository.customers(first)).extracting(customer -> customer.name())
				.containsExactly("Cliente Norte");

		var line = new DocumentLine(1, "Cuadro eléctrico", BigDecimal.ONE,
				new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("100.00"),
				new BigDecimal("21.00"), new BigDecimal("121.00"));
		LocalDate today = LocalDate.of(2026, 11, 20);
		var quote = repository.saveQuote(first, UUID.randomUUID(),
				repository.nextNumber(first, "PRES", 2026),
				new QuoteInput(firstCustomer.id(), today, today.plusDays(30), null,
						List.of()),
				new CalculatedDocument(new BigDecimal("100.00"), new BigDecimal("21.00"),
						new BigDecimal("121.00"), List.of(line)));
		var invoice = repository.convertQuote(first, quote,
				repository.nextNumber(first, "FAC", 2026), today, today.plusDays(30));

		assertThat(repository.quotes(first)).singleElement()
				.satisfies(saved -> assertThat(saved.status()).isEqualTo("INVOICED"));
		assertThat(repository.invoices(first)).singleElement()
				.satisfies(saved -> {
					assertThat(saved.quoteId()).isEqualTo(quote.id());
					assertThat(saved.total()).isEqualByComparingTo("121.00");
				});
		assertThat(repository.invoices(second)).isEmpty();
	}

	private UUID company(String name) {
		Company company = Company.create(UUID.randomUUID(), name, new CurrencyCode("EUR"), Instant.now());
		UUID id = companies.save(company).id();
		entityManager.flush();
		return id;
	}
}
