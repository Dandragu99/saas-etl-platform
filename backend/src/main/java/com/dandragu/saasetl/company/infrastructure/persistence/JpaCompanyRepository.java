package com.dandragu.saasetl.company.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;

@Repository
public class JpaCompanyRepository implements CompanyRepository {

	private final SpringDataCompanyRepository repository;

	public JpaCompanyRepository(SpringDataCompanyRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public Company save(Company company) {
		return repository.save(CompanyEntity.fromDomain(company)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Company> findById(UUID id) {
		return repository.findById(id).map(CompanyEntity::toDomain);
	}
}
