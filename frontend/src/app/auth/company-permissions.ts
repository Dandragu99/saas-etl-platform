import { MembershipRole } from './auth.models';

export type CompanyPermission = 'PRODUCT_READ' | 'PRODUCT_WRITE';

const ROLE_PERMISSIONS: Readonly<Record<MembershipRole, readonly CompanyPermission[]>> = {
  OWNER: ['PRODUCT_READ', 'PRODUCT_WRITE'],
  ADMIN: ['PRODUCT_READ', 'PRODUCT_WRITE'],
  INVENTORY_MANAGER: ['PRODUCT_READ'],
  VIEWER: ['PRODUCT_READ'],
};

export function hasCompanyPermission(
  role: MembershipRole | null | undefined,
  permission: CompanyPermission,
): boolean {
  return role !== null && role !== undefined && ROLE_PERMISSIONS[role].includes(permission);
}
