package com.dandragu.saasetl.inventory.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.application.CompanyPermission;
import com.dandragu.saasetl.inventory.domain.InventoryMovement;
import com.dandragu.saasetl.inventory.domain.InventoryMovementType;
import com.dandragu.saasetl.inventory.domain.StockLevel;

@Service
public class InventoryService {

	private static final int QUANTITY_SCALE = 4;
	private static final int QUANTITY_INTEGER_DIGITS = 15;

	private final InventoryRepository repository;
	private final CompanyAuthorizationService authorizationService;

	public InventoryService(
			InventoryRepository repository,
			CompanyAuthorizationService authorizationService) {
		this.repository = repository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public List<StockLevel> findStock() {
		UUID companyId = authorizationService.require(CompanyPermission.INVENTORY_READ).company().id();
		return repository.findStock(companyId);
	}

	@Transactional(readOnly = true)
	public List<InventoryMovement> findMovements() {
		UUID companyId = authorizationService.require(CompanyPermission.INVENTORY_READ).company().id();
		return repository.findMovements(companyId);
	}

	@Transactional
	public InventoryMovement register(RegisterInventoryMovementCommand command) {
		if (command == null || command.productId() == null || command.type() == null
				|| command.responsibleUserId() == null
				|| command.responsibleUserEmail() == null
				|| command.responsibleUserEmail().isBlank()) {
			throw new InvalidInventoryMovementException("required movement data is missing");
		}
		BigDecimal quantity = normalizeQuantity(command.quantity());
		UUID companyId = authorizationService.require(CompanyPermission.INVENTORY_WRITE).company().id();
		if (!repository.lockActiveProduct(companyId, command.productId())) {
			throw new InventoryProductNotFoundException();
		}

		Instant occurredAt = Instant.now();
		if (command.type() == InventoryMovementType.ENTRY) {
			repository.addStock(companyId, command.productId(), quantity, occurredAt);
		}
		else if (!repository.removeStock(companyId, command.productId(), quantity, occurredAt)) {
			throw new InsufficientStockException();
		}

		return repository.saveMovement(new InventoryMovement(
				UUID.randomUUID(), companyId, command.productId(), null, null,
				command.type(), quantity, command.responsibleUserId(),
				command.responsibleUserEmail().trim(), occurredAt));
	}

	private BigDecimal normalizeQuantity(BigDecimal value) {
		if (value == null || value.signum() <= 0 || value.scale() > QUANTITY_SCALE) {
			throw new InvalidInventoryMovementException("quantity must be positive with at most 4 decimals");
		}
		int integerDigits = Math.max(0, value.precision() - value.scale());
		if (integerDigits > QUANTITY_INTEGER_DIGITS) {
			throw new InvalidInventoryMovementException("quantity is too large");
		}
		return value.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
	}
}
