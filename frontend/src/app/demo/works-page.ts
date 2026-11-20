import { Component, computed, signal } from '@angular/core';
import { DEMO_WORKS } from './demo-data';

@Component({
  selector: 'app-works-page',
  templateUrl: './works-page.html',
  styleUrl: './works-page.css',
})
export class WorksPage {
  protected readonly filter = signal<'ALL' | 'PENDING' | 'ACTIVE' | 'DONE'>('ALL');
  protected readonly notice = signal('');
  protected readonly works = computed(() =>
    this.filter() === 'ALL'
      ? DEMO_WORKS
      : DEMO_WORKS.filter((work) => work.status === this.filter()),
  );
  protected setFilter(event: Event): void {
    this.filter.set(
      (event.target as HTMLSelectElement).value as 'ALL' | 'PENDING' | 'ACTIVE' | 'DONE',
    );
  }
  protected label(status: string): string {
    return (
      { PENDING: 'Pendiente', ACTIVE: 'En curso', DONE: 'Terminada' } as Record<string, string>
    )[status];
  }
  protected showPlanning(title: string): void {
    this.notice.set(`Planificación de “${title}” preparada para la siguiente fase.`);
  }
}
