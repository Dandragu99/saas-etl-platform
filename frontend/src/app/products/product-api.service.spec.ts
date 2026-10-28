import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { ProductApiService } from './product-api.service';
import { Product, ProductPage, ProductRequestError } from './product.models';

const PRODUCT: Product = {
  id: 'product-1',
  sku: 'CHAIR-1',
  name: 'Chair',
  description: null,
  category: 'Furniture',
  price: '12.3400',
  currency: 'EUR',
  unitOfMeasure: 'UNIT',
  active: true,
  version: 0,
  createdAt: '2026-10-23T10:00:00Z',
  updatedAt: '2026-10-23T10:00:00Z',
};

describe('ProductApiService', () => {
  let service: ProductApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should send controlled pagination, sorting and product filters', async () => {
    const response: ProductPage = {
      content: [PRODUCT],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
    };
    const result = firstValueFrom(
      service.getProducts({
        search: ' Chair or CH ',
        name: ' Chair ',
        sku: ' CH ',
        category: ' Furniture ',
        active: false,
        page: 0,
        size: 20,
        sort: 'price',
        direction: 'desc',
      }),
    );
    const request = http.expectOne(
      '/api/products?page=0&size=20&sort=price&direction=desc&search=Chair%20or%20CH&name=Chair&sku=CH&category=Furniture&active=false',
    );

    expect(request.request.method).toBe('GET');
    request.flush(response);
    await expect(result).resolves.toEqual(response);
  });

  it('should omit empty optional filters', async () => {
    const result = firstValueFrom(
      service.getProducts({
        search: ' ',
        name: ' ',
        page: 2,
        size: 20,
        sort: 'name',
        direction: 'asc',
      }),
    );
    const request = http.expectOne('/api/products?page=2&size=20&sort=name&direction=asc');
    request.flush({ content: [], totalElements: 0, totalPages: 0, page: 2, size: 20 });
    await expect(result).resolves.toMatchObject({ page: 2 });
  });

  it('should request a product by id', async () => {
    const result = firstValueFrom(service.getProduct('product/1'));
    const request = http.expectOne('/api/products/product%2F1');
    expect(request.request.method).toBe('GET');
    request.flush(PRODUCT);
    await expect(result).resolves.toEqual(PRODUCT);
  });

  it('should expose safe network and permission errors', async () => {
    const query = { page: 0, size: 20, sort: 'name' as const, direction: 'asc' as const };
    const networkResult = firstValueFrom(service.getProducts(query));
    http
      .expectOne('/api/products?page=0&size=20&sort=name&direction=asc')
      .error(new ProgressEvent('network error'));
    await expect(networkResult).rejects.toEqual(
      new ProductRequestError(
        'No se pudo conectar con el servidor. Comprueba que el backend está disponible.',
      ),
    );

    const forbiddenResult = firstValueFrom(service.getProducts(query));
    http.expectOne('/api/products?page=0&size=20&sort=name&direction=asc').flush(null, {
      status: 403,
      statusText: 'Forbidden',
    });
    await expect(forbiddenResult).rejects.toEqual(
      new ProductRequestError(
        'No tienes permiso para realizar esta operación.',
        'COMPANY_PERMISSION_DENIED',
        403,
      ),
    );
  });

  it('should obtain CSRF before creating a product without sending company or currency', async () => {
    const body = {
      sku: 'CHAIR-1',
      name: 'Chair',
      description: null,
      category: 'Furniture',
      price: '12.3400',
      unitOfMeasure: 'UNIT' as const,
    };
    const result = firstValueFrom(service.createProduct(body));
    http.expectOne('/api/auth/csrf').flush({});
    const request = http.expectOne('/api/products');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.body).not.toHaveProperty('companyId');
    expect(request.request.body).not.toHaveProperty('currency');
    request.flush(PRODUCT);
    await expect(result).resolves.toEqual(PRODUCT);
  });

  it('should update and change status using the expected version', async () => {
    const updateResult = firstValueFrom(
      service.updateProduct(PRODUCT.id, {
        sku: PRODUCT.sku,
        name: 'Updated chair',
        description: null,
        category: PRODUCT.category,
        price: PRODUCT.price,
        unitOfMeasure: PRODUCT.unitOfMeasure,
        expectedVersion: 3,
      }),
    );
    http.expectOne('/api/auth/csrf').flush({});
    const update = http.expectOne(`/api/products/${PRODUCT.id}`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body.expectedVersion).toBe(3);
    update.flush({ ...PRODUCT, name: 'Updated chair', version: 4 });
    await expect(updateResult).resolves.toMatchObject({ version: 4 });

    const statusResult = firstValueFrom(
      service.changeProductStatus(PRODUCT.id, { active: false, expectedVersion: 4 }),
    );
    http.expectOne('/api/auth/csrf').flush({});
    const status = http.expectOne(`/api/products/${PRODUCT.id}/status`);
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ active: false, expectedVersion: 4 });
    status.flush({ ...PRODUCT, active: false, version: 5 });
    await expect(statusResult).resolves.toMatchObject({ active: false, version: 5 });
  });

  it('should preserve product conflict codes from the backend', async () => {
    const result = firstValueFrom(
      service.changeProductStatus(PRODUCT.id, { active: false, expectedVersion: 0 }),
    );
    http.expectOne('/api/auth/csrf').flush({});
    http.expectOne(`/api/products/${PRODUCT.id}/status`).flush(
      {
        code: 'PRODUCT_VERSION_CONFLICT',
        message: 'El producto ha sido modificado.',
        status: 409,
        path: `/api/products/${PRODUCT.id}/status`,
        timestamp: '2026-10-23T10:00:00Z',
      },
      { status: 409, statusText: 'Conflict' },
    );

    await expect(result).rejects.toEqual(
      new ProductRequestError('El producto ha sido modificado.', 'PRODUCT_VERSION_CONFLICT', 409),
    );
  });
});
