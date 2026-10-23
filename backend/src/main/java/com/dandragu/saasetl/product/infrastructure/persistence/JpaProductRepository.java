package com.dandragu.saasetl.product.infrastructure.persistence;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.product.application.DuplicateProductSkuException;
import com.dandragu.saasetl.product.application.ProductNotFoundException;
import com.dandragu.saasetl.product.application.ProductPage;
import com.dandragu.saasetl.product.application.ProductQuery;
import com.dandragu.saasetl.product.application.ProductRepository;
import com.dandragu.saasetl.product.application.ProductSortDirection;
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
	public ProductPage search(UUID companyId, ProductQuery query) {
		String sortProperty = switch (query.sort()) {
			case NAME -> "name";
			case SKU -> "normalizedSku";
			case CATEGORY -> "category";
			case PRICE -> "price";
			case CREATED_AT -> "createdAt";
			case UPDATED_AT -> "updatedAt";
		};
		Sort.Direction direction = query.direction() == ProductSortDirection.ASC
				? Sort.Direction.ASC
				: Sort.Direction.DESC;
		PageRequest pageable = PageRequest.of(
				query.page(),
				query.size(),
				Sort.by(new Sort.Order(direction, sortProperty), new Sort.Order(direction, "id")));

		Specification<ProductEntity> specification = (root, criteriaQuery, builder) ->
				builder.equal(root.get("companyId"), companyId);
		if (query.name() != null) {
			specification = specification.and(containsIgnoringCase("name", query.name()));
		}
		if (query.sku() != null) {
			specification = specification.and(containsIgnoringCase("normalizedSku", query.sku()));
		}
		if (query.category() != null) {
			specification = specification.and(containsIgnoringCase("category", query.category()));
		}
		if (query.active() != null) {
			specification = specification.and((root, criteriaQuery, builder) ->
					builder.equal(root.get("active"), query.active()));
		}

		var result = repository.findAll(specification, pageable);
		return new ProductPage(
				result.getContent().stream().map(ProductEntity::toDomain).toList(),
				result.getTotalElements(),
				query.page(),
				query.size());
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

	private Specification<ProductEntity> containsIgnoringCase(String property, String value) {
		String pattern = "%" + escapeLike(value.toLowerCase(Locale.ROOT)) + "%";
		return (root, criteriaQuery, builder) ->
				builder.like(builder.lower(root.get(property)), pattern, '\\');
	}

	private String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
