package com.dandragu.saasetl.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;

@SpringBootTest
@Transactional
class JpaCompanyRepositoryTest {

	@Autowired
	private JpaCompanyRepository repository;

	@Test
	void shouldPersistAndRetrieveCompaniesWithoutConfusingTheirIdentity() {
		Instant now = Instant.parse("2026-10-18T10:00:00Z");
		Company first = Company.create(
				UUID.randomUUID(),
				"First Company",
				new CurrencyCode("EUR"),
				now);
		Company second = Company.create(
				UUID.randomUUID(),
				"Second Company",
				new CurrencyCode("USD"),
				now);

		repository.save(first);
		repository.save(second);

		assertThat(repository.findById(first.id())).contains(first);
		assertThat(repository.findById(second.id())).contains(second);
		assertThat(repository.findById(UUID.randomUUID())).isEmpty();
	}
}
