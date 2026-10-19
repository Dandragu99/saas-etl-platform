import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { AuthRequestError } from './auth.models';
import { AuthSessionService } from './auth-session.service';

describe('AuthSessionService', () => {
  const company = { id: 'c1', name: 'Nexo', defaultCurrency: 'EUR', role: 'OWNER' as const };
  const session = { userId: 'u1', email: 'owner@example.com', activeCompany: company };
  const api = {
    getSession: vi.fn(() => of(session)),
    login: vi.fn(() => of(session)),
    register: vi.fn(() => of({ userId: 'u1', companyId: 'c1' })),
    logout: vi.fn(() => of(undefined)),
    getCompanies: vi.fn(() => of([company])),
    selectCompany: vi.fn(() => of(company)),
  };

  beforeEach(() => {
    Object.values(api).forEach((mock) => mock.mockClear());
    TestBed.configureTestingModule({ providers: [{ provide: AuthApiService, useValue: api }] });
  });

  it('should load and retain the authenticated session', async () => {
    const service = TestBed.inject(AuthSessionService);
    expect(await firstValueFrom(service.loadSession())).toEqual(session);
    expect(service.session()).toEqual(session);
    expect(await firstValueFrom(service.loadSession())).toEqual(session);
    expect(api.getSession).toHaveBeenCalledTimes(1);
  });

  it('should treat an HTTP 401 as an anonymous session', async () => {
    api.getSession.mockReturnValueOnce(
      throwError(() => new AuthRequestError('AUTHENTICATION_REQUIRED', 'Required', 401)),
    );
    const service = TestBed.inject(AuthSessionService);
    expect(await firstValueFrom(service.loadSession())).toBeNull();
    expect(service.authenticated()).toBe(false);
  });
});
