import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { Subscription } from 'rxjs';

import { CsvPreviewApiService } from './csv-preview-api.service';
import {
  CsvRemoveColumnResponse,
  CsvPreviewRequestError,
  CsvPreviewResponse,
} from './csv-preview.models';
import { CsvPreviewTable } from './csv-preview-table/csv-preview-table';

type CsvPreviewStatus = 'idle' | 'loading' | 'success' | 'error';
type RemoveColumnStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-csv-preview',
  imports: [CsvPreviewTable],
  templateUrl: './csv-preview.html',
  styleUrl: './csv-preview.css',
})
export class CsvPreview implements OnDestroy {
  private readonly csvPreviewApiService = inject(CsvPreviewApiService);
  private transformationSubscription: Subscription | null = null;

  protected readonly selectedFile = signal<File | null>(null);
  protected readonly previewStatus = signal<CsvPreviewStatus>('idle');
  protected readonly previewResult = signal<CsvPreviewResponse | null>(null);
  protected readonly previewErrorMessage = signal<string | null>(null);
  protected readonly selectedColumn = signal<string | null>(null);
  protected readonly removeColumnStatus = signal<RemoveColumnStatus>('idle');
  protected readonly transformedPreview = signal<CsvRemoveColumnResponse | null>(null);
  protected readonly transformationErrorMessage = signal<string | null>(null);
  protected readonly canPreview = computed(
    () => this.selectedFile() !== null && this.previewStatus() !== 'loading',
  );
  protected readonly canRemoveColumn = computed(
    () =>
      this.selectedFile() !== null &&
      this.selectedColumn() !== null &&
      this.removeColumnStatus() !== 'loading',
  );

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cancelTransformation();
    this.selectedFile.set(input.files?.item(0) ?? null);
    this.previewStatus.set('idle');
    this.previewResult.set(null);
    this.previewErrorMessage.set(null);
    this.selectedColumn.set(null);
    this.resetTransformationState();
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
    this.resetTransformationState();

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
    this.selectedColumn.set(select.value || null);
    this.resetTransformationState();
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

  ngOnDestroy(): void {
    this.cancelTransformation();
  }

  private cancelTransformation(): void {
    this.transformationSubscription?.unsubscribe();
    this.transformationSubscription = null;
  }

  private resetTransformationState(): void {
    this.removeColumnStatus.set('idle');
    this.transformedPreview.set(null);
    this.transformationErrorMessage.set(null);
  }

  private toSafeMessage(error: unknown): string {
    if (error instanceof CsvPreviewRequestError) {
      return error.message;
    }

    return 'No se pudo procesar el archivo. Inténtalo de nuevo.';
  }
}
