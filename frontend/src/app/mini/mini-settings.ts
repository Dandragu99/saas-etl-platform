import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MiniStoreService } from './mini-store.service';

@Component({
  selector: 'app-mini-settings',
  imports: [ReactiveFormsModule],
  templateUrl: './mini-settings.html',
  styleUrl: './mini-settings.css',
})
export class MiniSettingsPage {
  protected readonly store = inject(MiniStoreService);
  private readonly fb = inject(FormBuilder);
  protected readonly saved = signal(false);
  protected readonly form = this.fb.nonNullable.group({
    tradeName: ['', Validators.required],
    taxId: [''],
    address: [''],
    phone: [''],
    email: ['', [Validators.email]],
    logoDataUrl: [''],
  });
  constructor() {
    effect(() => this.form.setValue(this.store.settings()));
  }
  protected logo(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => this.form.controls.logoDataUrl.setValue(String(reader.result));
    reader.readAsDataURL(file);
  }
  protected async save(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    await this.store.saveSettings({ id: 'business', ...this.form.getRawValue() });
    this.saved.set(true);
  }
}
