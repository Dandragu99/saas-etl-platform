package com.dandragu.saasetl.company.application;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.dandragu.saasetl.identity.domain.MembershipRole;

@Service
public class CompanyAuthorizationService {

	private static final Map<MembershipRole, EnumSet<CompanyPermission>> PERMISSIONS = permissions();

	private final CompanyContext companyContext;

	public CompanyAuthorizationService(CompanyContext companyContext) {
		this.companyContext = companyContext;
	}

	public CompanyAccess require(CompanyPermission permission) {
		CompanyAccess access = companyContext.requireAccess();
		if (!PERMISSIONS.get(access.role()).contains(permission)) {
			throw new CompanyPermissionDeniedException();
		}
		return access;
	}

	private static Map<MembershipRole, EnumSet<CompanyPermission>> permissions() {
		var permissions = new EnumMap<MembershipRole, EnumSet<CompanyPermission>>(MembershipRole.class);
		permissions.put(MembershipRole.OWNER, EnumSet.allOf(CompanyPermission.class));
		permissions.put(MembershipRole.ADMIN, EnumSet.of(
				CompanyPermission.COMPANY_READ,
				CompanyPermission.COMPANY_ADMIN,
				CompanyPermission.PRODUCT_READ,
				CompanyPermission.PRODUCT_WRITE,
				CompanyPermission.INVENTORY_READ,
				CompanyPermission.INVENTORY_WRITE,
				CompanyPermission.ETL_READ,
				CompanyPermission.ETL_EXECUTE));
		permissions.put(MembershipRole.INVENTORY_MANAGER, EnumSet.of(
				CompanyPermission.COMPANY_READ,
				CompanyPermission.PRODUCT_READ,
				CompanyPermission.INVENTORY_READ,
				CompanyPermission.INVENTORY_WRITE,
				CompanyPermission.ETL_READ,
				CompanyPermission.ETL_EXECUTE));
		permissions.put(MembershipRole.VIEWER, EnumSet.of(
				CompanyPermission.COMPANY_READ,
				CompanyPermission.PRODUCT_READ,
				CompanyPermission.INVENTORY_READ,
				CompanyPermission.ETL_READ));
		return Map.copyOf(permissions);
	}
}
