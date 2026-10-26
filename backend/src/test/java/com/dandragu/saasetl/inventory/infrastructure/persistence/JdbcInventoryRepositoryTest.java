package com.dandragu.saasetl.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dandragu.saasetl.inventory.domain.InventoryMovement;
import com.dandragu.saasetl.inventory.domain.InventoryMovementType;

@SpringBootTest
@Testcontainers
class JdbcInventoryRepositoryTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private JdbcInventoryRepository repository;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void cleanDatabase() {
		jdbc.update("DELETE FROM inventory_movements");
		jdbc.update("DELETE FROM inventory_stocks");
		jdbc.update("DELETE FROM products");
		jdbc.update("DELETE FROM company_memberships");
		jdbc.update("DELETE FROM users");
		jdbc.update("DELETE FROM companies");
	}

	@Test
	void shouldIsolateStockAndMovementsByCompany() {
		Fixture first = fixture("first@example.com", "FIRST");
		Fixture second = fixture("second@example.com", "SECOND");
		Instant now = Instant.parse("2026-10-25T10:00:00Z");

		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			assertThat(repository.lockActiveProduct(first.companyId(), first.productId())).isTrue();
			repository.addStock(first.companyId(), first.productId(), new BigDecimal("5.0000"), now);
			repository.saveMovement(new InventoryMovement(
					UUID.randomUUID(), first.companyId(), first.productId(), null, null,
					InventoryMovementType.ENTRY, new BigDecimal("5.0000"), first.userId(),
					"first@example.com", now));
		});

		assertThat(repository.findStock(first.companyId())).singleElement()
				.extracting(stock -> stock.quantity()).isEqualTo(new BigDecimal("5.0000"));
		assertThat(repository.findStock(second.companyId())).singleElement()
				.extracting(stock -> stock.quantity()).isEqualTo(new BigDecimal("0.0000"));
		assertThat(repository.findMovements(first.companyId())).hasSize(1);
		assertThat(repository.findMovements(second.companyId())).isEmpty();
		assertThat(repository.lockActiveProduct(second.companyId(), first.productId())).isFalse();
	}

	@Test
	void shouldSerializeConcurrentExitsAndNeverCreateNegativeStock() throws Exception {
		Fixture fixture = fixture("owner@example.com", "SKU-1");
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		transaction.executeWithoutResult(status -> {
			repository.lockActiveProduct(fixture.companyId(), fixture.productId());
			repository.addStock(
					fixture.companyId(), fixture.productId(), new BigDecimal("10.0000"), Instant.now());
		});

		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Future<Boolean> first = executor.submit(() -> removeSeven(transaction, fixture, start));
			Future<Boolean> second = executor.submit(() -> removeSeven(transaction, fixture, start));
			start.countDown();

			assertThat(java.util.List.of(first.get(), second.get()))
					.containsExactlyInAnyOrder(true, false);
		}

		assertThat(repository.findStock(fixture.companyId())).singleElement()
				.extracting(stock -> stock.quantity()).isEqualTo(new BigDecimal("3.0000"));
	}

	private boolean removeSeven(
			TransactionTemplate transaction, Fixture fixture, CountDownLatch start) throws Exception {
		start.await();
		return Boolean.TRUE.equals(transaction.execute(status -> {
			repository.lockActiveProduct(fixture.companyId(), fixture.productId());
			return repository.removeStock(
					fixture.companyId(), fixture.productId(), new BigDecimal("7.0000"), Instant.now());
		}));
	}

	private Fixture fixture(String email, String sku) {
		UUID companyId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-25T09:00:00Z");
		jdbc.update("""
				INSERT INTO companies (id, name, default_currency, status, created_at, updated_at)
				VALUES (?, ?, 'EUR', 'ACTIVE', ?, ?)
				""", companyId, sku + " Company", Timestamp.from(now), Timestamp.from(now));
		jdbc.update("""
				INSERT INTO users (id, email, normalized_email, password_hash, status, version, created_at, updated_at)
				VALUES (?, ?, ?, 'hash', 'ACTIVE', 0, ?, ?)
				""", userId, email, email, Timestamp.from(now), Timestamp.from(now));
		jdbc.update("""
				INSERT INTO products (id, company_id, sku, normalized_sku, name, category, price,
				 unit_of_measure, currency, active, version, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, 'Category', 1.0000, 'UNIT', 'EUR', TRUE, 0, ?, ?)
				""", productId, companyId, sku, sku, sku + " Product",
				Timestamp.from(now), Timestamp.from(now));
		return new Fixture(companyId, userId, productId);
	}

	private record Fixture(UUID companyId, UUID userId, UUID productId) {
	}
}
