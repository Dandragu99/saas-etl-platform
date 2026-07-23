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
  CsvDownloadResult,
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

  it('should request the complete transformed CSV with the same file and column', async () => {
    const file = new File(['id,email\n1,ana@example.com'], 'clientes.csv', {
      type: 'text/csv',
    });
    const responseBlob = new Blob(['id\n1\n'], { type: 'text/csv;charset=UTF-8' });

    const responsePromise = firstValueFrom(service.downloadRemovedColumn(file, 'email'));
    const request = httpTestingController.expectOne(
      '/api/csv/transform/remove-column/download',
    );

    expect(request.request.method).toBe('POST');
    expect(request.request.responseType).toBe('blob');
    expect(request.request.body).toBeInstanceOf(FormData);
    const formData = request.request.body as FormData;
    expect(formData.get('file')).toBe(file);
    expect(formData.get('column')).toBe('email');
    expect(request.request.headers.has('Content-Type')).toBe(false);

    request.flush(responseBlob, {
      headers: {
        'Content-Disposition': 'attachment; filename="clientes-sin-email.csv"',
      },
    });

    const result = await responsePromise;
    expect(result.blob).toBe(responseBlob);
    expect(result.fileName).toBe('clientes-sin-email.csv');
  });

  it('should decode UTF-8 filename* and prefer it over filename', async () => {
    const responsePromise = downloadWithHeader(
      service,
      httpTestingController,
      `attachment; filename="fallback.csv"; filename*=UTF-8''clientes-sin-correo%20electr%C3%B3nico.csv`,
    );

    await expect(responsePromise).resolves.toMatchObject({
      fileName: 'clientes-sin-correo electrónico.csv',
    });
  });

  it('should preserve plus signs in filename*', async () => {
    const responsePromise = downloadWithHeader(
      service,
      httpTestingController,
      `attachment; filename*=UTF-8''clientes+archivo.csv`,
    );

    await expect(responsePromise).resolves.toMatchObject({
      fileName: 'clientes+archivo.csv',
    });
  });

  it('should use the safe fallback when the filename is missing or invalid', async () => {
    const headers = [
      null,
      `attachment; filename*=UTF-8''..%2Fsecret.csv`,
      `attachment; filename*=UTF-8''control%00.csv`,
      `attachment; filename*=UTF-8''invalid%ZZ.csv`,
      'attachment; filename=".."',
    ];

    for (const header of headers) {
      const responsePromise = downloadWithHeader(service, httpTestingController, header);
      await expect(responsePromise).resolves.toMatchObject({
        fileName: 'transformed.csv',
      });
    }
  });

  it('should convert a JSON API error received as Blob', async () => {
    const file = new File(['id,email'], 'clientes.csv', { type: 'text/csv' });
    const apiError: ApiErrorResponse = {
      code: 'CSV_COLUMN_NOT_FOUND',
      message: 'La columna indicada no existe en el archivo CSV.',
      status: 422,
      path: '/api/csv/transform/remove-column/download',
      timestamp: '2026-07-23T08:00:00Z',
    };

    const responsePromise = firstValueFrom(service.downloadRemovedColumn(file, 'missing'));
    const request = httpTestingController.expectOne(
      '/api/csv/transform/remove-column/download',
    );
    request.flush(new Blob([JSON.stringify(apiError)], { type: 'application/json' }), {
      status: 422,
      statusText: 'Unprocessable Entity',
    });

    await expect(responsePromise).rejects.toEqual(
      new CsvPreviewRequestError(
        'CSV_COLUMN_NOT_FOUND',
        'La columna indicada no existe en el archivo CSV.',
        422,
      ),
    );
  });

  it('should hide technical Blob error content', async () => {
    const file = new File(['id,email'], 'clientes.csv', { type: 'text/csv' });

    const responsePromise = firstValueFrom(service.downloadRemovedColumn(file, 'email'));
    const request = httpTestingController.expectOne(
      '/api/csv/transform/remove-column/download',
    );
    request.flush(new Blob(['Sensitive stack trace'], { type: 'text/plain' }), {
      status: 500,
      statusText: 'Internal Server Error',
    });

    await expect(responsePromise).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
      message: 'No se pudo procesar el archivo. Inténtalo de nuevo.',
      status: 500,
    });
  });

  it('should return a safe network error for a download', async () => {
    const file = new File(['id,email'], 'clientes.csv', { type: 'text/csv' });

    const responsePromise = firstValueFrom(service.downloadRemovedColumn(file, 'email'));
    const request = httpTestingController.expectOne(
      '/api/csv/transform/remove-column/download',
    );
    request.error(new ProgressEvent('network error'));

    await expect(responsePromise).rejects.toMatchObject({
      code: 'NETWORK_ERROR',
      status: 0,
    });
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

function downloadWithHeader(
  service: CsvPreviewApiService,
  httpTestingController: HttpTestingController,
  contentDisposition: string | null,
): Promise<CsvDownloadResult> {
  const file = new File(['id,email\n1,ana@example.com'], 'clientes.csv', {
    type: 'text/csv',
  });
  const responsePromise = firstValueFrom(service.downloadRemovedColumn(file, 'email'));
  const request = httpTestingController.expectOne(
    '/api/csv/transform/remove-column/download',
  );
  const headers =
    contentDisposition === null ? undefined : { 'Content-Disposition': contentDisposition };
  request.flush(new Blob(['id\n1\n'], { type: 'text/csv' }), { headers });
  return responsePromise;
}
