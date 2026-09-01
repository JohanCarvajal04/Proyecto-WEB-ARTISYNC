import { Component, inject, signal, effect } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './shared/components/toast/toast.component';
import { AuthService } from './features/seguridad/services/auth.service';
import { UserService } from './features/perfil/services/user.service';
import { UserResponse } from './shared/models/user.model';
import { CompleteProfileModalComponent } from './shared/components/complete-profile-modal/complete-profile-modal.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent, CompleteProfileModalComponent],
  templateUrl: './app.html'
})
export class App {
  private authService = inject(AuthService);
  private userService = inject(UserService);

  readonly showProfileCompletion = signal<boolean>(false);
  readonly userProfile = signal<UserResponse | null>(null);

  constructor() {
    effect(() => {
      if (this.authService.isLoggedIn()) {
        this.userService.getCurrentUser().subscribe({
          next: (user) => {
            if (!user.fechaNacimiento || !user.idPais) {
              this.userProfile.set(user);
              this.showProfileCompletion.set(true);
            }
          },
          // Best-effort: si falla, el modal de completar perfil simplemente no
          // aparece esta vez (se reintenta en el próximo cambio de sesión/ruta).
          // No es un toast global porque este efecto corre en cada login de
          // cada usuario, y una molestia de red transitoria no debería
          // interrumpir a todo el mundo con un aviso en la pantalla raíz.
          error: (err) => console.error('No se pudo verificar si el perfil está completo', err)
        });
      } else {
        this.showProfileCompletion.set(false);
        this.userProfile.set(null);
      }
    });
  }
}
