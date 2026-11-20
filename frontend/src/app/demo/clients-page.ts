import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DEMO_CLIENTS, DemoClient, money } from './demo-data';

@Component({
  selector: 'app-clients-page',
  imports: [RouterLink],
  templateUrl: './clients-page.html',
  styleUrl: './clients-page.css',
})
export class ClientsPage {
  protected readonly query = signal('');
  protected readonly selected = signal<DemoClient | null>(null);
  protected readonly money = money;
  protected readonly clients = computed(() => {
    const q = this.query().trim().toLocaleLowerCase('es');
    return q
      ? DEMO_CLIENTS.filter((c) => `${c.name} ${c.contact}`.toLocaleLowerCase('es').includes(q))
      : DEMO_CLIENTS;
  });
  protected search(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }
}
