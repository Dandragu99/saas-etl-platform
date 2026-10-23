package com.dandragu.saasetl.product.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.product.application.DuplicateProductSkuException;
import com.dandragu.saasetl.product.application.ProductVersionConflictException;
import com.dandragu.saasetl.product.application.ProductQuery;
import com.dandragu.saasetl.product.application.ProductSort;
import com.dandragu.saasetl.product.application.ProductSortDirection;
import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductId;
import com.dandragu.saasetl.product.domain.ProductSku;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

@SpringBootTest
@Transactional
class JpaProductRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

	@Autowired
	private JpaProductRepository productRepository;
	@Autowired
	private CompanyRepository companyRepository;

	@Test
	void shouldIsolateProductsAndSkuUniquenessByCompany() {
		Company firstCompany = company("First", "EUR");
		Company secondCompany = company("Second", "USD");
		Product first = product(firstCompany, " sku-1 ");
		Product second = product(secondCompany, "SKU-1");

		productRepository.save(first);
		productRepository.save(second);

		assertThat(productRepository.findById(firstCompany.id(), first.id())).isPresent();
		assertThat(productRepository.findById(secondCompany.id(), first.id())).isEmpty();
		assertThat(productRepository.existsByNormalizedSku(firstCompany.id(), "SKU-1", null)).isTrue();
		assertThat(productRepository.existsByNormalizedSku(secondCompany.id(), "SKU-1", null)).isTrue();
	}

	@Test
	void shouldRejectDuplicateSkuEvenWhenTheExistingProductIsInactive() {
		Company company = company("Company", "EUR");
		Product original = product(company, "sku-1").changeActive(false, NOW.plusSeconds(1));
		productRepository.save(original);

		assertThatThrownBy(() -> productRepository.save(product(company, " SKU-1 ")))
				.isInstanceOf(DuplicateProductSkuException.class);
	}

	@Test
	void shouldRejectAnUpdateUsingAStaleVersion() {
		Company company = company("Company", "EUR");
		Product original = productRepository.save(product(company, "SKU-1"));
		Product firstUpdate = original.update(
				original.sku(), "First update", null, original.category(), original.price(),
				original.unitOfMeasure(), NOW.plusSeconds(1));
		Product stored = productRepository.save(firstUpdate);

		Product staleUpdate = original.update(
				original.sku(), "Stale update", null, original.category(), original.price(),
				original.unitOfMeasure(), NOW.plusSeconds(2));

		assertThat(stored.version()).isEqualTo(original.version() + 1);
		assertThatThrownBy(() -> productRepository.save(staleUpdate))
				.isInstanceOf(ProductVersionConflictException.class);
	}

	@Test
	void shouldSearchOnlyInsideTheRequestedCompanyWithFiltersAndPagination() {
		Company firstCompany = company("First", "EUR");
		Company secondCompany = company("Second", "USD");
		productRepository.save(product(firstCompany, "A-1", "Alpha chair", "Furniture", true));
		productRepository.save(product(firstCompany, "B-2", "Beta chair", "Furniture", false));
		productRepository.save(product(firstCompany, "C-3", "Cable", "Electronics", true));
		productRepository.save(product(secondCompany, "A-1", "Foreign chair", "Furniture", true));

		var firstPage = productRepository.search(firstCompany.id(), new ProductQuery(
				"chair", null, "furn", null, 0, 1, ProductSort.NAME, ProductSortDirection.DESC));
		var active = productRepository.search(firstCompany.id(), new ProductQuery(
				null, "c-3", null, true, 0, 10, ProductSort.SKU, ProductSortDirection.ASC));

		assertThat(firstPage.totalElements()).isEqualTo(2);
		assertThat(firstPage.totalPages()).isEqualTo(2);
		assertThat(firstPage.content()).extracting(Product::name).containsExactly("Beta chair");
		assertThat(active.content()).extracting(Product::name).containsExactly("Cable");
	}

	private Company company(String name, String currency) {
		Company company = Company.create(UUID.randomUUID(), name, new CurrencyCode(currency), NOW);
		return companyRepository.save(company);
	}

	private Product product(Company company, String sku) {
		return product(company, sku, "Product", "Category", true);
	}

	private Product product(
			Company company,
			String sku,
			String name,
			String category,
			boolean active) {
		Product product = Product.create(
				new ProductId(UUID.randomUUID()), company.id(), new ProductSku(sku), name, null,
				category, new BigDecimal("10.0000"), company.defaultCurrency(),
				UnitOfMeasure.UNIT, NOW);
		return active ? product : product.changeActive(false, NOW.plusSeconds(1));
	}
}
