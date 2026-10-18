package com.dandragu.saasetl.identity.domain;

import java.time.Instant;
import java.util.Objects;

public record User(
		UserId id,
		EmailAddress email,
		String passwordHash,
		UserStatus status,
		long version,
		Instant createdAt,
		Instant updatedAt,
		Instant lastLoginAt) {

	public static final int MAX_PASSWORD_HASH_LENGTH = 255;

	public User {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(email, "email must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (passwordHash == null || passwordHash.isBlank() || passwordHash.length() > MAX_PASSWORD_HASH_LENGTH) {
			throw new IllegalArgumentException("passwordHash must not be blank or exceed 255 characters");
		}
		if (version < 0) {
			throw new IllegalArgumentException("version must not be negative");
		}
		if (updatedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("updatedAt must not be before createdAt");
		}
	}

	public static User register(UserId id, EmailAddress email, String passwordHash, Instant now) {
		return new User(id, email, passwordHash, UserStatus.ACTIVE, 0, now, now, null);
	}
}
