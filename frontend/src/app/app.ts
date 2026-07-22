import { Component } from '@angular/core';

import { CsvPreview } from './csv-preview/csv-preview';

@Component({
  selector: 'app-root',
  imports: [CsvPreview],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
