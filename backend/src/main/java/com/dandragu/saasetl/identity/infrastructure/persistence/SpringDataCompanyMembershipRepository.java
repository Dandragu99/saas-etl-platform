package com.dandragu.saasetl.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dandragu.saasetl.identity.domain.MembershipStatus;

interface SpringDataCompanyMembershipRepository extends JpaRepository<CompanyMembershipEntity, UUID> {

	Optional<CompanyMembershipEntity> findByUserIdAndCompanyId(UUID userId, UUID companyId);

	List<CompanyMembershipEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

	List<CompanyMembershipEntity> findByUserIdAndStatusOrderByCreatedAtAsc(UUID userId, MembershipStatus status);
}
