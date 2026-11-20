import { TestBed } from '@angular/core/testing';
import { DEFAULT_SETTINGS, MiniQuote } from './mini.models';
import { MiniPdfService } from './mini-pdf.service';

describe('MiniPdfService', () => {
  it('creates a local PDF containing the demo disclaimer', async () => {
    const quote: MiniQuote = {
      id: 'q1',
      number: 'PRE-001',
      clientId: 'c1',
      clientName: 'Cliente',
      date: '2026-11-20',
      status: 'DRAFT',
      taxRate: 21,
      notes: '',
      lines: [{ description: 'Instalación', kind: 'LABOR', quantity: 2, unitPrice: 50 }],
      subtotal: 100,
      tax: 21,
      total: 121,
    };
    const blob = TestBed.inject(MiniPdfService).create(quote, DEFAULT_SETTINGS);
    const content = await blob.text();
    expect(blob.type).toBe('application/pdf');
    expect(content.startsWith('%PDF-1.4')).toBe(true);
    expect(content).toContain('NO VALIDO PARA EMISION FISCAL');
    expect(content).toContain('PRE-001');
  });
});
