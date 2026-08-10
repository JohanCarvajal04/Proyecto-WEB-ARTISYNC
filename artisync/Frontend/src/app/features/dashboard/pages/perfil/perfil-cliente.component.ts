import { Component, inject, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../seguridad/services/auth.service';
import { AvatarComponent } from '../../../../shared/components/avatar/avatar.component';

@Component({
  selector: 'app-perfil-cliente',
  standalone: true,
  imports: [RouterLink, AvatarComponent],
  templateUrl: './perfil-cliente.component.html'
})
export class PerfilClienteComponent {
  authService = inject(AuthService);

  userEmail = computed(() =>
    this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  userName = computed(() => {
    const email = this.userEmail();
    const prefix = email.split('@')[0];
    if (!prefix || prefix === '—') return 'Usuario';
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });

  userRole = computed(() => this.authService.primaryRole() || 'CLIENTE');

  roles = computed(() => this.authService.userRoles().map(r => r.replace('ROLE_', '')));

  totalPermisos = computed(() => this.authService.userPermissions().length);

  sesionExpira = computed(() => {
    const exp = this.authService.currentUser()?.exp;
    if (!exp) return null;
    return new Date(exp * 1000).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  });

  logout(): void {
    this.authService.logout();
  }
}
