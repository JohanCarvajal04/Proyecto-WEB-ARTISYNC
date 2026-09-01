import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
  encapsulation: ViewEncapsulation.None
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);

  /** Destino a retomar tras iniciar sesión, p. ej. llegando desde /acceso-requerido. */
  readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
  readonly showPassword = signal<boolean>(false);
  readonly showConfirmPassword = signal<boolean>(false);
  maxDate: string;

  constructor() {
    const today = new Date();
    today.setFullYear(today.getFullYear() - 18);
    this.maxDate = today.toISOString().split('T')[0];
  }

  form: FormGroup = this.fb.group({
    nombreCompleto: ['', [Validators.required, Validators.maxLength(200)]],
    correo: ['', [Validators.required, Validators.email]],
    fechaNacimiento: ['', [Validators.required, this.ageValidator]],
    contrasena: ['', [Validators.required, Validators.minLength(8), this.passwordStrengthValidator]],
    confirmarContrasena: ['', [Validators.required]],
    terminosYCondiciones: [false, Validators.requiredTrue]
  }, { validators: this.passwordMatchValidator });

  ageValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    const birthDate = new Date(control.value);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age >= 18 ? null : { underage: true };
  }

  passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const val = control.value || '';
    const hasUpper = /[A-Z]/.test(val);
    const hasLower = /[a-z]/.test(val);
    const hasNum = /[0-9]/.test(val);
    if (!hasUpper || !hasLower || !hasNum) {
      return { weakPassword: true };
    }
    return null;
  }

  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const pass = group.get('contrasena')?.value;
    const confirm = group.get('confirmarContrasena')?.value;
    return pass === confirm ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const val = this.form.getRawValue();

    const nameParts = (val.nombreCompleto || '').trim().split(' ');
    const nombres = nameParts[0] || '';
    const apellidos = nameParts.slice(1).join(' ') || nameParts[0] || '';

    this.authService.register({
      nombres,
      apellidos,
      correo: val.correo,
      contrasena: val.contrasena,
      fechaNacimiento: val.fechaNacimiento,
      rol: 'CLIENTE',
      aceptaTerminos: val.terminosYCondiciones
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.toastService.success('¡Cuenta creada exitosamente! Por favor inicia sesión.');
        this.router.navigate(['/auth/login'], this.returnUrl ? { queryParams: { returnUrl: this.returnUrl } } : {});
      },
      error: (err) => {
        this.isLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo crear la cuenta. Intenta de nuevo.');
      }
    });
  }
}
