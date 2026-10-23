package com.dandragu.saasetl.product.api;

import java.util.Locale;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.product.application.ProductQuery;
import com.dandragu.saasetl.product.application.ProductService;
import com.dandragu.saasetl.product.application.ProductSort;
import com.dandragu.saasetl.product.application.ProductSortDirection;
import com.dandragu.saasetl.product.domain.ProductId;

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
		ProductId productId;
		try {
			productId = new ProductId(UUID.fromString(id));
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidProductQueryException("product id must be a valid UUID");
		}
		return ProductResponse.from(productService.findById(productId));
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
}
