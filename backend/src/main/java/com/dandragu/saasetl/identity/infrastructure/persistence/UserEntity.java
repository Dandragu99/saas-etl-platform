package com.dandragu.saasetl.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.identity.domain.EmailAddress;
import com.dandragu.saasetl.identity.domain.User;
import com.dandragu.saasetl.identity.domain.UserId;
import com.dandragu.saasetl.identity.domain.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
class UserEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = EmailAddress.MAX_LENGTH)
	private String email;

	@Column(name = "normalized_email", nullable = false, unique = true, length = EmailAddress.MAX_LENGTH)
	private String normalizedEmail;

	@Column(name = "password_hash", nullable = false, length = User.MAX_PASSWORD_HASH_LENGTH)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	protected UserEntity() {
	}

	private UserEntity(User user) {
		this.id = user.id().value();
		this.email = user.email().value();
		this.normalizedEmail = user.email().value();
		this.passwordHash = user.passwordHash();
		this.status = user.status();
		this.version = user.version();
		this.createdAt = user.createdAt();
		this.updatedAt = user.updatedAt();
		this.lastLoginAt = user.lastLoginAt();
	}

	static UserEntity fromDomain(User user) {
		return new UserEntity(user);
	}

	User toDomain() {
		return new User(
				new UserId(id),
				new EmailAddress(email),
				passwordHash,
				status,
				version,
				createdAt,
				updatedAt,
				lastLoginAt);
	}
}
