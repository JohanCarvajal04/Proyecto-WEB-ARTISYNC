import { Component, OnDestroy, OnInit, inject, computed, signal, effect } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, interval, of, startWith, switchMap, catchError } from 'rxjs';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { NavItem } from '../../core/config/nav.config';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';
import { NavIconComponent } from '../../shared/components/nav-icon/nav-icon.component';
import { NotificacionService } from '../../features/comunicacion/services/notificacion.service';
import { SHELL_COPY } from './app-shell.config';

/** Cada cuánto se refresca el contador de la campana. */
const INTERVALO_CONTEO_MS = 60_000;

/** Clave del ancho fijado del menú lateral en localStorage. */
const CLAVE_SIDEBAR = 'artisync.sidebar.expandido';

/**
 * Cascarón único de la aplicación: header + sidebar de toda ruta autenticada.
 *
 * Sustituye a los dos layouts que existían antes (DashboardLayoutComponent y
 * ClientDashboardLayoutComponent), que eran estructuralmente idénticos y solo
 * diferían en paleta, iconos y la base del routerLink. La paleta por panel
 * vive en src/styles.css como variables CSS sobre `[data-panel]`; este
 * componente solo fija el atributo `data-panel` en la raíz.
 *
 * Además, al montarse en un padre de ruta sin `path` (ver app.routes.ts), este
 * componente cubre TODA ruta autenticada — incluidas /pedido, /legal y
 * /profile, que antes se renderizaban sin cascarón.
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AvatarComponent, NavIconComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css',
  host: { '(document:keydown.escape)': 'isMobileMenuOpen.set(false)' }
})
export class AppShellComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);

  /** Cajón lateral en móvil (<768px). Independiente del fijado de escritorio:
   *  en móvil el ancho siempre es w-64 y lo que se alterna es el translate. */
  readonly isMobileMenuOpen = signal<boolean>(false);

  /** Menú fijado abierto en escritorio. Hasta ahora el sidebar solo se abría
   *  con `md:hover:w-[240px]`: en una tableta táctil de 768px o más no hay
   *  hover, así que el menú quedaba permanentemente en 72px con las
   *  etiquetas a `md:opacity-0` — iconos sin nombre y sin forma de leerlos. */
  readonly sidebarExpandido = signal<boolean>(localStorage.getItem(CLAVE_SIDEBAR) === '1');

  /** Señal compartida con el servicio: marcar leído en la lista actualiza el badge. */
  readonly noLeidas = this.notificacionService.noLeidas;

  private conteoSub?: Subscription;

  constructor() {
    effect(() => localStorage.setItem(CLAVE_SIDEBAR, this.sidebarExpandido() ? '1' : '0'));
    // Con el cajón móvil abierto, el documento de detrás seguía haciendo scroll.
    effect(() => document.body.classList.toggle('overflow-hidden', this.isMobileMenuOpen()));
  }

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

  readonly panel = computed(() => this.authService.activePanel());
  readonly navBasePath = computed(() => this.authService.panelBasePath());
  readonly copy = computed(() => SHELL_COPY[this.panel()]);

  /** 'Mi cuenta' y no un correo de ejemplo con dominio real: es un respaldo de
   *  emergencia para cuando el JWT no trae ni `email` ni `sub`, no debería
   *  aparentar ser una cuenta real. */
  userEmail = computed(() => this.authService.currentUser()?.email || this.authService.currentUser()?.sub || 'Mi cuenta');
  userName = computed(() => {
    const email = this.userEmail();
    const prefix = email.split('@')[0];
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });
  userRole = computed(() => this.authService.primaryRole() || this.copy().rolPorDefecto);

  /** Ítems del panel activo, ya filtrados por permiso (AuthService aplica la
   *  misma regla para los cuatro paneles). */
  navItems = computed<NavItem[]>(() => this.authService.visibleNavItems());

  toggleSidebar(): void {
    this.sidebarExpandido.update(v => !v);
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
  }
}
