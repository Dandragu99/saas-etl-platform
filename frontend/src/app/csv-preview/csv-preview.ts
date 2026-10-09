import { DOCUMENT } from '@angular/common';
import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { Subscription } from 'rxjs';

import { CsvPreviewApiService } from './csv-preview-api.service';
import {
  CsvDownloadResult,
  CsvRemoveColumnResponse,
  CsvPreviewRequestError,
  CsvPreviewResponse,
} from './csv-preview.models';
import { CsvPreviewTable } from './csv-preview-table/csv-preview-table';
import { EtlExecutionHistory } from '../execution-history/etl-execution-history';

type CsvPreviewStatus = 'idle' | 'loading' | 'success' | 'error';
type RemoveColumnStatus = 'idle' | 'loading' | 'success' | 'error';
type DownloadStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-csv-preview',
  imports: [CsvPreviewTable, EtlExecutionHistory],
  templateUrl: './csv-preview.html',
  styleUrl: './csv-preview.css',
})
export class CsvPreview implements OnDestroy {
  private readonly csvPreviewApiService = inject(CsvPreviewApiService);
  private readonly document = inject(DOCUMENT);
  private transformationSubscription: Subscription | null = null;
  private downloadSubscription: Subscription | null = null;

  protected readonly selectedFile = signal<File | null>(null);
  protected readonly previewStatus = signal<CsvPreviewStatus>('idle');
  protected readonly previewResult = signal<CsvPreviewResponse | null>(null);
  protected readonly previewErrorMessage = signal<string | null>(null);
  protected readonly selectedColumn = signal<string | null>(null);
  protected readonly removeColumnStatus = signal<RemoveColumnStatus>('idle');
  protected readonly transformedPreview = signal<CsvRemoveColumnResponse | null>(null);
  protected readonly transformationErrorMessage = signal<string | null>(null);
  protected readonly downloadStatus = signal<DownloadStatus>('idle');
  protected readonly downloadErrorMessage = signal<string | null>(null);
  protected readonly downloadedFileName = signal<string | null>(null);
  protected readonly historyRefreshRequest = signal(0);
  protected readonly canPreview = computed(
    () => this.selectedFile() !== null && this.previewStatus() !== 'loading',
  );
  protected readonly canRemoveColumn = computed(
    () =>
      this.selectedFile() !== null &&
      this.selectedColumn() !== null &&
      this.removeColumnStatus() !== 'loading',
  );
  protected readonly canDownload = computed(
    () =>
      this.selectedFile() !== null &&
      this.selectedColumn() !== null &&
      this.downloadStatus() !== 'loading',
  );

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cancelTransformation();
    this.cancelDownload();
    this.selectedFile.set(input.files?.item(0) ?? null);
    this.previewStatus.set('idle');
    this.previewResult.set(null);
    this.previewErrorMessage.set(null);
    this.selectedColumn.set(null);
    this.resetTransformationState();
    this.resetDownloadState();
  }

  protected submit(): void {
    const file = this.selectedFile();
    if (file === null || this.previewStatus() === 'loading') {
      return;
    }

    this.previewStatus.set('loading');
    this.previewResult.set(null);
    this.previewErrorMessage.set(null);
    this.selectedColumn.set(null);
    this.cancelTransformation();
    this.cancelDownload();
    this.resetTransformationState();
    this.resetDownloadState();

    this.csvPreviewApiService.preview(file).subscribe({
      next: (result) => {
        this.previewResult.set(result);
        this.previewStatus.set('success');
      },
      error: (error: unknown) => {
        this.previewErrorMessage.set(this.toSafeMessage(error));
        this.previewStatus.set('error');
      },
    });
  }

  protected onColumnSelected(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.cancelTransformation();
    this.cancelDownload();
    this.selectedColumn.set(select.value || null);
    this.resetTransformationState();
    this.resetDownloadState();
  }

  protected removeColumn(): void {
    const file = this.selectedFile();
    const column = this.selectedColumn();
    if (file === null || column === null || this.removeColumnStatus() === 'loading') {
      return;
    }

    this.cancelTransformation();
    this.removeColumnStatus.set('loading');
    this.transformedPreview.set(null);
    this.transformationErrorMessage.set(null);

    this.transformationSubscription = this.csvPreviewApiService
      .removeColumn(file, column)
      .subscribe({
        next: (result) => {
          this.transformedPreview.set(result);
          this.removeColumnStatus.set('success');
        },
        error: (error: unknown) => {
          this.transformationErrorMessage.set(this.toSafeMessage(error));
          this.removeColumnStatus.set('error');
        },
      });
  }

  protected downloadRemovedColumn(): void {
    const file = this.selectedFile();
    const column = this.selectedColumn();
    if (file === null || column === null || this.downloadStatus() === 'loading') {
      return;
    }

    this.cancelDownload();
    this.downloadStatus.set('loading');
    this.downloadErrorMessage.set(null);
    this.downloadedFileName.set(null);

    this.downloadSubscription = this.csvPreviewApiService
      .downloadRemovedColumn(file, column)
      .subscribe({
        next: (result) => {
          this.requestHistoryRefresh();
          try {
            this.startBrowserDownload(result);
            this.downloadedFileName.set(result.fileName);
            this.downloadStatus.set('success');
          } catch {
            this.downloadErrorMessage.set('No se pudo iniciar la descarga. Inténtalo de nuevo.');
            this.downloadStatus.set('error');
          }
        },
        error: (error: unknown) => {
          this.requestHistoryRefresh();
          this.downloadErrorMessage.set(this.toSafeMessage(error));
          this.downloadStatus.set('error');
        },
      });
  }

  ngOnDestroy(): void {
    this.cancelTransformation();
    this.cancelDownload();
  }

  private cancelTransformation(): void {
    this.transformationSubscription?.unsubscribe();
    this.transformationSubscription = null;
  }

  private cancelDownload(): void {
    this.downloadSubscription?.unsubscribe();
    this.downloadSubscription = null;
  }

  private resetTransformationState(): void {
    this.removeColumnStatus.set('idle');
    this.transformedPreview.set(null);
    this.transformationErrorMessage.set(null);
  }

  private resetDownloadState(): void {
    this.downloadStatus.set('idle');
    this.downloadErrorMessage.set(null);
    this.downloadedFileName.set(null);
  }

  private requestHistoryRefresh(): void {
    this.historyRefreshRequest.update((value) => value + 1);
  }

  private startBrowserDownload(result: CsvDownloadResult): void {
    const objectUrl = URL.createObjectURL(result.blob);
    let link: HTMLAnchorElement | null = null;

    try {
      link = this.document.createElement('a');
      link.href = objectUrl;
      link.download = result.fileName;
      this.document.body.appendChild(link);
      link.click();
    } finally {
      try {
        link?.remove();
      } finally {
        URL.revokeObjectURL(objectUrl);
      }
    }
  }

  private toSafeMessage(error: unknown): string {
    if (error instanceof CsvPreviewRequestError) {
      return error.message;
    }

    return 'No se pudo procesar el archivo. Inténtalo de nuevo.';
  }
}
