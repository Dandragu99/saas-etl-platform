import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';

import { CsvPreview } from './csv-preview';
import { CsvPreviewApiService } from './csv-preview-api.service';
import {
  CsvRemoveColumnResponse,
  CsvPreviewRequestError,
  CsvPreviewResponse,
} from './csv-preview.models';

const PREVIEW_RESPONSE: CsvPreviewResponse = {
  fileName: 'clientes.csv',
  columns: ['id', 'nombre', 'email'],
  rows: [{ id: '1', nombre: 'Ana', email: 'ana@example.com' }],
  previewRowCount: 1,
  truncated: false,
};

const TRANSFORMED_RESPONSE: CsvRemoveColumnResponse = {
  fileName: 'clientes.csv',
  columns: ['id', 'nombre'],
  rows: [{ id: '1', nombre: 'Ana' }],
  previewRowCount: 1,
  truncated: false,
  transformation: {
    type: 'REMOVE_COLUMN',
    removedColumn: 'email',
  },
};

class CsvPreviewApiServiceStub {
  preview(_file: File): Observable<CsvPreviewResponse> {
    return of(PREVIEW_RESPONSE);
  }

  removeColumn(_file: File, _column: string): Observable<CsvRemoveColumnResponse> {
    return of(TRANSFORMED_RESPONSE);
  }
}

describe('CsvPreview', () => {
  let apiService: CsvPreviewApiServiceStub;

  beforeEach(async () => {
    apiService = new CsvPreviewApiServiceStub();

    await TestBed.configureTestingModule({
      imports: [CsvPreview],
      providers: [{ provide: CsvPreviewApiService, useValue: apiService }],
    }).compileComponents();
  });

  it('should hide column controls until original preview succeeds', async () => {
    const previewSubject = new Subject<CsvPreviewResponse>();
    vi.spyOn(apiService, 'preview').mockReturnValue(previewSubject);
    const fixture = TestBed.createComponent(CsvPreview);

    expect(fixture.nativeElement.querySelector('select')).toBeNull();
    selectFile(fixture.nativeElement as HTMLElement, 'clientes.csv');
    await fixture.whenStable();
    clickButton(fixture.nativeElement as HTMLElement, 'Mostrar vista previa');
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('select')).toBeNull();

    previewSubject.next(PREVIEW_RESPONSE);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('select')).not.toBeNull();
  });

  it('should list exactly the preview columns without selecting one', async () => {
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);

    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    const options = [...select.options].map((option) => option.textContent?.trim());
    expect(options).toEqual(['Selecciona una columna', 'id', 'nombre', 'email']);
    expect(select.value).toBe('');
    expect(findButton(fixture.nativeElement as HTMLElement, 'Eliminar columna').disabled).toBe(true);
  });

  it('should reuse the same file and selected column', async () => {
    const removeSpy = vi.spyOn(apiService, 'removeColumn');
    const fixture = TestBed.createComponent(CsvPreview);
    const file = await loadOriginalPreview(fixture);

    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();

    expect(removeSpy).toHaveBeenCalledWith(file, 'email');
  });

  it('should preserve original preview while transformation is loading', async () => {
    const transformationSubject = new Subject<CsvRemoveColumnResponse>();
    vi.spyOn(apiService, 'removeColumn').mockReturnValue(transformationSubject);
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);

    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('app-csv-preview-table')).toHaveLength(1);
    expect(element.textContent).toContain('Vista previa original');
    expect(element.textContent).toContain('Estamos preparando la vista previa transformada.');
    expect(findButton(element, 'Eliminando columna').disabled).toBe(true);
    expect((element.querySelector('input[type="file"]') as HTMLInputElement).disabled).toBe(false);

    transformationSubject.complete();
  });

  it('should render transformed preview separately and identify removed column', async () => {
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);
    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('app-csv-preview-table')).toHaveLength(2);
    expect(element.textContent).toContain('Vista previa original');
    expect(element.textContent).toContain('Vista previa transformada');
    expect(element.textContent).toContain('Columna eliminada: email');
  });

  it('should show transformed truncation notice', async () => {
    vi.spyOn(apiService, 'removeColumn').mockReturnValue(
      of({ ...TRANSFORMED_RESPONSE, truncated: true }),
    );
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);
    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain(
      'La vista previa muestra únicamente las primeras 20.',
    );
  });

  it('should show transformation error without removing original preview', async () => {
    vi.spyOn(apiService, 'removeColumn').mockReturnValue(
      throwError(
        () =>
          new CsvPreviewRequestError(
            'CSV_COLUMN_NOT_FOUND',
            'La columna indicada no existe.',
            422,
          ),
      ),
    );
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);
    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('app-csv-preview-table')).toHaveLength(1);
    expect(element.textContent).toContain('Vista previa original');
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'La columna indicada no existe.',
    );
  });

  it('should clear only transformed state when column changes', async () => {
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);
    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('app-csv-preview-table')).toHaveLength(2);

    await selectColumn(fixture, 'nombre');
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('app-csv-preview-table')).toHaveLength(1);
    expect(element.textContent).toContain('Vista previa original');
    expect((element.querySelector('select') as HTMLSelectElement).value).toBe('nombre');
  });

  it('should clear every previous state when file changes', async () => {
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);
    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();

    selectFile(fixture.nativeElement as HTMLElement, 'otros-clientes.csv');
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('app-csv-preview-table')).toHaveLength(0);
    expect(element.querySelector('select')).toBeNull();
    expect(element.textContent).toContain('otros-clientes.csv');
    expect(element.textContent).not.toContain('Columna eliminada:');
  });

  it('should cancel an old transformation when column changes', async () => {
    const transformationSubject = new Subject<CsvRemoveColumnResponse>();
    vi.spyOn(apiService, 'removeColumn').mockReturnValue(transformationSubject);
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);
    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();

    await selectColumn(fixture, 'nombre');
    transformationSubject.next(TRANSFORMED_RESPONSE);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('app-csv-preview-table')).toHaveLength(1);
    expect(fixture.nativeElement.textContent).not.toContain('Columna eliminada:');
  });

  it('should cancel an old transformation when file changes', async () => {
    const transformationSubject = new Subject<CsvRemoveColumnResponse>();
    vi.spyOn(apiService, 'removeColumn').mockReturnValue(transformationSubject);
    const fixture = TestBed.createComponent(CsvPreview);
    await loadOriginalPreview(fixture);
    await selectColumn(fixture, 'email');
    clickButton(fixture.nativeElement as HTMLElement, 'Eliminar columna');
    await fixture.whenStable();

    selectFile(fixture.nativeElement as HTMLElement, 'nuevo.csv');
    transformationSubject.next(TRANSFORMED_RESPONSE);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('app-csv-preview-table')).toHaveLength(0);
    expect(fixture.nativeElement.textContent).toContain('nuevo.csv');
    expect(fixture.nativeElement.textContent).not.toContain('Columna eliminada:');
  });

  it('should keep preview errors separate from transformation state', async () => {
    vi.spyOn(apiService, 'preview').mockReturnValue(
      throwError(() => new CsvPreviewRequestError('CSV_MALFORMED', 'El CSV no es válido.', 422)),
    );
    const fixture = TestBed.createComponent(CsvPreview);
    selectFile(fixture.nativeElement as HTMLElement, 'clientes.csv');
    await fixture.whenStable();
    clickButton(fixture.nativeElement as HTMLElement, 'Mostrar vista previa');
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain(
      'El CSV no es válido.',
    );
    expect(fixture.nativeElement.querySelector('select')).toBeNull();
  });
});

