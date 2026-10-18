package com.dandragu.saasetl.identity.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dandragu.saasetl.identity.domain.CompanyMembership;
import com.dandragu.saasetl.identity.domain.MembershipId;
import com.dandragu.saasetl.identity.domain.UserId;

public interface CompanyMembershipRepository {

	CompanyMembership save(CompanyMembership membership);

	Optional<CompanyMembership> findById(MembershipId id);

	Optional<CompanyMembership> findByUserIdAndCompanyId(UserId userId, UUID companyId);

	List<CompanyMembership> findByUserId(UserId userId);

	List<CompanyMembership> findActiveByUserId(UserId userId);
}
