package com.dandragu.saasetl.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

	Optional<UserEntity> findByNormalizedEmail(String normalizedEmail);

	boolean existsByNormalizedEmail(String normalizedEmail);
}
