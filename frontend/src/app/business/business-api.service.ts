import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, forkJoin, Observable, switchMap, throwError } from 'rxjs';

import { BusinessDashboard, Customer, CustomerInput, Invoice, Quote, QuoteInput } from './business.models';

export class BusinessRequestError extends Error {}

@Injectable({ providedIn: 'root' })
export class BusinessApiService {
  private readonly http = inject(HttpClient);

  loadWorkspace() {
    return forkJoin({
      dashboard: this.http.get<BusinessDashboard>('/api/business/dashboard'),
      customers: this.http.get<readonly Customer[]>('/api/business/customers'),
      quotes: this.http.get<readonly Quote[]>('/api/business/quotes'),
      invoices: this.http.get<readonly Invoice[]>('/api/business/invoices'),
    }).pipe(catchError((error: unknown) => throwError(() => this.toError(error))));
  }

  createCustomer(input: CustomerInput): Observable<Customer> {
    return this.write(this.http.post<Customer>('/api/business/customers', input));
  }

  updateCustomer(id: string, input: CustomerInput): Observable<Customer> {
    return this.write(this.http.put<Customer>(`/api/business/customers/${encodeURIComponent(id)}`, input));
  }

  createQuote(input: QuoteInput): Observable<Quote> {
    return this.write(this.http.post<Quote>('/api/business/quotes', input));
  }

  convertQuote(id: string): Observable<Invoice> {
    return this.write(this.http.post<Invoice>(`/api/business/quotes/${encodeURIComponent(id)}/invoice`, null));
  }

  seedDemo(): Observable<void> {
    return this.write(this.http.post<void>('/api/business/demo', null));
  }

  download(kind: 'quotes' | 'invoices', id: string): Observable<Blob> {
    return this.http.get(`/api/business/${kind}/${encodeURIComponent(id)}/pdf`, {
      responseType: 'blob',
    }).pipe(catchError((error: unknown) => throwError(() => this.toError(error))));
  }

  private write<T>(request: Observable<T>): Observable<T> {
    return this.http.get('/api/auth/csrf').pipe(
      switchMap(() => request),
      catchError((error: unknown) => throwError(() => this.toError(error))),
    );
  }

  private toError(error: unknown): BusinessRequestError {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return new BusinessRequestError('No se pudo conectar con el servidor.');
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return new BusinessRequestError('No tienes permiso para realizar esta operación.');
    }
    return new BusinessRequestError('No se pudo completar la operación. Revisa los datos e inténtalo de nuevo.');
  }
}
