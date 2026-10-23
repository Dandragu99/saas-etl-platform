package com.dandragu.saasetl.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductQueryTest {

	@Test
	void shouldNormalizeFiltersAndApplyControlledDefaults() {
		ProductQuery query = new ProductQuery(
				"  chair ", " ", null, true, 0, 20, null, null);

		assertThat(query.name()).isEqualTo("chair");
		assertThat(query.sku()).isNull();
		assertThat(query.sort()).isEqualTo(ProductSort.NAME);
		assertThat(query.direction()).isEqualTo(ProductSortDirection.ASC);
	}

	@Test
	void shouldRejectInvalidPagination() {
		assertThatThrownBy(() -> new ProductQuery(
				null, null, null, null, -1, 20, null, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ProductQuery(
				null, null, null, null, 0, ProductQuery.MAX_PAGE_SIZE + 1, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
