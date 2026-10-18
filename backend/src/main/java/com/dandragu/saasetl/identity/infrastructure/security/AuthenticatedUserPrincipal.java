package com.dandragu.saasetl.identity.infrastructure.security;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUserPrincipal(UUID userId, String email) implements Principal, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public AuthenticatedUserPrincipal {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(email, "email must not be null");
	}

	@Override
	public String getName() {
		return userId.toString();
	}
}
