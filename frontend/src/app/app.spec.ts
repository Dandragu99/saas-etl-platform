import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { App } from './app';
import { of } from 'rxjs';
import { AuthSessionService } from './auth/auth-session.service';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { EtlExecutionApiService } from './execution-history/etl-execution-api.service';

describe('App', () => {
  const session = {
    userId: 'user-1',
    email: 'owner@example.com',
    activeCompany: {
      id: 'company-1',
      name: 'Nexo',
      defaultCurrency: 'EUR',
      role: 'OWNER' as const,
    },
  };
  const sessions = {
    session: signal(session).asReadonly(),
    loadSession: () => of(session),
    getCompanies: () => of([session.activeCompany]),
    selectCompany: () => of(session.activeCompany),
    logout: () => of(undefined),
  };
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideRouter(routes),
        { provide: AuthSessionService, useValue: sessions },
        {
          provide: EtlExecutionApiService,
          useValue: {
            getExecutions: () =>
              of({ executions: [], totalElements: 0, page: 0, size: 10, totalPages: 0 }),
            getSummary: () => of({ total: 0, successful: 0, failed: 0, inProgress: 0 }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the application layout and dashboard route', async () => {
    const fixture = TestBed.createComponent(App);
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-main-layout')).not.toBeNull();
    expect(compiled.querySelector('app-dashboard')).not.toBeNull();
    expect(compiled.querySelector('app-etl-execution-history')).not.toBeNull();
    expect(compiled.querySelector('h1')?.textContent).toContain('Tu actividad, de un vistazo');
  });
});
