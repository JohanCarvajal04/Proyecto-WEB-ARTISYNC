import { Component, OnDestroy, OnInit, inject, computed, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, interval, of, startWith, switchMap, catchError } from 'rxjs';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { NavItem, navItemLinkCommands } from '../../core/config/nav.config';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';
import { NotificacionService } from '../../features/comunicacion/services/notificacion.service';

/** Cada cuánto se refresca el contador de la campana. */
const INTERVALO_CONTEO_MS = 60_000;

@Component({
  selector: 'app-client-dashboard-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AvatarComponent],
  templateUrl: './client-dashboard-layout.component.html'
})
export class ClientDashboardLayoutComponent implements OnInit, OnDestroy {
  authService = inject(AuthService);
  private notificacionService = inject(NotificacionService);

  readonly isMobileMenuOpen = signal<boolean>(false);

  /** Señal compartida con el servicio: marcar leído en la lista actualiza el badge. */
  readonly noLeidas = this.notificacionService.noLeidas;

  private conteoSub?: Subscription;

  ngOnInit(): void {
    this.conteoSub = interval(INTERVALO_CONTEO_MS).pipe(
      startWith(0),
      // El catchError va dentro del switchMap a propósito: si estuviera fuera,
      // un 500 puntual completaría el stream y el badge dejaría de refrescarse
      // durante toda la sesión.
      switchMap(() => this.notificacionService.contarNoLeidas().pipe(catchError(() => of(null))))
    ).subscribe();
  }

  ngOnDestroy(): void {
    this.conteoSub?.unsubscribe();
  }

  userEmail = computed(() => this.authService.currentUser()?.email || this.authService.currentUser()?.sub || 'cliente@artisync.com');
  userName = computed(() => {
    const email = this.userEmail();
    const prefix = email.split('@')[0];
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });
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
