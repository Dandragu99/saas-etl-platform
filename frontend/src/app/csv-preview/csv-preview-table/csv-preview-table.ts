import { Component, input } from '@angular/core';

import { CsvPreviewRow, CsvTabularPreview } from '../csv-preview.models';

let nextTableId = 0;

@Component({
  selector: 'app-csv-preview-table',
  templateUrl: './csv-preview-table.html',
  styleUrl: './csv-preview-table.css',
})
export class CsvPreviewTable {
  readonly preview = input.required<CsvTabularPreview>();
  readonly label = input.required<string>();
  protected readonly labelId = `csv-preview-table-title-${nextTableId++}`;

  protected cellValue(row: CsvPreviewRow, column: string): string {
    return row[column] ?? '';
  }
}
