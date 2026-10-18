package com.dandragu.saasetl.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.identity.application.CompanyMembershipRepository;
import com.dandragu.saasetl.identity.domain.CompanyMembership;
import com.dandragu.saasetl.identity.domain.MembershipId;
import com.dandragu.saasetl.identity.domain.UserId;

@Repository
public class JpaCompanyMembershipRepository implements CompanyMembershipRepository {

	private final SpringDataCompanyMembershipRepository repository;

	public JpaCompanyMembershipRepository(SpringDataCompanyMembershipRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public CompanyMembership save(CompanyMembership membership) {
		return repository.save(CompanyMembershipEntity.fromDomain(membership)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CompanyMembership> findById(MembershipId id) {
		return repository.findById(id.value()).map(CompanyMembershipEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CompanyMembership> findByUserIdAndCompanyId(UserId userId, UUID companyId) {
		return repository.findByUserIdAndCompanyId(userId.value(), companyId)
				.map(CompanyMembershipEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CompanyMembership> findByUserId(UserId userId) {
		return repository.findByUserIdOrderByCreatedAtAsc(userId.value()).stream()
				.map(CompanyMembershipEntity::toDomain)
				.toList();
	}
}
