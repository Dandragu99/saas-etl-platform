package com.dandragu.saasetl.product.api;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.product.application.CreateProductCommand;
import com.dandragu.saasetl.product.application.ProductQuery;
import com.dandragu.saasetl.product.application.ProductService;
import com.dandragu.saasetl.product.application.ProductSort;
import com.dandragu.saasetl.product.application.ProductSortDirection;
import com.dandragu.saasetl.product.application.UpdateProductCommand;
import com.dandragu.saasetl.product.domain.ProductId;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public ProductPageResponse getProducts(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String sku,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String active,
			@RequestParam(defaultValue = "0") String page,
			@RequestParam(defaultValue = "20") String size,
			@RequestParam(defaultValue = "name") String sort,
			@RequestParam(defaultValue = "asc") String direction) {
		ProductQuery query;
		try {
			query = new ProductQuery(
					name,
					sku,
					category,
					parseActive(active),
					Integer.parseInt(page),
					Integer.parseInt(size),
					ProductSort.valueOf(sort.trim().toUpperCase(Locale.ROOT)),
					ProductSortDirection.valueOf(direction.trim().toUpperCase(Locale.ROOT)));
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidProductQueryException(exception.getMessage());
		}
		return ProductPageResponse.from(productService.search(query));
	}

	@GetMapping("/{id}")
	public ProductResponse getProduct(@PathVariable String id) {
		return ProductResponse.from(productService.findById(parseProductId(id)));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProductResponse> createProduct(
			@Valid @RequestBody CreateProductRequest request) {
		try {
			var product = productService.create(new CreateProductCommand(
					request.sku(),
					request.name(),
					request.description(),
					request.category(),
					new BigDecimal(request.price()),
					request.unitOfMeasure()));
			return ResponseEntity.created(URI.create("/api/products/" + product.id().value()))
					.body(ProductResponse.from(product));
		}
		catch (IllegalArgumentException exception) {
			throw invalidRequest(exception);
		}
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ProductResponse updateProduct(
			@PathVariable String id,
			@Valid @RequestBody UpdateProductRequest request) {
		try {
			return ProductResponse.from(productService.update(new UpdateProductCommand(
					parseProductId(id),
					request.expectedVersion(),
					request.sku(),
					request.name(),
					request.description(),
					request.category(),
					new BigDecimal(request.price()),
					request.unitOfMeasure())));
		}
		catch (IllegalArgumentException exception) {
			throw invalidRequest(exception);
		}
	}

	@PatchMapping(path = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ProductResponse changeProductStatus(
			@PathVariable String id,
			@Valid @RequestBody ChangeProductStatusRequest request) {
		try {
			ProductId productId = parseProductId(id);
			return ProductResponse.from(request.active()
					? productService.reactivate(productId, request.expectedVersion())
					: productService.deactivate(productId, request.expectedVersion()));
		}
		catch (IllegalArgumentException exception) {
			throw invalidRequest(exception);
		}
	}

	private Boolean parseActive(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		if (value.equalsIgnoreCase("true")) {
			return true;
		}
		if (value.equalsIgnoreCase("false")) {
			return false;
		}
		throw new IllegalArgumentException("active must be true or false");
	}

	private ProductId parseProductId(String value) {
		try {
			return new ProductId(UUID.fromString(value));
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidProductRequestException("product id must be a valid UUID", exception);
		}
	}

	private InvalidProductRequestException invalidRequest(IllegalArgumentException exception) {
		return new InvalidProductRequestException("invalid product request", exception);
	}
}
