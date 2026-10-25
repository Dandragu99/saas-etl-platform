package com.dandragu.saasetl.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dandragu.saasetl.company.application.CompanyAccess;
import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.application.CompanyPermission;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.identity.domain.MembershipRole;
import com.dandragu.saasetl.inventory.domain.InventoryMovement;
import com.dandragu.saasetl.inventory.domain.InventoryMovementType;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

	private final UUID companyId = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	@Mock
	private InventoryRepository repository;
	@Mock
	private CompanyAuthorizationService authorizationService;

	private InventoryService service;

	@BeforeEach
	void setUp() {
		service = new InventoryService(repository, authorizationService);
		Company company = Company.create(
				companyId, "Company", new CurrencyCode("EUR"), Instant.parse("2026-10-25T10:00:00Z"));
		when(authorizationService.require(any())).thenReturn(new CompanyAccess(company, MembershipRole.OWNER));
	}

	@Test
	void shouldReadStockAndHistoryOnlyForAuthorizedCompany() {
		when(repository.findStock(companyId)).thenReturn(List.of());
		when(repository.findMovements(companyId)).thenReturn(List.of());

		assertThat(service.findStock()).isEmpty();
		assertThat(service.findMovements()).isEmpty();

		verify(authorizationService, times(2)).require(CompanyPermission.INVENTORY_READ);
		verify(repository).findStock(companyId);
		verify(repository).findMovements(companyId);
	}

	@Test
	void shouldRegisterEntryWithResponsibleUserAndCompany() {
		when(repository.lockActiveProduct(companyId, productId)).thenReturn(true);
		when(repository.saveMovement(any())).thenAnswer(invocation -> invocation.getArgument(0));

		InventoryMovement result = service.register(command(InventoryMovementType.ENTRY, "2.5"));

		assertThat(result.companyId()).isEqualTo(companyId);
		assertThat(result.responsibleUserId()).isEqualTo(userId);
		assertThat(result.responsibleUserEmail()).isEqualTo("owner@example.com");
		assertThat(result.quantity()).isEqualByComparingTo("2.5000");
		verify(authorizationService).require(CompanyPermission.INVENTORY_WRITE);
		verify(repository).addStock(any(), any(), any(), any());
	}

	@Test
	void shouldRejectAnExitThatWouldCreateNegativeStock() {
		when(repository.lockActiveProduct(companyId, productId)).thenReturn(true);
		when(repository.removeStock(any(), any(), any(), any())).thenReturn(false);

		assertThatThrownBy(() -> service.register(command(InventoryMovementType.EXIT, "7")))
				.isInstanceOf(InsufficientStockException.class);
		verify(repository, never()).saveMovement(any());
	}

	@Test
	void shouldHideForeignOrInactiveProductsAndValidateQuantity() {
		when(repository.lockActiveProduct(companyId, productId)).thenReturn(false);

		assertThatThrownBy(() -> service.register(command(InventoryMovementType.ENTRY, "1")))
				.isInstanceOf(InventoryProductNotFoundException.class);
		assertThatThrownBy(() -> service.register(command(InventoryMovementType.ENTRY, "0")))
				.isInstanceOf(InvalidInventoryMovementException.class);
		assertThatThrownBy(() -> service.register(command(InventoryMovementType.ENTRY, "1.00001")))
				.isInstanceOf(InvalidInventoryMovementException.class);
	}

	private RegisterInventoryMovementCommand command(InventoryMovementType type, String quantity) {
		return new RegisterInventoryMovementCommand(
				productId, type, new BigDecimal(quantity), userId, " owner@example.com ");
	}
}
