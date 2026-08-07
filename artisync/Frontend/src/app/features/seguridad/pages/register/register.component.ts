import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
  encapsulation: ViewEncapsulation.None
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);

  form: FormGroup = this.fb.group({
    nombreCompleto: ['', [Validators.required, Validators.maxLength(200)]],
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.required, Validators.minLength(8), this.passwordStrengthValidator]],
    confirmarContrasena: ['', [Validators.required]],
    terminosYCondiciones: [false, Validators.requiredTrue]
  }, { validators: this.passwordMatchValidator });

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
      fechaNacimiento: '2000-01-01',
      rol: 'CLIENTE',
      aceptaTerminos: val.terminosYCondiciones
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.toastService.success('¡Cuenta creada exitosamente! Por favor inicia sesión.');
        this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }
}
