package com.dandragu.saasetl.product.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dandragu.saasetl.common.api.error.ApiExceptionHandler;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.product.application.ProductNotFoundException;
import com.dandragu.saasetl.product.application.ProductPage;
import com.dandragu.saasetl.product.application.ProductQuery;
import com.dandragu.saasetl.product.application.ProductService;
import com.dandragu.saasetl.product.domain.Product;
import com.dandragu.saasetl.product.domain.ProductId;
import com.dandragu.saasetl.product.domain.ProductSku;
import com.dandragu.saasetl.product.domain.UnitOfMeasure;

class ProductControllerTest {

	private ProductService productService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		productService = mock(ProductService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void shouldReturnAProductPageWithDecimalPriceAsText() throws Exception {
		Product product = product();
		when(productService.search(any(ProductQuery.class)))
				.thenReturn(new ProductPage(List.of(product), 1, 0, 20));

		mockMvc.perform(get("/api/products")
					.param("name", "chair")
					.param("active", "true")
					.param("sort", "price")
					.param("direction", "desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(product.id().value().toString()))
				.andExpect(jsonPath("$.content[0].price").value("12.3400"))
				.andExpect(jsonPath("$.content[0].companyId").doesNotExist())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));
	}

	@Test
	void shouldReturnBadRequestForUncontrolledSortOrPagination() throws Exception {
		mockMvc.perform(get("/api/products").param("sort", "companyId"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PRODUCT_QUERY"));
		mockMvc.perform(get("/api/products").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PRODUCT_QUERY"));
		mockMvc.perform(get("/api/products").param("active", "sometimes"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PRODUCT_QUERY"));
	}

	@Test
	void shouldReturnTheSameNotFoundResponseForMissingOrForeignProducts() throws Exception {
		when(productService.findById(any(ProductId.class))).thenThrow(new ProductNotFoundException());

		mockMvc.perform(get("/api/products/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("El producto no existe."));
	}

	private Product product() {
		Instant now = Instant.parse("2026-10-23T10:00:00Z");
		return Product.create(
				new ProductId(UUID.randomUUID()),
				UUID.randomUUID(),
				new ProductSku("CHAIR-1"),
				"Chair",
				null,
				"Furniture",
				new BigDecimal("12.3400"),
				new CurrencyCode("EUR"),
				UnitOfMeasure.UNIT,
				now);
	}
}
