import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthRequestError } from '../auth.models';
import { AuthSessionService } from '../auth-session.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: '../auth-page.css',
})
export class Login {
  private readonly formBuilder = inject(FormBuilder);
  private readonly sessions = inject(AuthSessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly registered = this.route.snapshot.queryParamMap.get('registered') === 'true';
  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.sessions.login(this.form.getRawValue()).subscribe({
      next: () => {
        const requested = this.route.snapshot.queryParamMap.get('returnUrl');
        const destination = requested?.startsWith('/') ? requested : '/dashboard';
        void this.router.navigateByUrl(destination);
      },
      error: (error: unknown) => {
        this.errorMessage.set(
          error instanceof AuthRequestError
            ? error.message
            : 'No se pudo iniciar sesión. Inténtalo de nuevo.',
        );
        this.submitting.set(false);
      },
    });
  }
}
