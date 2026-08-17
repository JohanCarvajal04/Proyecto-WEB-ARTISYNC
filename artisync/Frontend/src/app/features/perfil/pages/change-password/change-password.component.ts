import { Component, inject, signal } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html'
})
export class ChangePasswordComponent {
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);
  private location = inject(Location);

  readonly isLoading = signal<boolean>(false);

  passwordForm = this.fb.group({
    contrasenaActual: ['', Validators.required],
    nuevaContrasena: ['', [Validators.required, Validators.minLength(8)]],
    confirmarContrasena: ['', Validators.required]
  }, { validators: this.passwordMatchValidator });

  passwordMatchValidator(g: any) {
    return g.get('nuevaContrasena').value === g.get('confirmarContrasena').value
      ? null : { 'mismatch': true };
  }

  goBack(): void {
    this.location.back();
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) {
      this.toastService.error('Revisa los campos del formulario');
      return;
    }

    const { contrasenaActual, nuevaContrasena } = this.passwordForm.value;
    if (!contrasenaActual || !nuevaContrasena) return;

    this.isLoading.set(true);
    this.userService.changePassword({ contrasenaActual, nuevaContrasena }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.toastService.success(res.mensaje || res.message || 'Contraseña actualizada exitosamente');
        this.goBack();
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }
}
