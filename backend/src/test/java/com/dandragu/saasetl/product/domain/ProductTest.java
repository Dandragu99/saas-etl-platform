package com.dandragu.saasetl.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.company.domain.CurrencyCode;

class ProductTest {

	private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

	@Test
	void shouldNormalizeSkuTextAndPrice() {
		Product product = product(" ab-c 1 ", new BigDecimal("12.34"));

		assertThat(product.sku().value()).isEqualTo("ab-c 1");
		assertThat(product.sku().normalizedValue()).isEqualTo("AB-C 1");
		assertThat(product.price()).isEqualByComparingTo("12.3400");
		assertThat(product.description()).isNull();
		assertThat(product.active()).isTrue();
		assertThat(product.version()).isZero();
	}

	@Test
	void shouldRejectInvalidProductValues() {
		assertThatThrownBy(() -> product("sku", new BigDecimal("-0.01")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> product("sku", new BigDecimal("1.00001")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> product("sku", new BigDecimal("1000000000000000")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> product("   ", BigDecimal.ONE))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> product("sku", "   ", BigDecimal.ONE))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> product("sku", "N".repeat(Product.MAX_NAME_LENGTH + 1), BigDecimal.ONE))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldAcceptPriceBoundaryValues() {
		assertThat(product("sku", BigDecimal.ZERO).price()).isEqualByComparingTo("0.0000");
		assertThat(product("sku", new BigDecimal("999999999999999.9999")).price())
				.isEqualByComparingTo("999999999999999.9999");
	}

	@Test
	void shouldUpdateAndChangeStatusWithoutLosingIdentityOrCurrency() {
		Product original = product("SKU-1", BigDecimal.TEN);
		Instant later = NOW.plusSeconds(30);

		Product updated = original.update(
				new ProductSku("SKU-2"),
				"Updated",
				"Description",
				"New category",
				new BigDecimal("20.50"),
				UnitOfMeasure.KG,
				later);
		Product inactive = updated.changeActive(false, later.plusSeconds(1));

		assertThat(updated.id()).isEqualTo(original.id());
		assertThat(updated.companyId()).isEqualTo(original.companyId());
		assertThat(updated.currency()).isEqualTo(original.currency());
		assertThat(updated.createdAt()).isEqualTo(original.createdAt());
		assertThat(inactive.active()).isFalse();
	}

	private Product product(String sku, BigDecimal price) {
		return product(sku, "Product name", price);
	}

	private Product product(String sku, String name, BigDecimal price) {
		return Product.create(
				new ProductId(UUID.randomUUID()),
				UUID.randomUUID(),
				new ProductSku(sku),
				name,
				" ",
				"Category",
				price,
				new CurrencyCode("EUR"),
				UnitOfMeasure.UNIT,
				NOW);
	}
}
