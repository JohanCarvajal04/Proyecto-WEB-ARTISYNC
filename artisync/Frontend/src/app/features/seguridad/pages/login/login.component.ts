import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);

  form: FormGroup = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
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
          this.router.navigate(['/auth/two-factor'], { state: { correo: credentials.correo } });
        } else {
          this.toastService.success('¡Bienvenido de nuevo a Artisync!');
          let returnUrl = this.route.snapshot.queryParams['returnUrl'];
          if (returnUrl === '/' || returnUrl?.includes('/auth/login')) {
            returnUrl = null;
          }

          if (returnUrl) {
            this.router.navigateByUrl(returnUrl);
          } else {
            const role = this.authService.primaryRole();
            if (role === 'ADMINISTRADOR') {
              this.router.navigate(['/admin/users']);
            } else {
              this.router.navigate(['/admin/users']); // Por defecto en esta fase
            }
          }
        }
      },
      error: () => {
        this.isLoading.set(false);
        this.toastService.error('Usuario o contraseña incorrectos');
      }
    });
  }
}
