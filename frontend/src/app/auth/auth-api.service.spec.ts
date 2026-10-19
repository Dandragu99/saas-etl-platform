import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthApiService } from './auth-api.service';

describe('AuthApiService', () => {
  let service: AuthApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should bootstrap CSRF before sending login credentials', () => {
    const session = { userId: 'u1', email: 'owner@example.com', activeCompany: null };
    service
      .login({ email: 'owner@example.com', password: 'a secure password' })
      .subscribe((value) => {
        expect(value).toEqual(session);
      });

    http.expectOne('/api/auth/csrf').flush({ headerName: 'X-XSRF-TOKEN' });
    const login = http.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual({
      email: 'owner@example.com',
      password: 'a secure password',
    });
    login.flush(session);
  });

  it('should bootstrap CSRF before registration and company selection', () => {
    service
      .register({
        email: 'owner@example.com',
        password: 'a secure password',
        companyName: 'Nexo',
        defaultCurrency: 'EUR',
      })
      .subscribe();
    http.expectOne('/api/auth/csrf').flush({});
    http.expectOne('/api/auth/register').flush({ userId: 'u1', companyId: 'c1' });

    service.selectCompany('c2').subscribe();
    http.expectOne('/api/auth/csrf').flush({});
    const selection = http.expectOne('/api/auth/session/company');
    expect(selection.request.body).toEqual({ companyId: 'c2' });
    selection.flush({ id: 'c2', name: 'Second', defaultCurrency: 'EUR', role: 'VIEWER' });
  });
});
