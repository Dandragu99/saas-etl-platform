import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthRequestError } from '../auth.models';
import { AuthSessionService } from '../auth-session.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: '../auth-page.css',
})
export class Register {
  private readonly formBuilder = inject(FormBuilder);
  private readonly sessions = inject(AuthSessionService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
    companyName: ['', [Validators.required, Validators.maxLength(150)]],
    defaultCurrency: ['EUR', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.sessions.register(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigate(['/login'], { queryParams: { registered: true } }),
      error: (error: unknown) => {
        this.errorMessage.set(
          error instanceof AuthRequestError
            ? error.message
            : 'No se pudo crear la cuenta. Inténtalo de nuevo.',
        );
        this.submitting.set(false);
      },
    });
  }
}
