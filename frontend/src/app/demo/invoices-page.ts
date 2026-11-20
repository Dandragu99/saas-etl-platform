import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DEMO_INVOICES, DemoInvoice, money } from './demo-data';

@Component({
  selector: 'app-invoices-page',
  imports: [RouterLink],
  templateUrl: './invoices-page.html',
  styleUrl: './invoices-page.css',
})
export class InvoicesPage {
  protected readonly query = signal('');
  protected readonly filter = signal<'ALL' | 'PAID' | 'PENDING' | 'OVERDUE'>('ALL');
  protected readonly selected = signal<DemoInvoice | null>(null);
  protected readonly money = money;
  protected readonly invoices = computed(() => {
    const q = this.query().toLocaleLowerCase('es').trim();
    return DEMO_INVOICES.filter(
      (i) =>
        (this.filter() === 'ALL' || i.status === this.filter()) &&
        (!q || `${i.number} ${i.client}`.toLocaleLowerCase('es').includes(q)),
    );
  });
  protected search(e: Event): void {
    this.query.set((e.target as HTMLInputElement).value);
  }
  protected setFilter(e: Event): void {
    this.filter.set(
      (e.target as HTMLSelectElement).value as 'ALL' | 'PAID' | 'PENDING' | 'OVERDUE',
    );
  }
  protected label(status: string): string {
    return ({ PAID: 'Pagada', PENDING: 'Pendiente', OVERDUE: 'Vencida' } as Record<string, string>)[
      status
    ];
  }
}
