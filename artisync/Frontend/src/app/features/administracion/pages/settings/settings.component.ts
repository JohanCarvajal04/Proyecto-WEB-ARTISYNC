import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../../perfil/services/user.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { UserResponse } from '../../../../shared/models/user.model';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './settings.component.html'
})
export class SettingsComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  readonly isLoading = signal<boolean>(true);
  readonly isSubmitting = signal<boolean>(false);
  readonly userProfile = signal<UserResponse | null>(null);

  toggle2FA(): void {
    this.router.navigate(['/profile/two-factor']);
  }

  openPasswordModal(): void {
    this.router.navigate(['/profile/change-password']);
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.userService.getCurrentUser().subscribe({
      next: (profile) => {
        this.userProfile.set(profile);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('Error al cargar la información del perfil');
        this.isLoading.set(false);
      }
    });
  }


  revokeAllSessions(): void {
    this.isSubmitting.set(true);
    this.userService.revokeAllMySessions().subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.toastService.success(res.message || 'Todas las sesiones fueron cerradas.');
      },
      error: () => {
        this.isSubmitting.set(false);
      }
    });
  }

  savePreferences(): void {
    this.toastService.success('Preferencias de notificación guardadas');
  }
}
