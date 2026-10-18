package com.dandragu.saasetl.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.identity.domain.CompanyMembership;
import com.dandragu.saasetl.identity.domain.MembershipId;
import com.dandragu.saasetl.identity.domain.MembershipRole;
import com.dandragu.saasetl.identity.domain.MembershipStatus;
import com.dandragu.saasetl.identity.domain.UserId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "company_memberships",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_company_memberships_user_company",
				columnNames = {"user_id", "company_id"}))
class CompanyMembershipEntity {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MembershipRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipStatus status;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CompanyMembershipEntity() {
	}

	private CompanyMembershipEntity(CompanyMembership membership) {
		this.id = membership.id().value();
		this.userId = membership.userId().value();
		this.companyId = membership.companyId();
		this.role = membership.role();
		this.status = membership.status();
		this.version = membership.version();
		this.createdAt = membership.createdAt();
		this.updatedAt = membership.updatedAt();
	}

	static CompanyMembershipEntity fromDomain(CompanyMembership membership) {
		return new CompanyMembershipEntity(membership);
	}

	CompanyMembership toDomain() {
		return new CompanyMembership(
				new MembershipId(id),
				new UserId(userId),
				companyId,
				role,
				status,
				version,
				createdAt,
				updatedAt);
	}
}
