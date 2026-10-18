package com.dandragu.saasetl.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CompanyMembership(
		MembershipId id,
		UserId userId,
		UUID companyId,
		MembershipRole role,
		MembershipStatus status,
		long version,
		Instant createdAt,
		Instant updatedAt) {

	public CompanyMembership {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(companyId, "companyId must not be null");
		Objects.requireNonNull(role, "role must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("version must not be negative");
		}
		if (updatedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("updatedAt must not be before createdAt");
		}
	}

	public static CompanyMembership owner(
			MembershipId id, UserId userId, UUID companyId, Instant now) {
		return new CompanyMembership(
				id, userId, companyId, MembershipRole.OWNER, MembershipStatus.ACTIVE, 0, now, now);
	}
}
