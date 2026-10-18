package com.dandragu.saasetl.identity.domain;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

	public UserId {
		Objects.requireNonNull(value, "value must not be null");
	}
}
