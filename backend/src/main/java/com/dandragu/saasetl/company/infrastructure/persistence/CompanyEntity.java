package com.dandragu.saasetl.company.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CompanyStatus;
import com.dandragu.saasetl.company.domain.CurrencyCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
class CompanyEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = Company.MAX_NAME_LENGTH)
	private String name;

	@Column(name = "default_currency", nullable = false, length = 3)
	private String defaultCurrency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CompanyStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CompanyEntity() {
	}

	private CompanyEntity(Company company) {
		this.id = company.id();
		this.name = company.name();
		this.defaultCurrency = company.defaultCurrency().value();
		this.status = company.status();
		this.createdAt = company.createdAt();
		this.updatedAt = company.updatedAt();
	}

	static CompanyEntity fromDomain(Company company) {
		return new CompanyEntity(company);
	}

	Company toDomain() {
		return new Company(
				id,
				name,
				new CurrencyCode(defaultCurrency),
				status,
				createdAt,
				updatedAt);
	}
}
