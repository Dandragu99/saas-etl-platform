import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Dashboard } from './dashboard';
import { EtlExecutionApiService } from '../execution-history/etl-execution-api.service';

describe('Dashboard', () => {
  const apiService = {
    getExecutions: vi.fn(() =>
      of({ executions: [], totalElements: 0, page: 0, size: 10, totalPages: 0 }),
    ),
    getSummary: vi.fn(() => of({ total: 0, successful: 0, failed: 0, inProgress: 0 })),
  };

  beforeEach(async () => {
    apiService.getExecutions.mockClear();
    apiService.getSummary.mockClear();
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([]), { provide: EtlExecutionApiService, useValue: apiService }],
    }).compileComponents();
  });

  it('should show honest quick access states and load the real ETL summary once', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    await fixture.whenStable();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Tu actividad, de un vistazo');
    expect(element.textContent).toContain('Módulo en preparación');
    expect(element.querySelector('app-etl-execution-history')).not.toBeNull();
    expect(apiService.getExecutions).toHaveBeenCalledTimes(1);
    expect(apiService.getSummary).toHaveBeenCalledTimes(1);
  });
});
