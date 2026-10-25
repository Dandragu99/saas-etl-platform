import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { InventoryApiService } from './inventory-api.service';

describe('InventoryApiService', () => {
  let service: InventoryApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InventoryApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should load company-scoped stock and movements', async () => {
    const result = firstValueFrom(service.getInventory());
    const stock = http.expectOne('/api/inventory/stocks');
    const movements = http.expectOne('/api/inventory/movements');
    expect(stock.request.method).toBe('GET');
    expect(movements.request.method).toBe('GET');
    stock.flush([{ productId: 'p1', productSku: 'A', productName: 'Alpha', unitOfMeasure: 'UNIT', quantity: '3.0000', updatedAt: '2026-10-25T10:00:00Z' }]);
    movements.flush([]);
    await expect(result).resolves.toMatchObject({ stocks: [{ quantity: '3.0000' }], movements: [] });
  });

  it('should obtain CSRF and register a movement without accepting company or user fields', async () => {
    const body = { productId: 'p1', type: 'EXIT' as const, quantity: '2.0000' };
    const result = firstValueFrom(service.registerMovement(body));
    http.expectOne('/api/auth/csrf').flush({});
    const request = http.expectOne('/api/inventory/movements');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.body).not.toHaveProperty('companyId');
    expect(request.request.body).not.toHaveProperty('responsibleUserId');
    request.flush({
      id: 'm1', ...body, productSku: 'A', productName: 'Alpha',
      responsibleUserId: 'u1', responsibleUserEmail: 'owner@example.com',
      occurredAt: '2026-10-25T10:00:00Z',
    });
    await expect(result).resolves.toMatchObject({ id: 'm1', type: 'EXIT' });
  });

  it('should preserve insufficient stock errors', async () => {
    const result = firstValueFrom(
      service.registerMovement({ productId: 'p1', type: 'EXIT', quantity: '20' }),
    );
    http.expectOne('/api/auth/csrf').flush({});
    http.expectOne('/api/inventory/movements').flush(
      {
        code: 'INSUFFICIENT_STOCK', message: 'La salida supera el stock disponible.',
        status: 409, path: '/api/inventory/movements', timestamp: '2026-10-25T10:00:00Z',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await expect(result).rejects.toMatchObject({ code: 'INSUFFICIENT_STOCK', status: 409 });
  });
});
