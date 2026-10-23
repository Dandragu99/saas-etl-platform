package com.dandragu.saasetl.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dandragu.saasetl.company.application.CompanyAccess;
import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.application.CompanyContext;
import com.dandragu.saasetl.company.application.CompanyPermission;
import com.dandragu.saasetl.company.application.CompanyPermissionDeniedException;
import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductId;
import com.dandragu.saasetl.product.domain.ProductSku;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;
import com.dandragu.saasetl.identity.domain.MembershipRole;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	private final UUID companyId = UUID.randomUUID();

	@Mock
	private ProductRepository productRepository;
	@Mock
	private CompanyRepository companyRepository;
	@Mock
	private CompanyAuthorizationService authorizationService;

	private ProductService service;

	@BeforeEach
	void setUp() {
		service = new ProductService(productRepository, companyRepository, authorizationService);
		Company company = Company.create(
				companyId, "Company", new CurrencyCode("EUR"), Instant.parse("2026-10-18T10:00:00Z"));
		lenient().when(authorizationService.require(any())).thenReturn(
				new CompanyAccess(company, com.dandragu.saasetl.identity.domain.MembershipRole.OWNER));
	}

	@Test
	void shouldCreateProductForTheContextCompanyUsingItsCurrency() {
		Company company = Company.create(
				companyId, "Company", new CurrencyCode("EUR"), Instant.parse("2026-10-18T10:00:00Z"));
		when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
		when(productRepository.save(any(Product.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Product result = service.create(new CreateProductCommand(
				" sku-1 ", "Product", null, "Category", new BigDecimal("9.99"),
				UnitOfMeasure.UNIT));

		assertThat(result.companyId()).isEqualTo(companyId);
		assertThat(result.currency()).isEqualTo(new CurrencyCode("EUR"));
		verify(authorizationService).require(CompanyPermission.PRODUCT_WRITE);
		verify(productRepository).existsByNormalizedSku(companyId, "SKU-1", null);
	}

	@Test
	void shouldRejectDuplicateSkuBeforeSaving() {
		Company company = Company.create(
				companyId, "Company", new CurrencyCode("EUR"), Instant.parse("2026-10-18T10:00:00Z"));
		when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
		when(productRepository.existsByNormalizedSku(companyId, "SKU-1", null)).thenReturn(true);

		assertThatThrownBy(() -> service.create(new CreateProductCommand(
				"sku-1", "Product", null, "Category", BigDecimal.ONE, UnitOfMeasure.UNIT)))
				.isInstanceOf(DuplicateProductSkuException.class);
	}

	@Test
	void shouldNotFindAProductOutsideTheContextCompany() {
		ProductId productId = new ProductId(UUID.randomUUID());
		when(productRepository.findById(companyId, productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(productId))
				.isInstanceOf(ProductNotFoundException.class);
		verify(authorizationService).require(CompanyPermission.PRODUCT_READ);
	}

	@ParameterizedTest
	@EnumSource(value = MembershipRole.class, names = {"INVENTORY_MANAGER", "VIEWER"})
	void shouldPreventReadOnlyRolesFromCreatingProducts(MembershipRole role) {
		Company company = Company.create(
				companyId, "Company", new CurrencyCode("EUR"), Instant.parse("2026-10-18T10:00:00Z"));
		CompanyContext context = org.mockito.Mockito.mock(CompanyContext.class);
		when(context.requireAccess()).thenReturn(new CompanyAccess(company, role));
		service = new ProductService(
				productRepository, companyRepository, new CompanyAuthorizationService(context));

		assertThatThrownBy(() -> service.create(new CreateProductCommand(
				"sku-1", "Product", null, "Category", BigDecimal.ONE, UnitOfMeasure.UNIT)))
				.isInstanceOf(CompanyPermissionDeniedException.class);
	}

	@Test
	void shouldUpdateAndDeactivateAProductUsingTheExpectedVersion() {
		Product current = product(2, true);
		when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
		when(productRepository.findById(companyId, current.id())).thenReturn(Optional.of(current));
		when(productRepository.existsByNormalizedSku(companyId, "SKU-2", current.id()))
				.thenReturn(false);
		when(productRepository.save(any(Product.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Product updated = service.update(new UpdateProductCommand(
				current.id(), 2, "SKU-2", "Updated", "Description", "New category",
				new BigDecimal("20.0000"), UnitOfMeasure.KG));
		Product inactive = service.deactivate(current.id(), 2);

		assertThat(updated.name()).isEqualTo("Updated");
		assertThat(updated.companyId()).isEqualTo(companyId);
		assertThat(inactive.active()).isFalse();
		verify(authorizationService, org.mockito.Mockito.times(2))
				.require(CompanyPermission.PRODUCT_WRITE);
	}

	@Test
	void shouldRejectStaleVersionAndForeignProductDuringWrites() {
		Product current = product(3, true);
		ProductId foreignId = new ProductId(UUID.randomUUID());
		when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
		when(productRepository.findById(companyId, current.id())).thenReturn(Optional.of(current));
		when(productRepository.findById(companyId, foreignId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(new UpdateProductCommand(
				current.id(), 2, "SKU-1", "Product", null, "Category",
				BigDecimal.ONE, UnitOfMeasure.UNIT)))
				.isInstanceOf(ProductVersionConflictException.class);
		assertThatThrownBy(() -> service.deactivate(foreignId, 0))
				.isInstanceOf(ProductNotFoundException.class);
	}

	private Company company() {
		return Company.create(
				companyId, "Company", new CurrencyCode("EUR"), Instant.parse("2026-10-18T10:00:00Z"));
	}

	private Product product(long version, boolean active) {
		Instant now = Instant.parse("2026-10-18T10:00:00Z");
		return new Product(
				new ProductId(UUID.randomUUID()), companyId, new ProductSku("SKU-1"), "Product", null,
				"Category", new BigDecimal("10.0000"), new CurrencyCode("EUR"),
				UnitOfMeasure.UNIT, active, version, now, now);
	}
}
