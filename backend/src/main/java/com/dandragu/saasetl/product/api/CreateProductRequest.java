package com.dandragu.saasetl.product.api;

import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductSku;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
		@NotBlank @Size(max = ProductSku.MAX_LENGTH) String sku,
		@NotBlank @Size(max = Product.MAX_NAME_LENGTH) String name,
		@Size(max = Product.MAX_DESCRIPTION_LENGTH) String description,
		@NotBlank @Size(max = Product.MAX_CATEGORY_LENGTH) String category,
		@NotBlank @Pattern(regexp = "[0-9]{1,15}(\\.[0-9]{1,4})?") String price,
		@NotNull UnitOfMeasure unitOfMeasure) {
}
