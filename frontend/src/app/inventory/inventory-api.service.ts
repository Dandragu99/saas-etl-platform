import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, forkJoin, Observable, switchMap, throwError } from 'rxjs';

import { ApiErrorResponse } from '../auth/auth.models';
import {
  InventoryMovement,
  InventoryRequestError,
  RegisterInventoryMovementRequest,
  StockLevel,
} from './inventory.models';

@Injectable({ providedIn: 'root' })
export class InventoryApiService {
  private readonly http = inject(HttpClient);

  getInventory(): Observable<{
    readonly stocks: readonly StockLevel[];
    readonly movements: readonly InventoryMovement[];
  }> {
    return forkJoin({
      stocks: this.http.get<readonly StockLevel[]>('/api/inventory/stocks'),
      movements: this.http.get<readonly InventoryMovement[]>('/api/inventory/movements'),
    }).pipe(catchError((error: unknown) => throwError(() => this.toRequestError(error))));
  }

  registerMovement(request: RegisterInventoryMovementRequest): Observable<InventoryMovement> {
    return this.http.get('/api/auth/csrf').pipe(
      switchMap(() =>
        this.http.post<InventoryMovement>('/api/inventory/movements', request),
      ),
      catchError((error: unknown) => throwError(() => this.toRequestError(error))),
    );
  }

  private toRequestError(error: unknown): InventoryRequestError {
    if (error instanceof InventoryRequestError) return error;
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return new InventoryRequestError(
        'No se pudo conectar con el servidor. Comprueba que el backend está disponible.',
      );
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return new InventoryRequestError(
        'No tienes permiso para realizar esta operación.',
        'COMPANY_PERMISSION_DENIED',
        403,
      );
    }
    if (error instanceof HttpErrorResponse && this.isApiError(error.error)) {
      return new InventoryRequestError(error.error.message, error.error.code, error.error.status);
    }
    return new InventoryRequestError('No se pudo completar la operación. Inténtalo de nuevo.');
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
