import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

import { EtlExecution, EtlExecutionRequestError } from './etl-execution.models';

@Injectable({ providedIn: 'root' })
export class EtlExecutionApiService {
  private readonly httpClient = inject(HttpClient);

  getExecutions(): Observable<readonly EtlExecution[]> {
    return this.httpClient
      .get<readonly EtlExecution[]>('/api/etl/executions')
      .pipe(catchError((error: unknown) => throwError(() => this.toRequestError(error))));
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
