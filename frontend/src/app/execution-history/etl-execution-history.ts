import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, OnDestroy, signal, untracked } from '@angular/core';
import { forkJoin, Subscription } from 'rxjs';

import { EtlExecutionApiService } from './etl-execution-api.service';
import {
  EtlExecution,
  EtlExecutionRequestError,
  EtlExecutionStatus,
  EtlExecutionSummary,
  EtlExecutionType,
} from './etl-execution.models';

type HistoryStatus = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-etl-execution-history',
  imports: [DatePipe],
  templateUrl: './etl-execution-history.html',
  styleUrl: './etl-execution-history.css',
})
export class EtlExecutionHistory implements OnDestroy {
  private static readonly PAGE_SIZE = 10;
  private readonly apiService = inject(EtlExecutionApiService);
  private loadSubscription: Subscription | null = null;

  readonly refreshRequest = input(0);

  protected readonly status = signal<HistoryStatus>('loading');
  protected readonly executions = signal<readonly EtlExecution[]>([]);
  protected readonly summary = signal<EtlExecutionSummary>({
    total: 0,
    successful: 0,
    failed: 0,
    inProgress: 0,
  });
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selectedStatus = signal<EtlExecutionStatus | null>(null);
  protected readonly currentPage = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  constructor() {
    effect(() => {
      this.refreshRequest();
      untracked(() => this.loadExecutions());
    });
  }

  protected refresh(): void {
    this.loadExecutions();
  }

  protected filterByStatus(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedStatus.set(value === '' ? null : (value as EtlExecutionStatus));
    this.currentPage.set(0);
    this.loadExecutions();
  }

  protected previousPage(): void {
    if (this.currentPage() === 0) return;
    this.currentPage.update((page) => page - 1);
    this.loadExecutions();
  }

  protected nextPage(): void {
    if (this.currentPage() + 1 >= this.totalPages()) return;
    this.currentPage.update((page) => page + 1);
    this.loadExecutions();
  }

  protected transformationLabel(type: EtlExecutionType): string {
    switch (type) {
      case 'REMOVE_COLUMN':
        return 'Eliminar columna';
    }
  }

  protected statusLabel(status: EtlExecutionStatus): string {
    switch (status) {
      case 'PENDING':
        return 'Pendiente';
      case 'RUNNING':
        return 'En ejecución';
      case 'SUCCESS':
        return 'Correcta';
      case 'FAILED':
        return 'Fallida';
    }
  }

  protected duration(execution: EtlExecution): string {
    if (execution.finishedAt === null) {
      return 'No disponible';
    }

    const startedAt = Date.parse(execution.startedAt);
    const finishedAt = Date.parse(execution.finishedAt);
    if (!Number.isFinite(startedAt) || !Number.isFinite(finishedAt) || finishedAt < startedAt) {
      return 'No disponible';
    }

    const milliseconds = finishedAt - startedAt;
    if (milliseconds < 1_000) {
      return '< 1 s';
    }

    const totalSeconds = Math.floor(milliseconds / 1_000);
    if (totalSeconds < 60) {
      return `${totalSeconds} s`;
    }

    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes} min ${seconds} s`;
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
  }

  private loadExecutions(): void {
    this.loadSubscription?.unsubscribe();
    this.status.set('loading');
    this.errorMessage.set(null);

    this.loadSubscription = forkJoin({
      page: this.apiService.getExecutions({
        status: this.selectedStatus() ?? undefined,
        page: this.currentPage(),
        size: EtlExecutionHistory.PAGE_SIZE,
      }),
      summary: this.apiService.getSummary(),
    }).subscribe({
      next: ({ page, summary }) => {
        this.executions.set(page.executions);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.summary.set(summary);
        this.status.set('success');
      },
      error: (error: unknown) => {
        this.executions.set([]);
        this.errorMessage.set(this.toSafeMessage(error));
        this.status.set('error');
      },
    });
  }

  private toSafeMessage(error: unknown): string {
    if (error instanceof EtlExecutionRequestError) {
      return error.message;
    }

    return 'No se pudo cargar el historial de ejecuciones. Inténtalo de nuevo.';
  }
}
