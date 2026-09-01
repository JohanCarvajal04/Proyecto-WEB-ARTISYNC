import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../features/seguridad/services/auth.service';

/**
 * Cascarón para visitantes sin sesión: header ligero + `<router-outlet>`, sin
 * sidebar ni sondeo de notificaciones (a diferencia de AppShellComponent, que
 * cubre toda ruta autenticada). Sirve al catálogo público montado bajo
 * `/explorar` en app.routes.ts.
 *
 * Un usuario que YA tiene sesión y abre `/explorar` también ve este cascarón
 * (la raíz `/explorar` no lleva authGuard): el header se lo señala mostrando
 * "Ir a mi panel" en vez de los botones de acceso.
 */
@Component({
  selector: 'app-public-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  templateUrl: './public-shell.component.html'
})
export class PublicShellComponent {
  private readonly authService = inject(AuthService);

  readonly isLoggedIn = computed(() => this.authService.isLoggedIn());
  readonly homeRoute = computed(() => this.authService.homeRoute());

  readonly currentYear = new Date().getFullYear();
}
