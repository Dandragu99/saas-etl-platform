import { Component, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MiniQuote, MiniQuoteLine, miniMoney } from './mini.models';
import { MiniStoreService } from './mini-store.service';

@Component({
  selector: 'app-mini-quote-form',
  imports: [ReactiveFormsModule],
  templateUrl: './mini-quote-form.html',
  styleUrl: './mini-quote-form.css',
})
export class MiniQuoteForm {
  protected readonly store = inject(MiniStoreService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  protected readonly money = miniMoney;
  protected readonly form = this.fb.nonNullable.group({
    clientId: ['', Validators.required],
    taxRate: [21, [Validators.required, Validators.min(0), Validators.max(100)]],
    notes: [''],
    lines: this.fb.array([this.newLine('MATERIAL'), this.newLine('LABOR')]),
  });
  protected get lines(): FormArray {
    return this.form.controls.lines;
  }
  protected addLine(kind: 'MATERIAL' | 'LABOR'): void {
    this.lines.push(this.newLine(kind));
  }
  protected removeLine(index: number): void {
    if (this.lines.length > 1) this.lines.removeAt(index);
  }
  protected subtotal(): number {
    return this.form
      .getRawValue()
      .lines.reduce((sum, line) => sum + Number(line.quantity) * Number(line.unitPrice), 0);
  }
  protected tax(): number {
    return (this.subtotal() * Number(this.form.controls.taxRate.value)) / 100;
  }
  protected total(): number {
    return this.subtotal() + this.tax();
  }
  protected async save(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const client = this.store.clients().find((item) => item.id === value.clientId);
    if (!client) return;
    const quote: MiniQuote = {
      id: crypto.randomUUID(),
      number: `PRE-${new Date().getFullYear()}-${String(this.store.quotes().length + 1).padStart(3, '0')}`,
      clientId: client.id,
      clientName: client.name,
      date: new Date().toISOString().slice(0, 10),
      status: 'DRAFT',
      taxRate: Number(value.taxRate),
      notes: value.notes,
      lines: value.lines as readonly MiniQuoteLine[],
      subtotal: this.subtotal(),
      tax: this.tax(),
      total: this.total(),
    };
    await this.store.saveQuote(quote);
    await this.router.navigate(['/mini/presupuestos']);
  }
  private newLine(kind: 'MATERIAL' | 'LABOR') {
    return this.fb.nonNullable.group({
      description: [kind === 'MATERIAL' ? '' : 'Mano de obra', [Validators.required]],
      kind: [kind],
      quantity: [1, [Validators.required, Validators.min(0.01)]],
      unitPrice: [0, [Validators.required, Validators.min(0)]],
    });
  }
}
