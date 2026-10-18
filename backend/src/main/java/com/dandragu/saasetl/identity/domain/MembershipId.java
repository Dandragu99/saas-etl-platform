package com.dandragu.saasetl.identity.domain;

import java.util.Objects;
import java.util.UUID;

public record MembershipId(UUID value) {

	public MembershipId {
		Objects.requireNonNull(value, "value must not be null");
	}
}
