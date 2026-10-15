import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';

import {
  EtlExecution,
  EtlExecutionPage,
  EtlExecutionQuery,
  EtlExecutionRequestError,
  EtlExecutionSummary,
} from './etl-execution.models';

@Injectable({ providedIn: 'root' })
export class EtlExecutionApiService {
  private readonly httpClient = inject(HttpClient);

  getExecutions(query?: EtlExecutionQuery): Observable<EtlExecutionPage> {
    let params = new HttpParams();
    if (query !== undefined) {
      params = params.set('page', query.page).set('size', query.size);
      if (query.status !== undefined) {
        params = params.set('status', query.status);
      }
    }

    return this.httpClient
      .get<readonly EtlExecution[]>('/api/etl/executions', { params, observe: 'response' })
      .pipe(
        map((response) => {
          const executions = response.body ?? [];
          return {
            executions,
            totalElements: this.headerNumber(
              response.headers.get('X-Total-Count'),
              executions.length,
            ),
            page: this.headerNumber(response.headers.get('X-Page'), query?.page ?? 0),
            size: this.headerNumber(response.headers.get('X-Page-Size'), query?.size ?? 100),
            totalPages: this.headerNumber(
              response.headers.get('X-Total-Pages'),
              executions.length === 0 ? 0 : 1,
            ),
          };
        }),
        catchError((error: unknown) => throwError(() => this.toRequestError(error))),
      );
  }

  getSummary(): Observable<EtlExecutionSummary> {
    return this.httpClient
      .get<EtlExecutionSummary>('/api/etl/executions/summary')
      .pipe(catchError((error: unknown) => throwError(() => this.toRequestError(error))));
  }

  private headerNumber(value: string | null, fallback: number): number {
    if (value === null) {
      return fallback;
    }
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
  }

  private toRequestError(error: unknown): EtlExecutionRequestError {
    if (error instanceof EtlExecutionRequestError) {
      return error;
    }

    if (error instanceof HttpErrorResponse && error.status === 0) {
      return new EtlExecutionRequestError(
        'No se pudo conectar con el servidor. Comprueba que el backend está disponible.',
      );
    }

    return new EtlExecutionRequestError(
      'No se pudo cargar el historial de ejecuciones. Inténtalo de nuevo.',
    );
  }
}
