import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { CsvPreviewApiService } from './csv-preview-api.service';
import {
  ApiErrorResponse,
  CsvRemoveColumnResponse,
  CsvPreviewRequestError,
  CsvPreviewResponse,
} from './csv-preview.models';

describe('CsvPreviewApiService', () => {
  let service: CsvPreviewApiService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CsvPreviewApiService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should upload the file using the relative preview endpoint', async () => {
    const file = new File(['id,name\n1,Ana'], 'clientes.csv', { type: 'text/csv' });
    const expectedResponse: CsvPreviewResponse = {
      fileName: 'clientes.csv',
      columns: ['id', 'name'],
      rows: [{ id: '1', name: 'Ana' }],
      previewRowCount: 1,
      truncated: false,
    };

    const responsePromise = firstValueFrom(service.preview(file));
    const request = httpTestingController.expectOne('/api/csv/preview');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeInstanceOf(FormData);
    const uploadedFile = (request.request.body as FormData).get('file');
    expect(uploadedFile).toBeInstanceOf(File);
    expect((uploadedFile as File).name).toBe('clientes.csv');
    expect((uploadedFile as File).size).toBe(file.size);
    expect((uploadedFile as File).type).toBe('text/csv');
    expect(request.request.headers.has('Content-Type')).toBe(false);

    request.flush(expectedResponse);

    await expect(responsePromise).resolves.toEqual(expectedResponse);
  });

  it('should expose the safe API error message', async () => {
    const file = new File([''], 'empty.csv', { type: 'text/csv' });
    const apiError: ApiErrorResponse = {
      code: 'CSV_FILE_EMPTY',
      message: 'El archivo CSV está vacío.',
      status: 400,
      path: '/api/csv/preview',
      timestamp: '2026-07-21T17:45:00Z',
    };

    const responsePromise = firstValueFrom(service.preview(file));
    const request = httpTestingController.expectOne('/api/csv/preview');
    request.flush(apiError, { status: 400, statusText: 'Bad Request' });

    await expect(responsePromise).rejects.toEqual(
      new CsvPreviewRequestError('CSV_FILE_EMPTY', 'El archivo CSV está vacío.', 400),
    );
  });

  it('should send the same file and literal column to the remove-column endpoint', async () => {
    const file = new File(['id,email\n1,ana@example.com'], 'clientes.csv', {
      type: 'text/csv',
    });
    const expectedResponse: CsvRemoveColumnResponse = {
      fileName: 'clientes.csv',
      columns: ['id'],
      rows: [{ id: '1' }],
      previewRowCount: 1,
      truncated: false,
      transformation: {
        type: 'REMOVE_COLUMN',
        removedColumn: 'email',
      },
    };

    const responsePromise = firstValueFrom(service.removeColumn(file, 'email'));
    const request = httpTestingController.expectOne('/api/csv/transform/remove-column');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeInstanceOf(FormData);
    const formData = request.request.body as FormData;
    const uploadedFile = formData.get('file');
    expect(uploadedFile).toBeInstanceOf(File);
    expect((uploadedFile as File).name).toBe(file.name);
    expect((uploadedFile as File).size).toBe(file.size);
    expect((uploadedFile as File).type).toBe(file.type);
    expect(formData.get('column')).toBe('email');
    expect(request.request.headers.has('Content-Type')).toBe(false);

    request.flush(expectedResponse);

    await expect(responsePromise).resolves.toEqual(expectedResponse);
  });

  it('should expose a safe remove-column API error', async () => {
    const file = new File(['id,email'], 'clientes.csv', { type: 'text/csv' });
    const apiError: ApiErrorResponse = {
      code: 'CSV_COLUMN_NOT_FOUND',
      message: 'La columna indicada no existe en el archivo CSV.',
      status: 422,
      path: '/api/csv/transform/remove-column',
      timestamp: '2026-07-22T17:45:00Z',
    };

    const responsePromise = firstValueFrom(service.removeColumn(file, 'missing'));
    const request = httpTestingController.expectOne('/api/csv/transform/remove-column');
    request.flush(apiError, { status: 422, statusText: 'Unprocessable Entity' });

    await expect(responsePromise).rejects.toEqual(
      new CsvPreviewRequestError(
        'CSV_COLUMN_NOT_FOUND',
        'La columna indicada no existe en el archivo CSV.',
        422,
      ),
    );
  });

  it('should hide an unknown technical response', async () => {
    const file = new File(['content'], 'clientes.csv', { type: 'text/csv' });

    const responsePromise = firstValueFrom(service.preview(file));
    const request = httpTestingController.expectOne('/api/csv/preview');
    request.flush('Sensitive technical response', {
      status: 500,
      statusText: 'Internal Server Error',
    });

    await expect(responsePromise).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
      message: 'No se pudo procesar el archivo. Inténtalo de nuevo.',
      status: 500,
    });
  });

  it('should return a comprehensible message for a network error', async () => {
    const file = new File(['content'], 'clientes.csv', { type: 'text/csv' });

    const responsePromise = firstValueFrom(service.preview(file));
    const request = httpTestingController.expectOne('/api/csv/preview');
    request.error(new ProgressEvent('network error'));

    await expect(responsePromise).rejects.toMatchObject({
      code: 'NETWORK_ERROR',
      status: 0,
    });
  });
});
