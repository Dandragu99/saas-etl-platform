package com.dandragu.saasetl.company.application;

import java.util.Optional;
import java.util.UUID;

import com.dandragu.saasetl.company.domain.Company;

public interface CompanyRepository {

	Company save(Company company);

	Optional<Company> findById(UUID id);
}
