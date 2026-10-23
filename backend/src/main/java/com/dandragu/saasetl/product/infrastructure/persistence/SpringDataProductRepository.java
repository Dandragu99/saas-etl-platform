package com.dandragu.saasetl.product.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface SpringDataProductRepository extends JpaRepository<ProductEntity, UUID>,
		JpaSpecificationExecutor<ProductEntity> {

	Optional<ProductEntity> findByIdAndCompanyId(UUID id, UUID companyId);

	boolean existsByCompanyIdAndNormalizedSku(UUID companyId, String normalizedSku);

	boolean existsByCompanyIdAndNormalizedSkuAndIdNot(
			UUID companyId,
			String normalizedSku,
			UUID excludedId);
}
