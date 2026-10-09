import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, OnDestroy, signal, untracked } from '@angular/core';
import { Subscription } from 'rxjs';

import { EtlExecutionApiService } from './etl-execution-api.service';
import {
  EtlExecution,
  EtlExecutionRequestError,
  EtlExecutionStatus,
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
  private readonly apiService = inject(EtlExecutionApiService);
  private loadSubscription: Subscription | null = null;

  readonly refreshRequest = input(0);

  protected readonly status = signal<HistoryStatus>('loading');
  protected readonly executions = signal<readonly EtlExecution[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.refreshRequest();
      untracked(() => this.loadExecutions());
    });
  }

  protected refresh(): void {
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

    this.loadSubscription = this.apiService.getExecutions().subscribe({
      next: (executions) => {
        this.executions.set(executions);
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
