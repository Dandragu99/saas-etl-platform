import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

import { Product, ProductPage, ProductQuery, ProductRequestError } from './product.models';

@Injectable({ providedIn: 'root' })
export class ProductApiService {
  private readonly http = inject(HttpClient);

  getProducts(query: ProductQuery): Observable<ProductPage> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sort', query.sort)
      .set('direction', query.direction);

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
      return new ProductRequestError('No tienes permiso para consultar los productos.');
    }
    return new ProductRequestError('No se pudieron cargar los productos. Inténtalo de nuevo.');
  }
}
