package com.dandragu.saasetl.inventory.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.inventory.application.InvalidInventoryMovementException;
import com.dandragu.saasetl.inventory.application.InventoryService;
import com.dandragu.saasetl.inventory.application.RegisterInventoryMovementCommand;
import com.dandragu.saasetl.identity.infrastructure.security.AuthenticatedUserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/api/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
public class InventoryController {

	private final InventoryService service;

	public InventoryController(InventoryService service) {
		this.service = service;
	}

	@GetMapping("/stocks")
	public List<StockLevelResponse> getStock() {
		return service.findStock().stream().map(StockLevelResponse::from).toList();
	}

	@GetMapping("/movements")
	public List<InventoryMovementResponse> getMovements() {
		return service.findMovements().stream().map(InventoryMovementResponse::from).toList();
	}

	@PostMapping(path = "/movements", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public InventoryMovementResponse registerMovement(
			@Valid @RequestBody RegisterInventoryMovementRequest request,
			Authentication authentication) {
		AuthenticatedUserPrincipal principal = principal(authentication);
		try {
			return InventoryMovementResponse.from(service.register(new RegisterInventoryMovementCommand(
					UUID.fromString(request.productId()), request.type(),
					new BigDecimal(request.quantity()), principal.userId(), principal.email())));
		}
		catch (InvalidInventoryMovementException exception) {
			throw exception;
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidInventoryMovementException("invalid movement request");
		}
	}

	private AuthenticatedUserPrincipal principal(Authentication authentication) {
		if (authentication == null
				|| !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
			throw new InvalidInventoryMovementException("authenticated user is required");
		}
		return principal;
	}
}
