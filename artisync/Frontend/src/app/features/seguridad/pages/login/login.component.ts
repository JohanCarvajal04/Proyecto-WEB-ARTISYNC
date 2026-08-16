import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  encapsulation: ViewEncapsulation.None
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);
  readonly showPassword = signal<boolean>(false);

  form: FormGroup = this.fb.group({
    correo: ['', [Validators.required]],
    contrasena: ['', [Validators.required]]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const credentials = this.form.getRawValue();

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        if (response.requiere2fa) {
          this.toastService.info('Se requiere verificación de dos factores');
          // §2.1 (OBS-AUTO-05): ya no se pasa el correo por history.state — el
          // backend identifica al usuario mediante el ticket pre-auth en la
          // cookie HttpOnly "preAuth2fa" que /auth/login acaba de fijar.
          this.router.navigate(['/auth/two-factor']);
        } else {
          this.toastService.success('¡Bienvenido de nuevo a Artisync!');
          let returnUrl = this.route.snapshot.queryParams['returnUrl'];
          if (returnUrl === '/' || returnUrl?.includes('/auth/login')) {
            returnUrl = null;
          }

          if (returnUrl) {
            this.router.navigateByUrl(returnUrl);
          } else {
            this.router.navigateByUrl(this.authService.homeRoute());
          }
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err?.status === 429) {
          // El backend responde con ProblemDetail (RFC 7807): el mensaje va en
          // "detail", no en "mensaje" (§2.2 / OBS-AUTO-06 — antes el filtro de
          // rate limit escribía un JSON ad-hoc {"mensaje": ...}).
          const msg = err.error?.detail || 'Demasiados intentos de inicio de sesión. Espera un minuto e intenta nuevamente.';
          this.toastService.warning(msg);
        } else {
          this.toastService.error('Usuario o contraseña incorrectos');
        }
      }
    });
  }
}
