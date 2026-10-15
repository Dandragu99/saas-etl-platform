import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';

import { EtlExecutionApiService } from './etl-execution-api.service';
import { EtlExecutionHistory } from './etl-execution-history';
import {
  EtlExecution,
  EtlExecutionPage,
  EtlExecutionRequestError,
  EtlExecutionSummary,
} from './etl-execution.models';

const EXECUTIONS: readonly EtlExecution[] = [
  {
    id: 'success-id',
    type: 'REMOVE_COLUMN',
    status: 'SUCCESS',
    startedAt: '2026-10-09T10:00:00Z',
    finishedAt: '2026-10-09T10:01:05Z',
    processedRecordCount: 25,
    errorMessage: null,
  },
  {
    id: 'failed-id',
    type: 'REMOVE_COLUMN',
    status: 'FAILED',
    startedAt: '2026-10-09T09:00:00Z',
    finishedAt: '2026-10-09T09:00:00.500Z',
    processedRecordCount: null,
    errorMessage: 'La ejecución no pudo completarse.',
  },
  {
    id: 'running-id',
    type: 'REMOVE_COLUMN',
    status: 'RUNNING',
    startedAt: '2026-10-09T08:00:00Z',
    finishedAt: null,
    processedRecordCount: null,
    errorMessage: null,
  },
  {
    id: 'pending-id',
    type: 'REMOVE_COLUMN',
    status: 'PENDING',
    startedAt: '2026-10-09T07:00:00Z',
    finishedAt: null,
    processedRecordCount: null,
    errorMessage: null,
  },
];

class EtlExecutionApiServiceStub {
  getExecutions(): Observable<EtlExecutionPage> {
    return of({ executions: EXECUTIONS, totalElements: 4, page: 0, size: 10, totalPages: 1 });
  }

  getSummary(): Observable<EtlExecutionSummary> {
    return of({ total: 4, successful: 1, failed: 1, inProgress: 2 });
  }
}

describe('EtlExecutionHistory', () => {
  let apiService: EtlExecutionApiServiceStub;

  beforeEach(async () => {
    apiService = new EtlExecutionApiServiceStub();
    await TestBed.configureTestingModule({
      imports: [EtlExecutionHistory],
      providers: [{ provide: EtlExecutionApiService, useValue: apiService }],
    }).compileComponents();
  });

  it('should show loading while the request is pending', async () => {
    const response = new Subject<EtlExecutionPage>();
    vi.spyOn(apiService, 'getExecutions').mockReturnValue(response);

    const fixture = createComponent();

    expect(fixture.nativeElement.textContent).toContain('Cargando historial');
    expect(findRefreshButton(fixture).disabled).toBe(true);
    response.complete();
  });

  it('should show a comprehensible empty state', async () => {
    vi.spyOn(apiService, 'getExecutions').mockReturnValue(
      of({ executions: [], totalElements: 0, page: 0, size: 10, totalPages: 0 }),
    );

    const fixture = createComponent();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('No hay ejecuciones para mostrar.');
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(0);
  });

  it('should render every status and the execution details', async () => {
    const fixture = createComponent();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelectorAll('tbody tr:not(.error-detail)')).toHaveLength(4);
    expect(element.querySelector('[data-status="SUCCESS"]')?.textContent).toContain('Correcta');
    expect(element.querySelector('[data-status="FAILED"]')?.textContent).toContain('Fallida');
    expect(element.querySelector('[data-status="RUNNING"]')?.textContent).toContain('En ejecución');
    expect(element.querySelector('[data-status="PENDING"]')?.textContent).toContain('Pendiente');
    expect(element.querySelector('code')?.getAttribute('title')).toBe('success-id');
    expect(element.textContent).toContain('Eliminar columna');
    expect(element.textContent).toContain('1 min 5 s');
    expect(element.textContent).toContain('25');
    expect(element.textContent).toContain('La ejecución no pudo completarse.');
    expect(element.textContent).toContain('< 1 s');
    expect(element.textContent).toContain('No disponible');
    expect(element.textContent).toContain('Completadas');
    expect(element.textContent).toContain('Fallidas');
    expect(element.textContent).toContain('En curso');
  });

  it('should reload the first page when filtering by status', async () => {
    const getExecutions = vi.spyOn(apiService, 'getExecutions');
    const fixture = createComponent();
    await fixture.whenStable();
    const select = fixture.nativeElement.querySelector(
      '#execution-status-filter',
    ) as HTMLSelectElement;

    select.value = 'FAILED';
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(getExecutions).toHaveBeenLastCalledWith({ status: 'FAILED', page: 0, size: 10 });
  });

  it('should show a safe error state', async () => {
    vi.spyOn(apiService, 'getExecutions').mockReturnValue(
      throwError(
        () =>
          new EtlExecutionRequestError(
            'No se pudo cargar el historial de ejecuciones. Inténtalo de nuevo.',
          ),
      ),
    );

    const fixture = createComponent();
    await fixture.whenStable();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alert.textContent).toContain('No se pudo obtener el historial.');
    expect(alert.textContent).toContain('Inténtalo de nuevo.');
  });

  it('should refresh manually', async () => {
    const getExecutions = vi.spyOn(apiService, 'getExecutions');
    const fixture = createComponent();
    await fixture.whenStable();

    findRefreshButton(fixture).click();
    await fixture.whenStable();

    expect(getExecutions).toHaveBeenCalledTimes(2);
  });

  it('should refresh when the parent changes the request input', async () => {
    const getExecutions = vi.spyOn(apiService, 'getExecutions');
    const fixture = createComponent();
    await fixture.whenStable();

    fixture.componentRef.setInput('refreshRequest', 1);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(getExecutions).toHaveBeenCalledTimes(2);
  });

  it('should cancel a pending request when destroyed', () => {
    let unsubscribed = false;
    vi.spyOn(apiService, 'getExecutions').mockReturnValue(
      new Observable<EtlExecutionPage>(() => () => {
        unsubscribed = true;
      }),
    );
    const fixture = createComponent();

    fixture.destroy();

    expect(unsubscribed).toBe(true);
  });
});

function createComponent(): ComponentFixture<EtlExecutionHistory> {
  const fixture = TestBed.createComponent(EtlExecutionHistory);
  fixture.detectChanges();
  return fixture;
}

function findRefreshButton(fixture: ComponentFixture<EtlExecutionHistory>): HTMLButtonElement {
  const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement | null;
  if (button === null) {
    throw new Error('Refresh button not found.');
  }
  return button;
}
