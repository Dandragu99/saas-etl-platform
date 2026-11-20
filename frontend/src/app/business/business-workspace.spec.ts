import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuthSessionService } from '../auth/auth-session.service';
import { BusinessApiService } from './business-api.service';
import { BusinessWorkspace } from './business-workspace';

describe('BusinessWorkspace', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BusinessWorkspace],
      providers: [
        {
          provide: AuthSessionService,
          useValue: { session: signal({ activeCompany: { role: 'OWNER' } }).asReadonly() },
        },
        {
          provide: BusinessApiService,
          useValue: {
            loadWorkspace: () => of({
              dashboard: { customers: 1, activeWorks: 1, quotes: 0, invoices: 0, pendingRevenue: 0 },
              customers: [{ id: '1', name: 'Instalaciones Demo' }],
              quotes: [], invoices: [],
            }),
          },
        },
      ],
    }).compileComponents();
  });

  it('renders the complete commercial flow and dashboard', () => {
    const fixture = TestBed.createComponent(BusinessWorkspace);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Crear cliente');
    expect(text).toContain('Preparar presupuesto');
    expect(text).toContain('Descargar PDF');
    expect(text).toContain('Instalaciones Demo');
  });
});
