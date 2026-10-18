package com.dandragu.saasetl.product.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.product.application.DuplicateProductSkuException;
import com.dandragu.saasetl.product.application.ProductNotFoundException;
import com.dandragu.saasetl.product.application.ProductRepository;
import com.dandragu.saasetl.product.application.ProductVersionConflictException;
import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductId;

@Repository
public class JpaProductRepository implements ProductRepository {

	private final SpringDataProductRepository repository;

	public JpaProductRepository(SpringDataProductRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public Product save(Product product) {
		try {
			if (product.version() == 0 && !repository.existsById(product.id().value())) {
				return repository.saveAndFlush(ProductEntity.newEntity(product)).toDomain();
			}

			ProductEntity entity = repository
					.findByIdAndCompanyId(product.id().value(), product.companyId())
					.orElseThrow(ProductNotFoundException::new);
			if (entity.version() != product.version()) {
				throw new ProductVersionConflictException();
			}
			entity.updateFrom(product);
			repository.flush();
			return entity.toDomain();
		}
		catch (ObjectOptimisticLockingFailureException exception) {
			throw new ProductVersionConflictException(exception);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateProductSkuException();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Product> findById(UUID companyId, ProductId productId) {
		return repository.findByIdAndCompanyId(productId.value(), companyId)
				.map(ProductEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNormalizedSku(
			UUID companyId,
			String normalizedSku,
			ProductId excludedProductId) {
		if (excludedProductId == null) {
			return repository.existsByCompanyIdAndNormalizedSku(companyId, normalizedSku);
		}
		return repository.existsByCompanyIdAndNormalizedSkuAndIdNot(
				companyId, normalizedSku, excludedProductId.value());
	}
}
