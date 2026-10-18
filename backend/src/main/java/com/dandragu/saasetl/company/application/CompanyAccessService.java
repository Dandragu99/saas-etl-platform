package com.dandragu.saasetl.company.application;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.company.domain.CompanyStatus;
import com.dandragu.saasetl.identity.application.CompanyMembershipRepository;
import com.dandragu.saasetl.identity.application.UserRepository;
import com.dandragu.saasetl.identity.domain.MembershipStatus;
import com.dandragu.saasetl.identity.domain.UserId;
import com.dandragu.saasetl.identity.domain.UserStatus;

@Service
public class CompanyAccessService {

	private final UserRepository userRepository;
	private final CompanyMembershipRepository membershipRepository;
	private final CompanyRepository companyRepository;

	public CompanyAccessService(
			UserRepository userRepository,
			CompanyMembershipRepository membershipRepository,
			CompanyRepository companyRepository) {
		this.userRepository = userRepository;
		this.membershipRepository = membershipRepository;
		this.companyRepository = companyRepository;
	}

	@Transactional(readOnly = true)
	public List<CompanyAccess> findAccessibleCompanies(UserId userId) {
		requireActiveUser(userId);
		var memberships = membershipRepository.findActiveByUserId(userId);
		var companies = companyRepository.findByIds(
				memberships.stream().map(membership -> membership.companyId()).toList()).stream()
				.filter(company -> company.status() == CompanyStatus.ACTIVE)
				.collect(Collectors.toMap(company -> company.id(), Function.identity()));
		return memberships.stream()
				.filter(membership -> companies.containsKey(membership.companyId()))
				.map(membership -> new CompanyAccess(
						companies.get(membership.companyId()), membership.role()))
				.toList();
	}

	@Transactional(readOnly = true)
	public CompanyAccess requireActiveAccess(UserId userId, UUID companyId) {
		requireActiveUser(userId);
		var membership = membershipRepository.findByUserIdAndCompanyId(userId, companyId)
				.filter(value -> value.status() == MembershipStatus.ACTIVE)
				.orElseThrow(CompanyAccessDeniedException::new);
		var company = companyRepository.findById(companyId)
				.filter(value -> value.status() == CompanyStatus.ACTIVE)
				.orElseThrow(CompanyAccessDeniedException::new);
		return new CompanyAccess(company, membership.role());
	}

	private void requireActiveUser(UserId userId) {
		boolean active = userRepository.findById(userId)
				.map(user -> user.status() == UserStatus.ACTIVE)
				.orElse(false);
		if (!active) {
			throw new CompanyAccessDeniedException();
		}
	}
}
