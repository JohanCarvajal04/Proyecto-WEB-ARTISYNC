import { Component, input } from '@angular/core';
import { AvatarComponent } from '../avatar/avatar.component';

/**
 * Cabecera simple de perfil: banner + avatar + nombre/email, sin el detalle
 * de roles/permisos/sesión (eso vive en Mi Cuenta, ver app-identity-card).
 * Usada en Mi Perfil (Creador) y Mi Perfil (Cliente), que solo necesitan
 * identificación visual.
 */
@Component({
  selector: 'app-profile-header',
  standalone: true,
  imports: [AvatarComponent],
  templateUrl: './profile-header.component.html'
})
export class ProfileHeaderComponent {
  nombre = input.required<string>();
  correo = input.required<string>();
  fotoUrl = input<string | null | undefined>(null);
  /** Insignia opcional (ej. "Creador") mostrada junto al nombre. */
  rolBadge = input<string | null>(null);
}
