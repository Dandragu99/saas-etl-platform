package com.dandragu.saasetl.product.domain;

import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {

	public ProductId {
		Objects.requireNonNull(value, "value must not be null");
	}
}
