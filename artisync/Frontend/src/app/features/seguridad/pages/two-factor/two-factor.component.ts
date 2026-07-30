import { Component, inject, signal, OnInit } from '@angular/core';
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
export class TwoFactorComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);
  correo = '';

  form: FormGroup = this.fb.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]]
  });

  ngOnInit(): void {
    const navState = history.state;
    this.correo = navState?.correo || '';
    if (!this.correo) {
      this.toastService.warning('No se identificó el correo de sesión. Por favor inicia sesión.');
      this.router.navigate(['/auth/login']);
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const { codigo } = this.form.getRawValue();

    this.authService.verify2fa({ correo: this.correo, codigo }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.toastService.success('¡Verificación de dos factores completada!');
        const role = this.authService.primaryRole();
        if (role === 'ADMINISTRADOR') {
          this.router.navigate(['/admin/users']);
        } else {
          this.router.navigate(['/admin/users']);
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }
}
