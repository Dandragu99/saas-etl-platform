import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, switchMap, throwError } from 'rxjs';

import {
  ApiErrorResponse,
  AuthRequestError,
  AuthSession,
  CompanyAccess,
  LoginRequest,
  RegistrationRequest,
  RegistrationResponse,
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  getSession(): Observable<AuthSession> {
    return this.http
      .get<AuthSession>('/api/auth/session')
      .pipe(catchError((error: unknown) => this.fail(error)));
  }

  login(request: LoginRequest): Observable<AuthSession> {
    return this.withCsrf(this.http.post<AuthSession>('/api/auth/login', request));
  }

  register(request: RegistrationRequest): Observable<RegistrationResponse> {
    return this.withCsrf(this.http.post<RegistrationResponse>('/api/auth/register', request));
  }

  logout(): Observable<void> {
    return this.withCsrf(
      this.http.post('/api/auth/logout', null, { observe: 'response' }).pipe(map(() => undefined)),
    );
  }

  getCompanies(): Observable<readonly CompanyAccess[]> {
    return this.http
      .get<readonly CompanyAccess[]>('/api/auth/companies')
      .pipe(catchError((error: unknown) => this.fail(error)));
  }

  selectCompany(companyId: string): Observable<CompanyAccess> {
    return this.withCsrf(this.http.put<CompanyAccess>('/api/auth/session/company', { companyId }));
  }

  private withCsrf<T>(request: Observable<T>): Observable<T> {
    return this.http.get('/api/auth/csrf').pipe(
      switchMap(() => request),
      catchError((error: unknown) => this.fail(error)),
    );
  }

  private fail(error: unknown): Observable<never> {
    if (error instanceof AuthRequestError) {
      return throwError(() => error);
    }
    if (error instanceof HttpErrorResponse) {
      const body: unknown = error.error;
      if (this.isApiError(body)) {
        return throwError(() => new AuthRequestError(body.code, body.message, body.status));
      }
      if (error.status === 0) {
        return throwError(
          () => new AuthRequestError('NETWORK_ERROR', 'No se pudo conectar con el servidor.', 0),
        );
      }
    }
    return throwError(
      () => new AuthRequestError('INTERNAL_ERROR', 'La operación no pudo completarse.', 500),
    );
  }

  private isApiError(value: unknown): value is ApiErrorResponse {
    if (typeof value !== 'object' || value === null) {
      return false;
    }
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
