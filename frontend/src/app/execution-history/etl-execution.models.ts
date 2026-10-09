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

export class EtlExecutionRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'EtlExecutionRequestError';
  }
}
