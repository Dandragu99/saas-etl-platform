import { Component, inject, signal } from '@angular/core';
import { MiniWork, WorkStatus } from './mini.models';
import { MiniStoreService } from './mini-store.service';

@Component({
  selector: 'app-mini-works',
  templateUrl: './mini-works.html',
  styleUrl: './mini-works.css',
})
export class MiniWorks {
  protected readonly store = inject(MiniStoreService);
  protected readonly filter = signal<'ALL' | WorkStatus>('ALL');
  protected works(): readonly MiniWork[] {
    return this.filter() === 'ALL'
      ? this.store.works()
      : this.store.works().filter((item) => item.status === this.filter());
  }
  protected setFilter(event: Event): void {
    this.filter.set((event.target as HTMLSelectElement).value as 'ALL' | WorkStatus);
  }
  protected async next(work: MiniWork): Promise<void> {
    const status: WorkStatus =
      work.status === 'PENDING' ? 'ACTIVE' : work.status === 'ACTIVE' ? 'DONE' : 'PENDING';
    await this.store.changeWorkStatus(work, status);
  }
  protected label(status: WorkStatus): string {
    return ({ PENDING: 'Pendiente', ACTIVE: 'En curso', DONE: 'Finalizado' } as const)[status];
  }
}
