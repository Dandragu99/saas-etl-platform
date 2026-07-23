import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, from, map, mergeMap, Observable, of, throwError } from 'rxjs';

import {
  ApiErrorResponse,
  CsvDownloadResult,
  CsvRemoveColumnResponse,
  CsvPreviewRequestError,
  CsvPreviewResponse,
} from './csv-preview.models';

@Injectable({ providedIn: 'root' })
export class CsvPreviewApiService {
  private static readonly DOWNLOAD_FALLBACK_FILE_NAME = 'transformed.csv';

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

  downloadRemovedColumn(file: File, column: string): Observable<CsvDownloadResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('column', column);

    return this.httpClient
      .post('/api/csv/transform/remove-column/download', formData, {
        observe: 'response',
        responseType: 'blob',
      })
      .pipe(
        map((response) => {
          if (response.body === null) {
            throw new CsvPreviewRequestError(
              'INTERNAL_ERROR',
              'No se pudo descargar el archivo. Inténtalo de nuevo.',
              500,
            );
          }

          return {
            blob: response.body,
            fileName: this.extractDownloadFileName(
              response.headers.get('Content-Disposition'),
            ),
          };
        }),
        catchError((error: unknown) => this.toDownloadRequestError(error)),
      );
  }

  private toDownloadRequestError(error: unknown): Observable<never> {
    if (!(error instanceof HttpErrorResponse) || error.status === 0) {
      return throwError(() => this.toRequestError(error));
    }

    if (!(error.error instanceof Blob)) {
      return throwError(() => this.toRequestError(error));
    }

    return from(error.error.text()).pipe(
      map((body) => this.parseApiErrorResponse(body)),
      catchError(() => of(null)),
      mergeMap((requestError) =>
        throwError(() => requestError ?? this.toRequestError(error)),
      ),
    );
  }

  private parseApiErrorResponse(body: string): CsvPreviewRequestError | null {
    let parsedBody: unknown;
    try {
      parsedBody = JSON.parse(body) as unknown;
    } catch {
      return null;
    }

    if (!this.isApiErrorResponse(parsedBody)) {
      return null;
    }

    return new CsvPreviewRequestError(
      parsedBody.code,
      parsedBody.message,
      parsedBody.status,
    );
  }

  private extractDownloadFileName(contentDisposition: string | null): string {
    if (contentDisposition === null) {
      return CsvPreviewApiService.DOWNLOAD_FALLBACK_FILE_NAME;
    }

    const extendedMatch =
      /(?:^|;)\s*filename\*\s*=\s*(?:"([^"]*)"|([^;]*))/i.exec(contentDisposition);
    if (extendedMatch !== null) {
      const extendedValue = (extendedMatch[1] ?? extendedMatch[2] ?? '').trim();
      const encodingPrefix = /^UTF-8''/i.exec(extendedValue);
      if (encodingPrefix === null) {
        return CsvPreviewApiService.DOWNLOAD_FALLBACK_FILE_NAME;
      }

      try {
        const decoded = decodeURIComponent(extendedValue.slice(encodingPrefix[0].length));
        return (
          this.sanitizeDownloadFileName(decoded) ??
          CsvPreviewApiService.DOWNLOAD_FALLBACK_FILE_NAME
        );
      } catch {
        return CsvPreviewApiService.DOWNLOAD_FALLBACK_FILE_NAME;
      }
    }

    const quotedMatch =
      /(?:^|;)\s*filename\s*=\s*"((?:\\.|[^"])*)"/i.exec(contentDisposition);
    const unquotedMatch =
      /(?:^|;)\s*filename\s*=\s*([^;]*)/i.exec(contentDisposition);
    const rawFileName =
      quotedMatch?.[1]?.replace(/\\(["\\])/g, '$1') ?? unquotedMatch?.[1]?.trim();

    return (
      this.sanitizeDownloadFileName(rawFileName) ??
      CsvPreviewApiService.DOWNLOAD_FALLBACK_FILE_NAME
    );
  }

  private sanitizeDownloadFileName(value: string | undefined): string | null {
    if (value === undefined) {
      return null;
    }

    const fileName = value.trim();
    if (
      fileName.length === 0 ||
      fileName === '.' ||
      fileName === '..' ||
      /[\/\\]/.test(fileName) ||
      /[\u0000-\u001f\u007f]/.test(fileName) ||
      /[<>:"|?*]/.test(fileName)
    ) {
      return null;
    }

    return fileName;
  }

  private toRequestError(error: unknown): CsvPreviewRequestError {
    if (error instanceof CsvPreviewRequestError) {
      return error;
    }

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