async function loadOriginalPreview(fixture: ComponentFixture<CsvPreview>): Promise<File> {
  const file = selectFile(fixture.nativeElement as HTMLElement, 'clientes.csv');
  await fixture.whenStable();
  clickButton(fixture.nativeElement as HTMLElement, 'Mostrar vista previa');
  await fixture.whenStable();
  return file;
}

function selectFile(element: HTMLElement, fileName: string): File {
  const input = element.querySelector('input[type="file"]') as HTMLInputElement;
  const file = new File(['id,nombre,email\n1,Ana,ana@example.com'], fileName, {
    type: 'text/csv',
  });
  const files = {
    0: file,
    length: 1,
    item: (index: number) => (index === 0 ? file : null),
    [Symbol.iterator]: () => [file][Symbol.iterator](),
  } as FileList;
  Object.defineProperty(input, 'files', { configurable: true, value: files });
  input.dispatchEvent(new Event('change'));
  return file;
}

async function selectColumn(fixture: ComponentFixture<CsvPreview>, column: string): Promise<void> {
  const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
  select.value = column;
  select.dispatchEvent(new Event('change'));
  await fixture.whenStable();
}

function findButton(element: HTMLElement, text: string): HTMLButtonElement {
  const button = [...element.querySelectorAll('button')].find((candidate) =>
    candidate.textContent?.includes(text),
  );
  if (button === undefined) {
    throw new Error(`Button not found: ${text}`);
  }
  return button as HTMLButtonElement;
}

function clickButton(element: HTMLElement, text: string): void {
  findButton(element, text).click();
}
