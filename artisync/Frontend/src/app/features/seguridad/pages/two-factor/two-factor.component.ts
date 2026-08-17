import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-two-factor',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './two-factor.component.html'
})
export class TwoFactorComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);

  // §2.1 (OBS-AUTO-05): el backend identifica al usuario mediante la cookie
  // HttpOnly "preAuth2fa" — ya no depende de history.state, así que esta
  // página sigue siendo usable tras un F5 (antes, perder el state expulsaba
  // al usuario de vuelta a /auth/login).
  //
  // Acepta 6 dígitos (TOTP) u 8 caracteres alfanuméricos (código de respaldo)
  // — antes el patrón forzaba exactamente 6 dígitos, dejando sin forma de
  // entrar a quien perdía su autenticador y solo tenía códigos de respaldo.
  form: FormGroup = this.fb.group({
    codigo: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(8)]]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const { codigo } = this.form.getRawValue();

    this.authService.verify2fa({ codigo }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.toastService.success('¡Verificación de dos factores completada!');
        this.router.navigateByUrl(this.authService.homeRoute());
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err?.status === 401) {
          this.toastService.error(err.error?.detail || 'Código inválido o sesión de verificación expirada.');
          this.router.navigate(['/auth/login']);
        } else {
          this.toastService.error(err.error?.detail || 'No se pudo verificar el código. Intenta nuevamente.');
        }
      }
    });
  }
}
