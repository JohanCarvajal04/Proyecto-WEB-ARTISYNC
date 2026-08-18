import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { Router, UrlTree, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect, vi } from 'vitest';

import { authGuard } from './auth.guard';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { ToastService } from '../services/toast.service';
import { ADMIN_PANEL_PERMISSIONS } from '../config/nav.config';

/** Doble de AuthService con solo lo que el guard consulta. */
function authFalso(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    waitForSessionRestore: () => Promise.resolve(),
    isLoggedIn: () => true,
    userRoles: () => ['ROLE_MODERADOR'],
    hasAnyPermission: () => false,
    homeRoute: () => '/admin/mod-overview',
    ...overrides
  };
}

function ejecutar(auth: unknown, data: Record<string, unknown>, url = '/admin/paises') {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      { provide: AuthService, useValue: auth }
    ]
  });

  const route = { data } as unknown as ActivatedRouteSnapshot;
  const stateSnapshot = { url } as RouterStateSnapshot;

  return TestBed.runInInjectionContext(() => authGuard(route, stateSnapshot)) as Promise<boolean | UrlTree>;
}

describe('authGuard — denegación por permiso', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('deja pasar cuando el usuario tiene alguno de los permisos requeridos', async () => {
    const auth = authFalso({ hasAnyPermission: () => true });
    const resultado = await ejecutar(auth, { permissions: ['PAIS_VER', 'PAIS_EDITAR'] });

    expect(resultado).toBe(true);
  });

  it('devuelve al inicio del usuario en vez de a /no-autorizado', async () => {
    // La denegación corriente ya no es un callejón sin salida: se vuelve a la
    // primera página que el usuario sí puede ver.
    const auth = authFalso();
    const resultado = await ejecutar(auth, { permissions: ['PAIS_VER'] });

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(String(resultado)).toBe('/admin/mod-overview');
  });

  it('nombra la sección por su etiqueta, nunca por códigos de permiso', async () => {
    const auth = authFalso();
    await ejecutar(auth, { permissions: ['PAIS_VER'] }, '/admin/paises');

    const mensaje = TestBed.inject(ToastService).toasts()[0]?.message ?? '';
    expect(mensaje).toContain('Gestión de Países');
    expect(mensaje).not.toContain('PAIS_VER');
  });

  it('no filtra ningún código de autorización en el aviso', async () => {
    // La puerta del panel exige "cualquiera de" casi treinta permisos: además de
    // ser nomenclatura interna, enumerarlos producía un aviso ilegible.
    const auth = authFalso();
    await ejecutar(auth, { permissions: [...ADMIN_PANEL_PERMISSIONS] }, '/admin/users');

    const mensaje = TestBed.inject(ToastService).toasts()[0]?.message ?? '';
    for (const permiso of ADMIN_PANEL_PERMISSIONS) {
      expect(mensaje, `el aviso revela ${permiso}`).not.toContain(permiso);
    }
    expect(mensaje.length).toBeLessThan(160);
  });

  it('cae en un texto genérico si la ruta no está en el catálogo', async () => {
    const auth = authFalso();
    await ejecutar(auth, { permissions: ['ALGO'] }, '/admin/pantalla-desconocida');

    expect(TestBed.inject(ToastService).toasts()[0]?.message).toContain('a esta sección');
  });

  it('cae en /no-autorizado solo si el usuario no tiene ninguna página accesible', async () => {
    const auth = authFalso({ homeRoute: () => '/no-autorizado' });
    const resultado = await ejecutar(auth, { permissions: ['PAIS_VER'] });

    expect(String(resultado)).toBe('/no-autorizado');
  });

  it('no entra en bucle si el destino de respaldo es la propia ruta denegada', async () => {
    const auth = authFalso({ homeRoute: () => '/admin/paises' });
    const resultado = await ejecutar(auth, { permissions: ['PAIS_VER'] }, '/admin/paises');

    expect(String(resultado)).toBe('/no-autorizado');
  });

  it('deja pasar a ADMIN aunque le falte el permiso, igual que el backend', async () => {
    const auth = authFalso({ userRoles: () => ['ROLE_ADMIN'] });
    const resultado = await ejecutar(auth, { permissions: ['PERMISO_INEXISTENTE'] });

    expect(resultado).toBe(true);
  });

  it('manda al login conservando returnUrl si no hay sesión', async () => {
    const auth = authFalso({ isLoggedIn: () => false });
    const router = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router }
      ]
    });

    const route = { data: {} } as unknown as ActivatedRouteSnapshot;
    const snapshot = { url: '/admin/paises' } as RouterStateSnapshot;
    const resultado = await TestBed.runInInjectionContext(() => authGuard(route, snapshot));

    expect(resultado).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login'], {
      queryParams: { returnUrl: '/admin/paises' }
    });
  });
});
