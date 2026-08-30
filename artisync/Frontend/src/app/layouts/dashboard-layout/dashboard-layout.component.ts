import { Component, OnDestroy, OnInit, inject, computed, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, interval, of, startWith, switchMap, catchError } from 'rxjs';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { NavItem, navItemLinkCommands, PANEL_BASE_PATH } from '../../core/config/nav.config';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';
import { NotificacionService } from '../../features/comunicacion/services/notificacion.service';

/** Cada cuánto se refresca el contador de la campana. */
const INTERVALO_CONTEO_MS = 60_000;

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AvatarComponent],
  templateUrl: './dashboard-layout.component.html'
})
export class DashboardLayoutComponent implements OnInit, OnDestroy {
  authService = inject(AuthService);
  private notificacionService = inject(NotificacionService);

  readonly isMobileMenuOpen = signal<boolean>(false);

  /** Señal compartida con el servicio: marcar leído actualiza el badge. */
  readonly noLeidas = this.notificacionService.noLeidas;

  private conteoSub?: Subscription;

  ngOnInit(): void {
    // catchError dentro del switchMap: fuera, un error puntual completaría el
    // stream y el badge dejaría de refrescarse durante toda la sesión.
    this.conteoSub = interval(INTERVALO_CONTEO_MS).pipe(
      startWith(0),
      switchMap(() => this.notificacionService.contarNoLeidas().pipe(catchError(() => of(null))))
    ).subscribe();
  }

  ngOnDestroy(): void {
    this.conteoSub?.unsubscribe();
  }

  userEmail = computed(() => this.authService.currentUser()?.email || this.authService.currentUser()?.sub || 'admin@artisync.com');
  userName = computed(() => {
    const email = this.userEmail();
    const prefix = email.split('@')[0];
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });
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
