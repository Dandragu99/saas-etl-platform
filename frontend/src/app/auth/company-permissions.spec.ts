import { hasCompanyPermission } from './company-permissions';

describe('hasCompanyPermission', () => {
  it('should expose product write only for owner and admin', () => {
    expect(hasCompanyPermission('OWNER', 'PRODUCT_WRITE')).toBe(true);
    expect(hasCompanyPermission('ADMIN', 'PRODUCT_WRITE')).toBe(true);
    expect(hasCompanyPermission('INVENTORY_MANAGER', 'PRODUCT_WRITE')).toBe(false);
    expect(hasCompanyPermission('VIEWER', 'PRODUCT_WRITE')).toBe(false);
  });

  it('should allow inventory writes for owner, admin and inventory manager', () => {
    expect(hasCompanyPermission('OWNER', 'INVENTORY_WRITE')).toBe(true);
    expect(hasCompanyPermission('ADMIN', 'INVENTORY_WRITE')).toBe(true);
    expect(hasCompanyPermission('INVENTORY_MANAGER', 'INVENTORY_WRITE')).toBe(true);
    expect(hasCompanyPermission('VIEWER', 'INVENTORY_WRITE')).toBe(false);
    expect(hasCompanyPermission('VIEWER', 'INVENTORY_READ')).toBe(true);
  });
});
