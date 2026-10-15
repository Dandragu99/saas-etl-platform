export type EtlExecutionType = 'REMOVE_COLUMN';

export type EtlExecutionStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

export interface EtlExecution {
  readonly id: string;
  readonly type: EtlExecutionType;
  readonly status: EtlExecutionStatus;
  readonly startedAt: string;
  readonly finishedAt: string | null;
  readonly processedRecordCount: number | null;
  readonly errorMessage: string | null;
}

export interface EtlExecutionQuery {
  readonly status?: EtlExecutionStatus;
  readonly page: number;
  readonly size: number;
}

export interface EtlExecutionPage {
  readonly executions: readonly EtlExecution[];
  readonly totalElements: number;
  readonly page: number;
  readonly size: number;
  readonly totalPages: number;
}

export interface EtlExecutionSummary {
  readonly total: number;
  readonly successful: number;
  readonly failed: number;
  readonly inProgress: number;
}

export class EtlExecutionRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'EtlExecutionRequestError';
  }
}
