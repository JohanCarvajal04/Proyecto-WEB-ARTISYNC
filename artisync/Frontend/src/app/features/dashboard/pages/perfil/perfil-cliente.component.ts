import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ProfileHeaderComponent } from '../../../../shared/components/profile-header/profile-header.component';
import { SolicitudVerificacionComponent } from '../../../perfil/components/solicitud-verificacion/solicitud-verificacion.component';
import { UserService } from '../../../perfil/services/user.service';
import { UserResponse } from '../../../../shared/models/user.model';
import { nombreUsuario } from '../../../../shared/utils/nombre-usuario';

@Component({
  selector: 'app-perfil-cliente',
  standalone: true,
  imports: [RouterLink, ProfileHeaderComponent, SolicitudVerificacionComponent],
  templateUrl: './perfil-cliente.component.html'
})
export class PerfilClienteComponent implements OnInit {
  authService = inject(AuthService);
  private userService = inject(UserService);

  readonly usuario = signal<UserResponse | null>(null);
  readonly isLoading = signal<boolean>(true);

  userEmail = computed(() =>
    this.usuario()?.correo || this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  userName = computed(() => nombreUsuario(this.usuario(), this.userEmail()));

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (u) => {
        this.usuario.set(u);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }
}
