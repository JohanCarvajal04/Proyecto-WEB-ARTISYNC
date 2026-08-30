import { describe, expect, it } from 'vitest';
import { Route } from '@angular/router';
import { routes } from './app.routes';
import { ADMIN_PANEL_PERMISSIONS, CREADOR_PANEL_PERMISSIONS } from './core/config/nav.config';
import { guestGuard } from './core/guards/guest.guard';

/** Ramas que exigen sesión y, por tanto, deben renderizarse con encabezado y menú. */
const RAMAS_AUTENTICADAS = ['admin', 'dashboard', 'creador', 'cuenta', 'pedido', 'legal', 'profile'];

const cascaron = routes.find(r => r.path === '' && r.children) as Route;

describe('árbol de rutas raíz', () => {
  it('existe el cascarón y monta toda rama autenticada bajo él', () => {
    // La regresión original: /pedido, /legal y /profile colgaban de la raíz
    // como hermanas de admin/dashboard/creador/cuenta, así que se pintaban
    // sin encabezado ni menú lateral.
    expect(cascaron).toBeDefined();
    expect(cascaron.children!.map(c => c.path).sort()).toEqual([...RAMAS_AUTENTICADAS].sort());
  });

  it('fuera del cascarón solo quedan las rutas sin sesión', () => {
    const fuera = routes.filter(r => r !== cascaron).map(r => r.path);
    expect(fuera).toEqual(['auth', 'no-autorizado', 'acceso-requerido', 'explorar', '', '**']);
  });

  it('el catálogo público (/explorar) no lleva guard ni cuelga del cascarón', () => {
    // Debe ser navegable sin sesión: si llevara authGuard, o si colgara del
    // cascarón autenticado, un visitante anónimo rebotaría a /auth/login antes
    // de ver un solo servicio.
    const explorar = routes.find(r => r.path === 'explorar')!;
    expect(explorar.canActivate).toBeUndefined();
    expect(cascaron.children!.some(c => c.path === 'explorar')).toBe(false);
  });

  it('/acceso-requerido no lleva guard', () => {
    // Es el destino AL QUE se manda a un visitante sin sesión (ver
    // exigirSesion()); si llevara authGuard, sería inalcanzable para quien
    // más la necesita.
    const accesoRequerido = routes.find(r => r.path === 'acceso-requerido')!;
    expect(accesoRequerido.canActivate).toBeUndefined();
  });

  it('el redirect de la raíz precede al cascarón', () => {
    // El cascarón es `path: ''` sin pathMatch: casaría con la URL vacía y
    // fallaría al no encontrar hijo, dejando la aplicación en blanco en vez
    // de ir al login.
    const iRedirect = routes.findIndex(r => r.path === '' && r.pathMatch === 'full');
    expect(iRedirect).toBeGreaterThanOrEqual(0);
    expect(iRedirect).toBeLessThan(routes.indexOf(cascaron));
  });

  it('el cascarón no declara data', () => {
    // Con paramsInheritanceStrategy 'emptyOnly' (el valor por defecto), un
    // padre de path vacío propaga su data a TODOS sus hijos: unos permisos
    // aquí se le exigirían también a /legal y /profile.
    expect(cascaron.data).toBeUndefined();
  });

  it('solo admin y creador exigen permisos de panel', () => {
    const conPermisos = cascaron.children!
      .filter(c => c.data?.['permissions'])
      .map(c => [c.path, c.data!['permissions']]);
    expect(conPermisos).toEqual([
      ['admin', ADMIN_PANEL_PERMISSIONS],
      ['creador', CREADOR_PANEL_PERMISSIONS]
    ]);
  });

  it('toda rama con permisos de panel lleva su propio guard', () => {
    // Los permisos los consume authGuard leyendo route.data: sin guard en la
    // MISMA ruta, el data es decorativo.
    for (const hijo of cascaron.children!.filter(c => c.data?.['permissions'])) {
      expect(hijo.canActivate?.length, hijo.path).toBeGreaterThan(0);
    }
  });

  it('las rutas públicas no exigen sesión', () => {
    const auth = routes.find(r => r.path === 'auth')!;
    const noAutorizado = routes.find(r => r.path === 'no-autorizado')!;
    // 'auth' lleva guestGuard (lo contrario de authGuard: rechaza a quien ya
    // tiene sesión), y 'no-autorizado' no lleva ningún guard.
    expect(auth.canActivate).toEqual([guestGuard]);
    expect(noAutorizado.canActivate).toBeUndefined();
  });
});
