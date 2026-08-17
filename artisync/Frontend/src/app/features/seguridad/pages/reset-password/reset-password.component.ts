import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);
  token = '';

  form: FormGroup = this.fb.group({
    nuevaContrasena: ['', [Validators.required, Validators.minLength(8)]],
    confirmarContrasena: ['', [Validators.required]]
  }, { validators: this.passwordMatchValidator });

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] || '';
    if (!this.token) {
      this.toastService.error('Token inválido o faltante');
    }
  }

  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const pass = group.get('nuevaContrasena')?.value;
    const confirm = group.get('confirmarContrasena')?.value;
    return pass === confirm ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const { nuevaContrasena } = this.form.getRawValue();

    this.authService.resetPassword({ token: this.token, nuevaContrasena }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.toastService.success('Contraseña restablecida con éxito. Inicia sesión.');
        this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }
}
