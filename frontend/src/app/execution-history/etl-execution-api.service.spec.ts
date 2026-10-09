import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { EtlExecutionApiService } from './etl-execution-api.service';
import { EtlExecution, EtlExecutionRequestError } from './etl-execution.models';

describe('EtlExecutionApiService', () => {
  let service: EtlExecutionApiService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(EtlExecutionApiService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should request the execution history using the real backend field names', async () => {
    const expected: readonly EtlExecution[] = [
      {
        id: 'ca5b9df0-28ae-4cd1-859a-075a28297d97',
        type: 'REMOVE_COLUMN',
        status: 'SUCCESS',
        startedAt: '2026-10-09T10:00:00Z',
        finishedAt: '2026-10-09T10:00:01Z',
        processedRecordCount: 25,
        errorMessage: null,
      },
    ];

    const responsePromise = firstValueFrom(service.getExecutions());
    const request = httpTestingController.expectOne('/api/etl/executions');

    expect(request.request.method).toBe('GET');
    request.flush(expected);

    await expect(responsePromise).resolves.toEqual(expected);
  });

  it('should return a safe network error', async () => {
    const responsePromise = firstValueFrom(service.getExecutions());
    const request = httpTestingController.expectOne('/api/etl/executions');
    request.error(new ProgressEvent('network error'));

    await expect(responsePromise).rejects.toEqual(
      new EtlExecutionRequestError(
        'No se pudo conectar con el servidor. Comprueba que el backend está disponible.',
      ),
    );
  });

  it('should hide technical server responses', async () => {
    const responsePromise = firstValueFrom(service.getExecutions());
    const request = httpTestingController.expectOne('/api/etl/executions');
    request.flush('Sensitive stack trace', {
      status: 500,
      statusText: 'Internal Server Error',
    });

    await expect(responsePromise).rejects.toEqual(
      new EtlExecutionRequestError(
        'No se pudo cargar el historial de ejecuciones. Inténtalo de nuevo.',
      ),
    );
  });
});
