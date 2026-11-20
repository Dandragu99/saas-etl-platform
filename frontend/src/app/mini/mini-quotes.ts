import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MiniPdfService } from './mini-pdf.service';
import { MiniQuote, miniMoney } from './mini.models';
import { MiniStoreService } from './mini-store.service';

@Component({
  selector: 'app-mini-quotes',
  imports: [RouterLink],
  templateUrl: './mini-quotes.html',
  styleUrl: './mini-quotes.css',
})
export class MiniQuotes {
  protected readonly store = inject(MiniStoreService);
  private readonly pdf = inject(MiniPdfService);
  protected readonly selected = signal<MiniQuote | null>(null);
  protected readonly query = signal('');
  protected readonly status = signal('ALL');
  protected readonly date = signal('');
  protected readonly money = miniMoney;
  protected filtered(): readonly MiniQuote[] {
    const q = this.query().trim().toLocaleLowerCase('es');
    return this.store
      .quotes()
      .filter(
        (item) =>
          (this.status() === 'ALL' || item.status === this.status()) &&
          (!this.date() || item.date === this.date()) &&
          (!q || item.clientName.toLocaleLowerCase('es').includes(q)),
      );
  }
  protected search(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }
  protected filter(event: Event): void {
    this.status.set((event.target as HTMLSelectElement).value);
  }
  protected filterDate(event: Event): void {
    this.date.set((event.target as HTMLInputElement).value);
  }
  protected download(quote: MiniQuote): void {
    this.pdf.download(quote, this.store.settings());
  }
  protected share(quote: MiniQuote): void {
    void this.pdf.share(quote, this.store.settings());
  }
  protected label(status: string): string {
    return ({ DRAFT: 'Borrador', SENT: 'Enviado', ACCEPTED: 'Aceptado' } as Record<string, string>)[
      status
    ];
  }
}
