import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DEFAULT_SETTINGS, MiniQuote } from './mini.models';
import { MiniQuotes } from './mini-quotes';
import { MiniStoreService } from './mini-store.service';

describe('MiniQuotes', () => {
  const quote: MiniQuote = {
    id: 'q1',
    number: 'PRE-001',
    clientId: 'c1',
    clientName: 'Reformas Martínez',
    date: '2026-11-20',
    status: 'SENT',
    taxRate: 21,
    notes: '',
    lines: [],
    subtotal: 100,
    tax: 21,
    total: 121,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiniQuotes],
      providers: [
        provideRouter([]),
        {
          provide: MiniStoreService,
          useValue: { quotes: signal([quote]), settings: signal(DEFAULT_SETTINGS) },
        },
      ],
    }).compileComponents();
  });

  it('filters persisted documents by date', () => {
    const fixture = TestBed.createComponent(MiniQuotes);
    fixture.detectChanges();
    const date = (fixture.nativeElement as HTMLElement).querySelector(
      '.date-filter',
    ) as HTMLInputElement;
    date.value = '2026-11-19';
    date.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay presupuestos');
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('PRE-001');
  });
});
