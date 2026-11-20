import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthSessionService } from '../auth/auth-session.service';
import { hasCompanyPermission } from '../auth/company-permissions';
import { BusinessApiService } from './business-api.service';
import { BusinessDashboard, Customer, Invoice, Quote, QuoteInput } from './business.models';

@Component({
  selector: 'app-business-workspace',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './business-workspace.html',
  styleUrl: './business-workspace.css',
})
export class BusinessWorkspace implements OnInit {
  private readonly api = inject(BusinessApiService);
  private readonly sessions = inject(AuthSessionService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(true);
  protected readonly pending = signal(false);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly editingCustomerId = signal<string | null>(null);
  protected readonly dashboard = signal<BusinessDashboard | null>(null);
  protected readonly customers = signal<readonly Customer[]>([]);
  protected readonly quotes = signal<readonly Quote[]>([]);
  protected readonly invoices = signal<readonly Invoice[]>([]);
  protected readonly canWrite = () => hasCompanyPermission(
    this.sessions.session()?.activeCompany?.role,
    'BUSINESS_WRITE',
  );

  protected readonly customerForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    taxId: ['', Validators.maxLength(30)],
    email: ['', [Validators.email, Validators.maxLength(320)]],
    phone: ['', Validators.maxLength(40)],
    address: ['', Validators.maxLength(500)],
    notes: ['', Validators.maxLength(2000)],
  });

  protected readonly quoteForm = this.fb.nonNullable.group({
    customerId: ['', Validators.required],
    issueDate: [this.today(), Validators.required],
    validUntil: [this.afterDays(30), Validators.required],
    notes: ['', Validators.maxLength(2000)],
    lines: this.fb.array([this.newLine()]),
  });

  protected get lines(): FormArray { return this.quoteForm.controls.lines; }

  ngOnInit(): void { this.reload(); }

  protected addLine(): void { if (this.lines.length < 100) this.lines.push(this.newLine()); }
  protected removeLine(index: number): void { if (this.lines.length > 1) this.lines.removeAt(index); }

  protected saveCustomer(): void {
    this.customerForm.markAllAsTouched();
    if (this.customerForm.invalid || !this.canWrite()) return;
    const id = this.editingCustomerId();
    const request = id
      ? this.api.updateCustomer(id, this.customerForm.getRawValue())
      : this.api.createCustomer(this.customerForm.getRawValue());
    this.run(request, () => {
      this.customerForm.reset();
      this.editingCustomerId.set(null);
      this.notice.set(id ? 'Cliente actualizado correctamente.' : 'Cliente creado correctamente.');
    });
  }

  protected editCustomer(customer: Customer): void {
    this.editingCustomerId.set(customer.id);
    this.customerForm.setValue({
      name: customer.name, taxId: customer.taxId ?? '', email: customer.email ?? '',
      phone: customer.phone ?? '', address: customer.address ?? '', notes: customer.notes ?? '',
    });
  }

  protected cancelEdit(): void {
    this.editingCustomerId.set(null);
    this.customerForm.reset();
  }

  protected saveQuote(): void {
    this.quoteForm.markAllAsTouched();
    const value = this.quoteForm.getRawValue();
    if (this.quoteForm.invalid || value.validUntil < value.issueDate || !this.canWrite()) return;
    const input: QuoteInput = {
      ...value,
      lines: value.lines.map((line) => ({
        description: line.description,
        quantity: Number(line.quantity),
        unitPrice: Number(line.unitPrice),
        taxRate: Number(line.taxRate),
      })),
    };
    this.run(this.api.createQuote(input), () => {
      this.quoteForm.reset({ issueDate: this.today(), validUntil: this.afterDays(30), notes: '', customerId: '' });
      while (this.lines.length > 1) this.lines.removeAt(1);
      this.lines.at(0).reset({ description: '', quantity: 1, unitPrice: 0, taxRate: 21 });
      this.notice.set('Presupuesto creado. Ya puedes convertirlo en factura.');
    });
  }

  protected convert(quote: Quote): void {
    this.run(this.api.convertQuote(quote.id), () => this.notice.set(`${quote.number} convertido en factura.`));
  }

  protected seed(): void {
    this.run(this.api.seedDemo(), () => this.notice.set('Datos ficticios preparados para la demostración.'));
  }

  protected download(kind: 'quotes' | 'invoices', id: string, number: string): void {
    this.api.download(kind, id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${number}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (error: Error) => this.error.set(error.message),
    });
  }

  protected money(value: number): string {
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(value);
  }

  protected fieldError(name: 'name' | 'email'): string {
    const field = this.customerForm.controls[name];
    if (!field.touched || !field.errors) return '';
    if (field.errors['required']) return 'El nombre es obligatorio.';
    if (field.errors['email']) return 'Introduce un correo válido.';
    return 'El valor es demasiado largo.';
  }

  protected reload(): void {
    this.loading.set(true);
    this.api.loadWorkspace().subscribe({
      next: (data) => {
        this.dashboard.set(data.dashboard);
        this.customers.set(data.customers);
        this.quotes.set(data.quotes);
        this.invoices.set(data.invoices);
        this.loading.set(false);
        this.pending.set(false);
      },
      error: (error: Error) => { this.error.set(error.message); this.loading.set(false); this.pending.set(false); },
    });
  }

  private run(request: { subscribe: (observer: { next: () => void; error: (error: Error) => void }) => unknown }, onSuccess: () => void): void {
    this.pending.set(true);
    this.error.set('');
    this.notice.set('');
    request.subscribe({
      next: () => { onSuccess(); this.reload(); },
      error: (error: Error) => { this.error.set(error.message); this.pending.set(false); },
    });
  }

  private newLine() {
    return this.fb.nonNullable.group({
      description: ['', [Validators.required, Validators.maxLength(500)]],
      quantity: [1, [Validators.required, Validators.min(0.001)]],
      unitPrice: [0, [Validators.required, Validators.min(0)]],
      taxRate: [21, [Validators.required, Validators.min(0), Validators.max(100)]],
    });
  }

  private today(): string { return new Date().toISOString().slice(0, 10); }
  private afterDays(days: number): string {
    const date = new Date(); date.setDate(date.getDate() + days); return date.toISOString().slice(0, 10);
  }
}
