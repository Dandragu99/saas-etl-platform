package com.dandragu.saasetl.company.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.identity.domain.MembershipRole;

class CompanyAuthorizationServiceTest {

	@ParameterizedTest
	@EnumSource(MembershipRole.class)
	void shouldApplyThePermissionMatrix(MembershipRole role) {
		CompanyContext context = mock(CompanyContext.class);
		when(context.requireAccess()).thenReturn(access(role));
		CompanyAuthorizationService service = new CompanyAuthorizationService(context);

		for (CompanyPermission permission : CompanyPermission.values()) {
			if (allowed(role).contains(permission)) {
				assertThatCode(() -> service.require(permission)).doesNotThrowAnyException();
			} else {
				assertThatThrownBy(() -> service.require(permission))
						.isInstanceOf(CompanyPermissionDeniedException.class);
			}
		}
	}

	private CompanyAccess access(MembershipRole role) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new CompanyAccess(
				Company.create(UUID.randomUUID(), "Test Company", new CurrencyCode("EUR"), now),
				role);
	}

	private EnumSet<CompanyPermission> allowed(MembershipRole role) {
		return switch (role) {
			case OWNER -> EnumSet.allOf(CompanyPermission.class);
			case ADMIN -> EnumSet.of(
					CompanyPermission.COMPANY_READ,
					CompanyPermission.COMPANY_ADMIN,
					CompanyPermission.PRODUCT_READ,
					CompanyPermission.PRODUCT_WRITE,
					CompanyPermission.INVENTORY_READ,
					CompanyPermission.INVENTORY_WRITE,
					CompanyPermission.ETL_READ,
					CompanyPermission.ETL_EXECUTE);
			case INVENTORY_MANAGER -> EnumSet.of(
					CompanyPermission.COMPANY_READ,
					CompanyPermission.PRODUCT_READ,
					CompanyPermission.INVENTORY_READ,
					CompanyPermission.INVENTORY_WRITE,
					CompanyPermission.ETL_READ,
					CompanyPermission.ETL_EXECUTE);
			case VIEWER -> EnumSet.of(
					CompanyPermission.COMPANY_READ,
					CompanyPermission.PRODUCT_READ,
					CompanyPermission.INVENTORY_READ,
					CompanyPermission.ETL_READ);
		};
	}
}
