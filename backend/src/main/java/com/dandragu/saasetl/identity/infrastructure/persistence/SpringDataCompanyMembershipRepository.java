package com.dandragu.saasetl.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCompanyMembershipRepository extends JpaRepository<CompanyMembershipEntity, UUID> {

	Optional<CompanyMembershipEntity> findByUserIdAndCompanyId(UUID userId, UUID companyId);

	List<CompanyMembershipEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
