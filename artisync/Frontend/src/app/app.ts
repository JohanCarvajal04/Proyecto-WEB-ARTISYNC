import { Component, inject, signal, effect } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './shared/components/toast/toast.component';
import { AuthService } from './features/seguridad/services/auth.service';
import { UserService } from './features/perfil/services/user.service';
import { UserResponse } from './shared/models/user.model';
import { CompleteProfileModalComponent } from './shared/components/complete-profile-modal/complete-profile-modal.component';
import { ForceVerificationModalComponent } from './shared/components/force-verification-modal/force-verification-modal.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent, CompleteProfileModalComponent, ForceVerificationModalComponent],
  templateUrl: './app.html'
})
export class App {
  private authService = inject(AuthService);
  private userService = inject(UserService);

  readonly showProfileCompletion = signal<boolean>(false);
  readonly showForceVerification = signal<boolean>(false);
  readonly userProfile = signal<UserResponse | null>(null);

  constructor() {
    effect(() => {
      if (this.authService.isLoggedIn()) {
        this.userService.getCurrentUser().subscribe({
          next: (user) => {
            if (!user.fechaNacimiento || !user.idPais) {
              this.userProfile.set(user);
              this.showProfileCompletion.set(true);
            } else {
              if (user.roles.includes('CREADOR') || user.roles.includes('CLIENTE')) {
                if (!user.identidadVerificada) {
                  const pending = localStorage.getItem(`verificacion_pendiente_${user.idUsuario}`);
                  if (!pending) {
                    this.showForceVerification.set(true);
                  }
                }
              }
            }
          },
          error: (err) => console.error('No se pudo verificar si el perfil está completo', err)
        });
      } else {
        this.showProfileCompletion.set(false);
        this.showForceVerification.set(false);
        this.userProfile.set(null);
      }
    });
  }
}
