import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { authGuard } from './auth.guard';
import { AuthSession } from './auth.models';
import { AuthSessionService } from './auth-session.service';

describe('authGuard', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/dashboard' } as RouterStateSnapshot;

  it('allows an authenticated session', async () => {
    configure(of({ userId: 'user-1', email: 'ana@example.com', activeCompany: null }));

    const result = await runGuard();

    expect(result).toBe(true);
  });

  it('redirects an anonymous request to login and preserves the requested URL', async () => {
    const router = configure(of(null));
    const result = await runGuard();

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fdashboard');
  });

  function configure(session: Observable<AuthSession | null>): Router {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthSessionService,
          useValue: { loadSession: vi.fn(() => session) },
        },
      ],
    });
    return TestBed.inject(Router);
  }

  function runGuard(): Promise<boolean | UrlTree> {
    return TestBed.runInInjectionContext(() =>
      firstValueFrom(authGuard(route, state) as Observable<boolean | UrlTree>),
    );
  }
});
