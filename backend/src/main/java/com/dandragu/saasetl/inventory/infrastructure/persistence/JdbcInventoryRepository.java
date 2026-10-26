package com.dandragu.saasetl.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.dandragu.saasetl.inventory.application.InventoryRepository;
import com.dandragu.saasetl.inventory.domain.InventoryMovement;
import com.dandragu.saasetl.inventory.domain.InventoryMovementType;
import com.dandragu.saasetl.inventory.domain.StockLevel;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

@Repository
public class JdbcInventoryRepository implements InventoryRepository {

	private final JdbcTemplate jdbc;

	public JdbcInventoryRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public boolean lockActiveProduct(UUID companyId, UUID productId) {
		return !jdbc.queryForList("""
				SELECT id FROM products
				WHERE company_id = ? AND id = ? AND active = TRUE
				FOR UPDATE
				""", UUID.class, companyId, productId).isEmpty();
	}

	@Override
	public List<StockLevel> findStock(UUID companyId) {
		return jdbc.query("""
				SELECT p.id, p.sku, p.name, p.unit_of_measure,
				       COALESCE(s.quantity, CAST(0 AS NUMERIC(19,4))),
				       COALESCE(s.updated_at, p.updated_at)
				FROM products p
				LEFT JOIN inventory_stocks s
				  ON s.company_id = p.company_id AND s.product_id = p.id
				WHERE p.company_id = ?
				ORDER BY p.name ASC, p.id ASC
				""", this::mapStock, companyId);
	}

	@Override
	public List<InventoryMovement> findMovements(UUID companyId) {
		return jdbc.query("""
				SELECT m.id, m.company_id, m.product_id, p.sku, p.name,
				       m.movement_type, m.quantity, m.responsible_user_id,
				       m.responsible_user_email, m.occurred_at
				FROM inventory_movements m
				JOIN products p ON p.company_id = m.company_id AND p.id = m.product_id
				WHERE m.company_id = ?
				ORDER BY m.occurred_at DESC, m.id DESC
				""", this::mapMovement, companyId);
	}

	@Override
	public void addStock(UUID companyId, UUID productId, BigDecimal quantity, Instant occurredAt) {
		ensureStockRow(companyId, productId, occurredAt);
		jdbc.update("""
				UPDATE inventory_stocks
				SET quantity = quantity + ?, version = version + 1, updated_at = ?
				WHERE company_id = ? AND product_id = ?
				""", quantity, Timestamp.from(occurredAt), companyId, productId);
	}

	@Override
	public boolean removeStock(
			UUID companyId, UUID productId, BigDecimal quantity, Instant occurredAt) {
		ensureStockRow(companyId, productId, occurredAt);
		return jdbc.update("""
				UPDATE inventory_stocks
				SET quantity = quantity - ?, version = version + 1, updated_at = ?
				WHERE company_id = ? AND product_id = ? AND quantity >= ?
				""", quantity, Timestamp.from(occurredAt), companyId, productId, quantity) == 1;
	}

	@Override
	public InventoryMovement saveMovement(InventoryMovement movement) {
		jdbc.update("""
				INSERT INTO inventory_movements (
				    id, company_id, product_id, movement_type, quantity,
				    responsible_user_id, responsible_user_email, occurred_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				movement.id(), movement.companyId(), movement.productId(), movement.type().name(),
				movement.quantity(), movement.responsibleUserId(), movement.responsibleUserEmail(),
				Timestamp.from(movement.occurredAt()));
		return jdbc.queryForObject("""
				SELECT m.id, m.company_id, m.product_id, p.sku, p.name,
				       m.movement_type, m.quantity, m.responsible_user_id,
				       m.responsible_user_email, m.occurred_at
				FROM inventory_movements m
				JOIN products p ON p.company_id = m.company_id AND p.id = m.product_id
				WHERE m.company_id = ? AND m.id = ?
				""", this::mapMovement, movement.companyId(), movement.id());
	}

	private void ensureStockRow(UUID companyId, UUID productId, Instant occurredAt) {
		jdbc.update("""
				INSERT INTO inventory_stocks (company_id, product_id, quantity, version, updated_at)
				SELECT ?, ?, 0, 0, ?
				WHERE NOT EXISTS (
				    SELECT 1 FROM inventory_stocks WHERE company_id = ? AND product_id = ?)
				""", companyId, productId, Timestamp.from(occurredAt), companyId, productId);
	}

	private StockLevel mapStock(ResultSet result, int rowNumber) throws SQLException {
		return new StockLevel(
				result.getObject(1, UUID.class), result.getString(2), result.getString(3),
				UnitOfMeasure.valueOf(result.getString(4)), result.getBigDecimal(5),
				result.getTimestamp(6).toInstant());
	}

	private InventoryMovement mapMovement(ResultSet result, int rowNumber) throws SQLException {
		return new InventoryMovement(
				result.getObject(1, UUID.class), result.getObject(2, UUID.class),
				result.getObject(3, UUID.class), result.getString(4), result.getString(5),
				InventoryMovementType.valueOf(result.getString(6)), result.getBigDecimal(7),
				result.getObject(8, UUID.class), result.getString(9),
				result.getTimestamp(10).toInstant());
	}
}
