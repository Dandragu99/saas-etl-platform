package com.dandragu.saasetl.product.application;

import java.util.Optional;
import java.util.UUID;

import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductId;

public interface ProductRepository {

	Product save(Product product);

	Optional<Product> findById(UUID companyId, ProductId productId);

	ProductPage search(UUID companyId, ProductQuery query);

	boolean existsByNormalizedSku(UUID companyId, String normalizedSku, ProductId excludedProductId);
}
