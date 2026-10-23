package com.dandragu.saasetl.product.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeProductStatusRequest(
		@NotNull Boolean active,
		@NotNull @PositiveOrZero Long expectedVersion) {
}
