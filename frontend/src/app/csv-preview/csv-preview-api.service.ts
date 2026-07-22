import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

import {
  ApiErrorResponse,
  CsvRemoveColumnResponse,
  CsvPreviewRequestError,
  CsvPreviewResponse,
} from './csv-preview.models';

@Injectable({ providedIn: 'root' })
export class CsvPreviewApiService {
  private readonly httpClient = inject(HttpClient);

  preview(file: File): Observable<CsvPreviewResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);

    return this.httpClient
      .post<CsvPreviewResponse>('/api/csv/preview', formData)
      .pipe(catchError((error: unknown) => throwError(() => this.toRequestError(error))));
  }

  removeColumn(file: File, column: string): Observable<CsvRemoveColumnResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('column', column);

    return this.httpClient
      .post<CsvRemoveColumnResponse>('/api/csv/transform/remove-column', formData)
      .pipe(catchError((error: unknown) => throwError(() => this.toRequestError(error))));
  }

  private toRequestError(error: unknown): CsvPreviewRequestError {
    if (error instanceof HttpErrorResponse) {
      const responseBody: unknown = error.error;
      if (this.isApiErrorResponse(responseBody)) {
        return new CsvPreviewRequestError(
          responseBody.code,
          responseBody.message,
          responseBody.status,
        );
      }

      if (error.status === 0) {
        return new CsvPreviewRequestError(
          'NETWORK_ERROR',
          'No se pudo conectar con el servidor. Comprueba que el backend está disponible.',
          0,
        );
      }
    }

    return new CsvPreviewRequestError(
      'INTERNAL_ERROR',
      'No se pudo procesar el archivo. Inténtalo de nuevo.',
      500,
    );
  }

  private isApiErrorResponse(value: unknown): value is ApiErrorResponse {
    if (!this.isRecord(value)) {
      return false;
    }

    return (
      typeof value['code'] === 'string' &&
      typeof value['message'] === 'string' &&
      typeof value['status'] === 'number' &&
      typeof value['path'] === 'string' &&
      typeof value['timestamp'] === 'string'
    );
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
  }
}
