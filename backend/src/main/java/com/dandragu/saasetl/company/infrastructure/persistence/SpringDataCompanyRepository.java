package com.dandragu.saasetl.company.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCompanyRepository extends JpaRepository<CompanyEntity, UUID> {
}
