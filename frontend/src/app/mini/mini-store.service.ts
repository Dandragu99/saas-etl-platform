import { computed, inject, Injectable, signal } from '@angular/core';

import { MiniDatabaseService } from './mini-database.service';
import {
  DEFAULT_SETTINGS,
  MiniClient,
  MiniQuote,
  MiniSettings,
  MiniWork,
  WorkStatus,
} from './mini.models';

@Injectable({ providedIn: 'root' })
export class MiniStoreService {
  private readonly database = inject(MiniDatabaseService);
  readonly clients = signal<readonly MiniClient[]>([]);
  readonly quotes = signal<readonly MiniQuote[]>([]);
  readonly works = signal<readonly MiniWork[]>([]);
  readonly settings = signal<MiniSettings>(DEFAULT_SETTINGS);
  readonly ready = signal(false);
  readonly pendingWorks = computed(() => this.works().filter((work) => work.status !== 'DONE'));

  async load(): Promise<void> {
    const [clients, quotes, works, settings] = await Promise.all([
      this.database.all<MiniClient>('clients'),
      this.database.all<MiniQuote>('quotes'),
      this.database.all<MiniWork>('works'),
      this.database.get<MiniSettings>('settings', 'business'),
    ]);
    this.clients.set([...clients].sort((a, b) => a.name.localeCompare(b.name, 'es')));
    this.quotes.set([...quotes].sort((a, b) => b.date.localeCompare(a.date)));
    this.works.set([...works].sort((a, b) => a.date.localeCompare(b.date)));
    this.settings.set(settings ?? DEFAULT_SETTINGS);
    this.ready.set(true);
  }

  async saveClient(client: MiniClient): Promise<void> {
    await this.database.put('clients', client);
    await this.load();
  }

  async saveQuote(quote: MiniQuote): Promise<void> {
    await this.database.put('quotes', quote);
    await this.load();
  }

  async changeWorkStatus(work: MiniWork, status: WorkStatus): Promise<void> {
    await this.database.put('works', { ...work, status });
    await this.load();
  }

  async saveSettings(settings: MiniSettings): Promise<void> {
    await this.database.put('settings', settings);
    await this.load();
  }
}
