import { Component, OnDestroy, OnInit, inject, computed, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, interval, of, startWith, switchMap, catchError } from 'rxjs';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { NavItem, navItemLinkCommands, PANEL_BASE_PATH } from '../../core/config/nav.config';
import { UserMenuComponent } from '../../shared/components/user-menu/user-menu.component';
import { NotificacionService } from '../../features/comunicacion/services/notificacion.service';
import { UserService } from '../../features/perfil/services/user.service';
import { UserResponse } from '../../shared/models/user.model';
import { nombreUsuario } from '../../shared/utils/nombre-usuario';

/** Cada cuánto se refresca el contador de la campana. */
const INTERVALO_CONTEO_MS = 60_000;

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UserMenuComponent],
  templateUrl: './dashboard-layout.component.html'
})
export class DashboardLayoutComponent implements OnInit, OnDestroy {
  authService = inject(AuthService);
  private notificacionService = inject(NotificacionService);
  private userService = inject(UserService);

  readonly isMobileMenuOpen = signal<boolean>(false);

  /** Señal compartida con el servicio: marcar leído actualiza el badge. */
  readonly noLeidas = this.notificacionService.noLeidas;

  private conteoSub?: Subscription;

  readonly perfil = signal<UserResponse | null>(null);

  ngOnInit(): void {
    // catchError dentro del switchMap: fuera, un error puntual completaría el
    // stream y el badge dejaría de refrescarse durante toda la sesión.
    this.conteoSub = interval(INTERVALO_CONTEO_MS).pipe(
      startWith(0),
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

  userEmail = computed(() => this.authService.currentUser()?.email || this.authService.currentUser()?.sub || 'admin@artisync.com');
  userName = computed(() => nombreUsuario(this.perfil(), this.userEmail(), 'Administrador'));
  userRole = computed(() => this.authService.primaryRole() || 'Administrador');

  /**
   * El filtrado vive en AuthService para que ambos layouts apliquen la misma
   * regla. Antes, un rol sin entrada en NAV_CONFIG caía al menú completo de
   * ADMINISTRADOR: enseñaba todas las pantallas de administración a un rol que
   * no tenía esos permisos.
   */
  navItems = computed<NavItem[]>(() => this.authService.visibleNavItems());

  /** Ver `navItemLinkCommands`: evita que una `item.route` con "/" se rompa en el routerLink. */
  linkCommands(item: NavItem): string[] {
    return navItemLinkCommands(item, PANEL_BASE_PATH.admin);
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
  }
}
