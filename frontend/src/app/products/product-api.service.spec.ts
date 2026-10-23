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
      '/api/products?page=0&size=20&sort=price&direction=desc&name=Chair&sku=CH&category=Furniture&active=false',
    );

    expect(request.request.method).toBe('GET');
    request.flush(response);
    await expect(result).resolves.toEqual(response);
  });

  it('should omit empty optional filters', async () => {
    const result = firstValueFrom(
      service.getProducts({
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
      new ProductRequestError('No tienes permiso para consultar los productos.'),
    );
  });
});
