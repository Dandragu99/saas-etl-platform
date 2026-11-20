import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AssistantPage } from './assistant-page';
import { InvoicesPage } from './invoices-page';

describe('electrician demo pages', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

  it('filters the invoice archive by collection status', () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    const select = (fixture.nativeElement as HTMLElement).querySelector(
      'select',
    ) as HTMLSelectElement;
    select.value = 'OVERDUE';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('FAC-2026-0038');
    expect(text).not.toContain('FAC-2026-0041');
  });

  it('answers a suggested assistant question with demo data', () => {
    const fixture = TestBed.createComponent(AssistantPage);
    fixture.detectChanges();
    const button = (fixture.nativeElement as HTMLElement).querySelector(
      '.suggestions button',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('6.875,22 €');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('respuestas preparadas');
  });
});
