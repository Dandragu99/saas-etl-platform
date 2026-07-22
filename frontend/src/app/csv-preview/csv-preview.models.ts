export type CsvPreviewRow = Readonly<Record<string, string>>;

export interface CsvTabularPreview {
  readonly fileName: string;
  readonly columns: readonly string[];
  readonly rows: readonly CsvPreviewRow[];
  readonly previewRowCount: number;
  readonly truncated: boolean;
}

export interface CsvPreviewResponse extends CsvTabularPreview {}

export interface RemoveColumnTransformation {
  readonly type: 'REMOVE_COLUMN';
  readonly removedColumn: string;
}

export interface CsvRemoveColumnResponse extends CsvTabularPreview {
  readonly transformation: RemoveColumnTransformation;
}

export interface ApiErrorResponse {
  readonly code: string;
  readonly message: string;
  readonly status: number;
  readonly path: string;
  readonly timestamp: string;
}

export class CsvPreviewRequestError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = 'CsvPreviewRequestError';
  }
}
