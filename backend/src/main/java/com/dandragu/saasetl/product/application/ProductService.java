package com.dandragu.saasetl.product.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.company.application.CompanyAuthorizationService;
import com.dandragu.saasetl.company.application.CompanyPermission;
import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CompanyStatus;
import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductId;
import com.dandragu.saasetl.product.domain.ProductSku;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final CompanyRepository companyRepository;
	private final CompanyAuthorizationService authorizationService;

	public ProductService(
			ProductRepository productRepository,
			CompanyRepository companyRepository,
			CompanyAuthorizationService authorizationService) {
		this.productRepository = productRepository;
		this.companyRepository = companyRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional
	public Product create(CreateProductCommand command) {
		Company company = requireActiveCompany(CompanyPermission.PRODUCT_WRITE);
		ProductSku sku = new ProductSku(command.sku());
		requireUniqueSku(company.id(), sku, null);

		Product product = Product.create(
				new ProductId(UUID.randomUUID()),
				company.id(),
				sku,
				command.name(),
				command.description(),
				command.category(),
				command.price(),
				company.defaultCurrency(),
				command.unitOfMeasure(),
				Instant.now());
		return productRepository.save(product);
	}

	@Transactional(readOnly = true)
	public Product findById(ProductId productId) {
		UUID companyId = requireCompanyId(CompanyPermission.PRODUCT_READ);
		return productRepository.findById(companyId, productId)
				.orElseThrow(ProductNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public ProductPage search(ProductQuery query) {
		return productRepository.search(requireCompanyId(CompanyPermission.PRODUCT_READ), query);
	}

	@Transactional
	public Product update(UpdateProductCommand command) {
		UUID companyId = requireActiveCompany(CompanyPermission.PRODUCT_WRITE).id();
		Product current = requireProduct(companyId, command.productId());
		requireExpectedVersion(current, command.expectedVersion());
		ProductSku sku = new ProductSku(command.sku());
		requireUniqueSku(companyId, sku, current.id());

		return productRepository.save(current.update(
				sku,
				command.name(),
				command.description(),
				command.category(),
				command.price(),
				command.unitOfMeasure(),
				Instant.now()));
	}

	@Transactional
	public Product deactivate(ProductId productId, long expectedVersion) {
		return changeActive(productId, expectedVersion, false);
	}

	@Transactional
	public Product reactivate(ProductId productId, long expectedVersion) {
		return changeActive(productId, expectedVersion, true);
	}

	private Product changeActive(ProductId productId, long expectedVersion, boolean active) {
		UUID companyId = requireActiveCompany(CompanyPermission.PRODUCT_WRITE).id();
		Product current = requireProduct(companyId, productId);
		requireExpectedVersion(current, expectedVersion);
		if (current.active() == active) {
			return current;
		}
		return productRepository.save(current.changeActive(active, Instant.now()));
	}

	private Company requireActiveCompany(CompanyPermission permission) {
		UUID companyId = requireCompanyId(permission);
		Company company = companyRepository.findById(companyId)
				.orElseThrow(ProductCompanyUnavailableException::new);
		if (company.status() != CompanyStatus.ACTIVE) {
			throw new ProductCompanyUnavailableException();
		}
		return company;
	}

	private UUID requireCompanyId(CompanyPermission permission) {
		return authorizationService.require(permission).company().id();
	}

	private Product requireProduct(UUID companyId, ProductId productId) {
		return productRepository.findById(companyId, productId)
				.orElseThrow(ProductNotFoundException::new);
	}

	private void requireUniqueSku(UUID companyId, ProductSku sku, ProductId excludedProductId) {
		if (productRepository.existsByNormalizedSku(
				companyId, sku.normalizedValue(), excludedProductId)) {
			throw new DuplicateProductSkuException();
		}
	}

	private void requireExpectedVersion(Product product, long expectedVersion) {
		if (product.version() != expectedVersion) {
			throw new ProductVersionConflictException();
		}
	}
}
