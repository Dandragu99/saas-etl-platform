import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, Observable, of, shareReplay, tap, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import {
  AuthRequestError,
  AuthSession,
  CompanyAccess,
  LoginRequest,
  RegistrationRequest,
  RegistrationResponse,
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly api = inject(AuthApiService);
  private readonly sessionState = signal<AuthSession | null>(null);
  private readonly resolvedState = signal(false);
  private loadRequest?: Observable<AuthSession | null>;

  readonly session = this.sessionState.asReadonly();
  readonly authenticated = computed(() => this.sessionState() !== null);

  loadSession(): Observable<AuthSession | null> {
    if (this.resolvedState()) {
      return of(this.sessionState());
    }
    if (this.loadRequest !== undefined) {
      return this.loadRequest;
    }
    this.loadRequest = this.api.getSession().pipe(
      catchError((error: unknown) => {
        if (error instanceof AuthRequestError && error.status === 401) {
          return of(null);
        }
        return throwError(() => error);
      }),
      tap((session) => {
        this.sessionState.set(session);
        this.resolvedState.set(true);
      }),
      finalize(() => {
        this.loadRequest = undefined;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.loadRequest;
  }

  login(request: LoginRequest): Observable<AuthSession> {
    return this.api.login(request).pipe(
      tap((session) => {
        this.sessionState.set(session);
        this.resolvedState.set(true);
      }),
    );
  }

  register(request: RegistrationRequest): Observable<RegistrationResponse> {
    return this.api.register(request);
  }

  logout(): Observable<void> {
    return this.api.logout().pipe(finalize(() => this.clear()));
  }

  getCompanies(): Observable<readonly CompanyAccess[]> {
    return this.api.getCompanies();
  }

  selectCompany(companyId: string): Observable<CompanyAccess> {
    return this.api.selectCompany(companyId).pipe(
      tap((company) => {
        const current = this.sessionState();
        if (current !== null) {
          this.sessionState.set({ ...current, activeCompany: company });
        }
      }),
    );
  }

  clear(): void {
    this.sessionState.set(null);
    this.resolvedState.set(true);
  }
}
