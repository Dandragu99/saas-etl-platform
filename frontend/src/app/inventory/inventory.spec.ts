import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuthSessionService } from '../auth/auth-session.service';
import { AuthSession } from '../auth/auth.models';
import { InventoryApiService } from './inventory-api.service';
import { Inventory } from './inventory';

describe('Inventory', () => {
  const session = signal<AuthSession>({
    userId: 'user-1',
    email: 'manager@example.com',
    activeCompany: { id: 'company-1', name: 'Company', defaultCurrency: 'EUR', role: 'INVENTORY_MANAGER' as const },
  });

  beforeEach(async () => {
    session.set({
      userId: 'user-1', email: 'manager@example.com',
      activeCompany: { id: 'company-1', name: 'Company', defaultCurrency: 'EUR', role: 'INVENTORY_MANAGER' },
    });
    await TestBed.configureTestingModule({
      imports: [Inventory],
      providers: [
        { provide: AuthSessionService, useValue: { session: session.asReadonly() } },
        {
          provide: InventoryApiService,
          useValue: {
            getInventory: () => of({
              stocks: [{ productId: 'p1', productSku: 'A-1', productName: 'Alpha', unitOfMeasure: 'UNIT', quantity: '4.0000', updatedAt: '2026-10-25T10:00:00Z' }],
              movements: [{ id: 'm1', productId: 'p1', productSku: 'A-1', productName: 'Alpha', type: 'ENTRY', quantity: '4.0000', responsibleUserId: 'user-1', responsibleUserEmail: 'manager@example.com', occurredAt: '2026-10-25T10:00:00Z' }],
            }),
            registerMovement: () => of({}),
          },
        },
      ],
    }).compileComponents();
  });

  it('should show stock, history and movement form to an inventory manager', async () => {
    const fixture = TestBed.createComponent(Inventory);
    await fixture.whenStable();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('4.0000');
    expect(element.textContent).toContain('manager@example.com');
    expect(element.querySelector('.movement-form')).not.toBeNull();
  });

  it('should keep inventory read-only for viewers', async () => {
    session.set({
      userId: 'user-2', email: 'viewer@example.com',
      activeCompany: { id: 'company-1', name: 'Company', defaultCurrency: 'EUR', role: 'VIEWER' },
    });
    const fixture = TestBed.createComponent(Inventory);
    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.movement-form')).toBeNull();
  });
});
