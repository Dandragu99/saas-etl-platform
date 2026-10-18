package com.dandragu.saasetl.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dandragu.saasetl.company.application.CompanyContext;
import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductId;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	private final UUID companyId = UUID.randomUUID();

	@Mock
	private ProductRepository productRepository;
	@Mock
	private CompanyRepository companyRepository;
	@Mock
	private CompanyContext companyContext;

	private ProductService service;

	@BeforeEach
	void setUp() {
		service = new ProductService(productRepository, companyRepository, companyContext);
		when(companyContext.requireCompanyId()).thenReturn(companyId);
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
	}
}
