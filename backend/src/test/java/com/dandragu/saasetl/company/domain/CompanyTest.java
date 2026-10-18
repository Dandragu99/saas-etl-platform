package com.dandragu.saasetl.company.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CompanyTest {

	private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

	@Test
	void shouldCreateAnActiveCompanyWithNormalizedValues() {
		Company company = Company.create(
				UUID.randomUUID(),
				"  Acme Europe  ",
				new CurrencyCode(" eur "),
				NOW);

		assertThat(company.name()).isEqualTo("Acme Europe");
		assertThat(company.defaultCurrency().value()).isEqualTo("EUR");
		assertThat(company.status()).isEqualTo(CompanyStatus.ACTIVE);
		assertThat(company.createdAt()).isEqualTo(NOW);
		assertThat(company.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void shouldRejectBlankOrOversizedNames() {
		CurrencyCode currency = new CurrencyCode("EUR");

		assertThatThrownBy(() -> Company.create(UUID.randomUUID(), "   ", currency, NOW))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Company.create(
				UUID.randomUUID(),
				"a".repeat(Company.MAX_NAME_LENGTH + 1),
				currency,
				NOW))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectUnknownCurrencyCodesAndInvalidTimestampOrder() {
		assertThatThrownBy(() -> new CurrencyCode("ZZZ"))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> new Company(
				UUID.randomUUID(),
				"Acme",
				new CurrencyCode("EUR"),
				CompanyStatus.ACTIVE,
				NOW,
				NOW.minusSeconds(1)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
