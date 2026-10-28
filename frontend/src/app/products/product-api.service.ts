import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, switchMap, throwError } from 'rxjs';

import { ApiErrorResponse } from '../auth/auth.models';
import {
  ChangeProductStatusRequest,
  CreateProductRequest,
  Product,
  ProductPage,
  ProductQuery,
  ProductRequestError,
  UpdateProductRequest,
} from './product.models';

@Injectable({ providedIn: 'root' })
export class ProductApiService {
  private readonly http = inject(HttpClient);

  getProducts(query: ProductQuery): Observable<ProductPage> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sort', query.sort)
      .set('direction', query.direction);

    params = this.addTextFilter(params, 'search', query.search);
    params = this.addTextFilter(params, 'name', query.name);
    params = this.addTextFilter(params, 'sku', query.sku);
    params = this.addTextFilter(params, 'category', query.category);
    if (query.active !== undefined) {
      params = params.set('active', query.active);
    }

    return this.http
      .get<ProductPage>('/api/products', { params })
      .pipe(catchError((error: unknown) => throwError(() => this.toRequestError(error))));
  }

  getProduct(id: string): Observable<Product> {
    return this.http
      .get<Product>(`/api/products/${encodeURIComponent(id)}`)
      .pipe(catchError((error: unknown) => throwError(() => this.toRequestError(error))));
  }

  createProduct(request: CreateProductRequest): Observable<Product> {
    return this.withCsrf(this.http.post<Product>('/api/products', request));
  }

  updateProduct(id: string, request: UpdateProductRequest): Observable<Product> {
    return this.withCsrf(
      this.http.put<Product>(`/api/products/${encodeURIComponent(id)}`, request),
    );
  }

  changeProductStatus(id: string, request: ChangeProductStatusRequest): Observable<Product> {
    return this.withCsrf(
      this.http.patch<Product>(`/api/products/${encodeURIComponent(id)}/status`, request),
    );
  }

  private withCsrf<T>(request: Observable<T>): Observable<T> {
    return this.http.get('/api/auth/csrf').pipe(
      switchMap(() => request),
      catchError((error: unknown) => throwError(() => this.toRequestError(error))),
    );
  }

  private addTextFilter(params: HttpParams, name: string, value: string | undefined): HttpParams {
    const normalized = value?.trim();
    return normalized ? params.set(name, normalized) : params;
  }

  private toRequestError(error: unknown): ProductRequestError {
    if (error instanceof ProductRequestError) {
      return error;
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return new ProductRequestError(
        'No se pudo conectar con el servidor. Comprueba que el backend está disponible.',
      );
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return new ProductRequestError(
        'No tienes permiso para realizar esta operación.',
        'COMPANY_PERMISSION_DENIED',
        403,
      );
    }
    if (error instanceof HttpErrorResponse && this.isApiError(error.error)) {
      return new ProductRequestError(error.error.message, error.error.code, error.error.status);
    }
    return new ProductRequestError(
      'No se pudo completar la operación. Inténtalo de nuevo.',
      'PRODUCT_REQUEST_FAILED',
      error instanceof HttpErrorResponse ? error.status : 500,
    );
  }

  private isApiError(value: unknown): value is ApiErrorResponse {
    if (typeof value !== 'object' || value === null) return false;
    const record = value as Record<string, unknown>;
    return (
      typeof record['code'] === 'string' &&
      typeof record['message'] === 'string' &&
      typeof record['status'] === 'number' &&
      typeof record['path'] === 'string' &&
      typeof record['timestamp'] === 'string'
    );
  }
}
