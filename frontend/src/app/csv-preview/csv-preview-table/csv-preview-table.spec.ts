import { TestBed } from '@angular/core/testing';

import { CsvTabularPreview } from '../csv-preview.models';
import { CsvPreviewTable } from './csv-preview-table';

const PREVIEW: CsvTabularPreview = {
  fileName: 'clientes.csv',
  columns: ['id', 'nombre', 'email'],
  rows: [{ email: 'ana@example.com', id: '1', nombre: 'Ana' }],
  previewRowCount: 1,
  truncated: false,
};

describe('CsvPreviewTable', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CsvPreviewTable] }).compileComponents();
  });

  it('should render cells following column order', async () => {
    const fixture = TestBed.createComponent(CsvPreviewTable);
    fixture.componentRef.setInput('preview', PREVIEW);
    fixture.componentRef.setInput('label', 'Vista previa original');
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    const headers = [...element.querySelectorAll('th')].map((item) => item.textContent?.trim());
    const cells = [...element.querySelectorAll('tbody td')].map((item) => item.textContent?.trim());
    expect(headers).toEqual(['id', 'nombre', 'email']);
    expect(cells).toEqual(['1', 'Ana', 'ana@example.com']);
    expect(element.textContent).toContain('Vista previa original');
    expect(element.textContent).toContain('1 fila mostrada');
  });

  it('should render a header-only preview', async () => {
    const fixture = TestBed.createComponent(CsvPreviewTable);
    fixture.componentRef.setInput('preview', {
      ...PREVIEW,
      rows: [],
      previewRowCount: 0,
    });
    fixture.componentRef.setInput('label', 'Vista previa transformada');
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain(
      'El archivo contiene encabezados, pero no filas de datos.',
    );
    expect(fixture.nativeElement.textContent).toContain('0 filas mostradas');
  });

  it('should show truncation notice only when required', async () => {
    const fixture = TestBed.createComponent(CsvPreviewTable);
    fixture.componentRef.setInput('preview', { ...PREVIEW, truncated: true });
    fixture.componentRef.setInput('label', 'Vista previa transformada');
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain(
      'La vista previa muestra únicamente las primeras 20.',
    );

    fixture.componentRef.setInput('preview', PREVIEW);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).not.toContain(
      'La vista previa muestra únicamente las primeras 20.',
    );
  });
});
