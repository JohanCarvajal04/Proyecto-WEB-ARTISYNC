import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css',
  encapsulation: ViewEncapsulation.None
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);
  readonly emailSent = signal<boolean>(false);

  form: FormGroup = this.fb.group({
    correo: ['', [Validators.required, Validators.email]]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const { correo } = this.form.getRawValue();

    this.authService.forgotPassword({ correo }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.emailSent.set(true);
        this.toastService.success('Instrucciones enviadas a tu correo');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo enviar el correo. Intenta de nuevo.');
      }
    });
  }
}
