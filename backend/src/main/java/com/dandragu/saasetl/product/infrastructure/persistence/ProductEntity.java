package com.dandragu.saasetl.product.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductId;
import com.dandragu.saasetl.product.domain.ProductSku;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "products")
class ProductEntity {

	@Id
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Column(nullable = false, length = ProductSku.MAX_LENGTH)
	private String sku;

	@Column(name = "normalized_sku", nullable = false, length = ProductSku.MAX_LENGTH)
	private String normalizedSku;

	@Column(nullable = false, length = Product.MAX_NAME_LENGTH)
	private String name;

	@Column(length = Product.MAX_DESCRIPTION_LENGTH)
	private String description;

	@Column(nullable = false, length = Product.MAX_CATEGORY_LENGTH)
	private String category;

	@Column(nullable = false, precision = 19, scale = Product.PRICE_SCALE)
	private BigDecimal price;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "unit_of_measure", nullable = false, length = 30)
	private UnitOfMeasure unitOfMeasure;

	@Column(nullable = false)
	private boolean active;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ProductEntity() {
	}

	private ProductEntity(Product product) {
		this.id = product.id().value();
		this.companyId = product.companyId();
		updateFrom(product);
		this.createdAt = product.createdAt();
	}

	static ProductEntity newEntity(Product product) {
		return new ProductEntity(product);
	}

	void updateFrom(Product product) {
		this.sku = product.sku().value();
		this.normalizedSku = product.sku().normalizedValue();
		this.name = product.name();
		this.description = product.description();
		this.category = product.category();
		this.price = product.price();
		this.currency = product.currency().value();
		this.unitOfMeasure = product.unitOfMeasure();
		this.active = product.active();
		this.updatedAt = product.updatedAt();
	}

	long version() {
		return version == null ? 0 : version;
	}

	Product toDomain() {
		return new Product(
				new ProductId(id),
				companyId,
				new ProductSku(sku, normalizedSku),
				name,
				description,
				category,
				price,
				new CurrencyCode(currency),
				unitOfMeasure,
				active,
				version(),
				createdAt,
				updatedAt);
	}
}
