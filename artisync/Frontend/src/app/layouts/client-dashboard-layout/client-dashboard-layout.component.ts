import { Component, OnDestroy, OnInit, inject, computed, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, interval, of, startWith, switchMap, catchError } from 'rxjs';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { NavItem, navItemLinkCommands } from '../../core/config/nav.config';
import { UserMenuComponent } from '../../shared/components/user-menu/user-menu.component';
import { NotificacionService } from '../../features/comunicacion/services/notificacion.service';
import { UserService } from '../../features/perfil/services/user.service';
import { UserResponse } from '../../shared/models/user.model';
import { nombreUsuario } from '../../shared/utils/nombre-usuario';

/** Cada cuánto se refresca el contador de la campana. */
const INTERVALO_CONTEO_MS = 60_000;

@Component({
  selector: 'app-client-dashboard-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UserMenuComponent],
  templateUrl: './client-dashboard-layout.component.html'
})
export class ClientDashboardLayoutComponent implements OnInit, OnDestroy {
  authService = inject(AuthService);
  private notificacionService = inject(NotificacionService);
  private userService = inject(UserService);

  readonly isMobileMenuOpen = signal<boolean>(false);

  /** Señal compartida con el servicio: marcar leído en la lista actualiza el badge. */
  readonly noLeidas = this.notificacionService.noLeidas;

  private conteoSub?: Subscription;

  readonly perfil = signal<UserResponse | null>(null);

  ngOnInit(): void {
    this.conteoSub = interval(INTERVALO_CONTEO_MS).pipe(
      startWith(0),
      // El catchError va dentro del switchMap a propósito: si estuviera fuera,
      // un 500 puntual completaría el stream y el badge dejaría de refrescarse
      // durante toda la sesión.
      switchMap(() => this.notificacionService.contarNoLeidas().pipe(catchError(() => of(null))))
    ).subscribe();

    // Solo para mostrar el nombre real en la cabecera: el JWT decodificado
    // (AuthService.currentUser) trae únicamente email/sub, sin nombres/apellidos.
    this.userService.getCurrentUser().subscribe({
      next: (perfil) => this.perfil.set(perfil),
      error: () => {}
    });
  }

  ngOnDestroy(): void {
    this.conteoSub?.unsubscribe();
  }

  userEmail = computed(() => this.authService.currentUser()?.email || this.authService.currentUser()?.sub || 'cliente@artisync.com');
  userName = computed(() => nombreUsuario(this.perfil(), this.userEmail(), 'Cliente'));
  userRole = computed(() => this.authService.primaryRole() || 'Cliente');

  /** Mismo origen que el layout de administración: ítems del panel activo ya
   * filtrados por permiso. Este layout no filtraba nada. */
  navItems = computed<NavItem[]>(() => this.authService.visibleNavItems());

  navBasePath = computed(() => this.authService.panelBasePath());

  /** Ver `navItemLinkCommands`: evita que una `item.route` con "/" se rompa en el routerLink. */
  linkCommands(item: NavItem): string[] {
    return navItemLinkCommands(item, this.navBasePath());
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
  }
}
