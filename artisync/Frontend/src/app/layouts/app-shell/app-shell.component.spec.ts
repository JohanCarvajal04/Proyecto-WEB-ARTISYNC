import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { AppShellComponent } from './app-shell.component';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { NotificacionService } from '../../features/comunicacion/services/notificacion.service';
import { PanelId, PANEL_BASE_PATH, NAV_CATALOG } from '../../core/config/nav.config';

/** Doble de AuthService con solo lo que el shell consulta. */
function authFalso(panel: PanelId = 'creador') {
  return {
    activePanel: () => panel,
    panelBasePath: () => PANEL_BASE_PATH[panel],
    visibleNavItems: () => NAV_CATALOG.filter(i => i.panel === panel),
    primaryRole: () => 'CREADOR',
    currentUser: () => ({ email: 'ana@ejemplo.com' }),
    logout: vi.fn()
  };
}

function notificacionFalso() {
  return { noLeidas: signal(0), contarNoLeidas: vi.fn(() => of(0)) };
}

function crear(panel: PanelId = 'creador', notificacion = notificacionFalso()) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      { provide: AuthService, useValue: authFalso(panel) },
      { provide: NotificacionService, useValue: notificacion }
    ]
  });
  const fixture = TestBed.createComponent(AppShellComponent);
  fixture.detectChanges();
  return { fixture, notificacion };
}

describe('AppShellComponent', () => {
  it('fija data-panel según el panel activo', async () => {
    const { fixture } = crear('admin');
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[data-panel]').getAttribute('data-panel')).toBe('admin');
  });

  it('ningún enlace del menú apunta a /admin cuando el panel activo es creador', async () => {
    // Antes, el layout de administración cableaba `item.basePath ?? '/admin'`:
    // si ese hábito volviera a colarse en el shell unificado, el menú de
    // cualquier panel distinto de admin enlazaría a pantallas de administración.
    const { fixture } = crear('creador');
    await fixture.whenStable();

    const enlaces: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('nav a'));
    expect(enlaces.length).toBeGreaterThan(0);
    for (const a of enlaces) {
      expect(a.getAttribute('href')).not.toMatch(/^\/admin\b/);
    }
  });

  it('"Mi Cuenta" resuelve a /cuenta/configuracion en el panel de creador', async () => {
    const { fixture } = crear('creador');
    await fixture.whenStable();

    const enlaces: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('nav a'));
    const miCuenta = enlaces.find(a => a.textContent?.includes('Mi Cuenta'));
    expect(miCuenta?.getAttribute('href')).toBe('/cuenta/configuracion');
  });

  it('la campana de notificaciones respeta el panel activo', async () => {
    const { fixture } = crear('admin');
    await fixture.whenStable();

    const campana: HTMLAnchorElement = fixture.nativeElement.querySelector('header a[aria-label]');
    expect(campana.getAttribute('href')).toBe('/admin/notificaciones');
  });

  it('ngOnDestroy corta el sondeo de notificaciones', async () => {
    vi.useFakeTimers();
    const { fixture, notificacion } = crear('creador');
    await fixture.whenStable();
    notificacion.contarNoLeidas.mockClear();

    fixture.destroy();
    vi.advanceTimersByTime(120_000);

    expect(notificacion.contarNoLeidas).not.toHaveBeenCalled();
    vi.useRealTimers();
  });

  describe('fijado del menú lateral', () => {
    beforeEach(() => localStorage.removeItem('artisync.sidebar.expandido'));

    it('arranca contraído cuando no hay preferencia guardada', async () => {
      const { fixture } = crear('creador');
      await fixture.whenStable();

      expect(fixture.componentInstance.sidebarExpandido()).toBe(false);
    });

    it('toggleSidebar() persiste la preferencia en localStorage', async () => {
      // Antes el menú solo se abría con `md:hover:w-[240px]`: en una tableta
      // táctil de 768px o más no hay hover, así que quedaba permanentemente
      // colapsado sin ninguna forma de fijarlo abierto.
      const { fixture } = crear('creador');
      await fixture.whenStable();

      fixture.componentInstance.toggleSidebar();
      await fixture.whenStable();

      expect(fixture.componentInstance.sidebarExpandido()).toBe(true);
      expect(localStorage.getItem('artisync.sidebar.expandido')).toBe('1');
    });

    it('un shell recreado respeta la preferencia guardada', async () => {
      localStorage.setItem('artisync.sidebar.expandido', '1');
      const { fixture } = crear('creador');
      await fixture.whenStable();

      expect(fixture.componentInstance.sidebarExpandido()).toBe(true);
    });
  });

  describe('cajón móvil', () => {
    it('toggleMobileMenu() bloquea el scroll del body mientras está abierto', async () => {
      // Con el cajón abierto, el documento de detrás seguía haciendo scroll.
      const { fixture } = crear('creador');
      await fixture.whenStable();

      fixture.componentInstance.toggleMobileMenu();
      await fixture.whenStable();
      expect(document.body.classList.contains('overflow-hidden')).toBe(true);

      fixture.componentInstance.toggleMobileMenu();
      await fixture.whenStable();
      expect(document.body.classList.contains('overflow-hidden')).toBe(false);
    });
  });
});
