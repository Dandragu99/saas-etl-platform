import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MiniClient } from './mini.models';
import { MiniStoreService } from './mini-store.service';

@Component({
  selector: 'app-mini-clients',
  imports: [ReactiveFormsModule],
  templateUrl: './mini-clients.html',
  styleUrl: './mini-clients.css',
})
export class MiniClients {
  protected readonly store = inject(MiniStoreService);
  private readonly fb = inject(FormBuilder);
  protected readonly open = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly query = signal('');
  protected readonly saving = signal(false);
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    taxId: [''],
    phone: [''],
    email: ['', [Validators.email]],
    address: [''],
  });
  protected filtered(): readonly MiniClient[] {
    const q = this.query().trim().toLocaleLowerCase('es');
    return q
      ? this.store
          .clients()
          .filter((c) => `${c.name} ${c.phone}`.toLocaleLowerCase('es').includes(q))
      : this.store.clients();
  }
  protected search(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }
  protected create(): void {
    this.editingId.set(null);
    this.form.reset();
    this.open.set(true);
  }
  protected edit(client: MiniClient): void {
    this.editingId.set(client.id);
    this.form.setValue({
      name: client.name,
      taxId: client.taxId,
      phone: client.phone,
      email: client.email,
      address: client.address,
    });
    this.open.set(true);
  }
  protected async save(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving.set(true);
    const value = this.form.getRawValue();
    await this.store.saveClient({
      ...value,
      id: this.editingId() ?? crypto.randomUUID(),
      createdAt: new Date().toISOString().slice(0, 10),
    });
    this.saving.set(false);
    this.open.set(false);
  }
}
