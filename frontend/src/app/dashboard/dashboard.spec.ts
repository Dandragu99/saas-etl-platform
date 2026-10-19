import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Dashboard } from './dashboard';
import { EtlExecutionApiService } from '../execution-history/etl-execution-api.service';
import { of } from 'rxjs';

describe('Dashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
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

  it('should show honest quick access and the company-scoped history', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    await fixture.whenStable();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Tu actividad, de un vistazo');
    expect(element.textContent).toContain('Módulo en preparación');
    expect(element.querySelector('app-etl-execution-history')).not.toBeNull();
    expect(element.textContent).toContain('No hay ejecuciones para mostrar.');
  });
});
