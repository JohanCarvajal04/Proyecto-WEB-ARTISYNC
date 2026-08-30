import { InjectionToken } from '@angular/core';

/**
 * Prefijo con el que los componentes del catálogo (`explorar`, `servicio-detalle`,
 * `creador-publico`) construyen sus `routerLink` internos.
 *
 * El mismo árbol de rutas (`CATALOGO_ROUTES`) se monta en dos sitios distintos:
 * bajo `/explorar` (cascarón público, para visitantes sin sesión) y bajo
 * `/dashboard/explorar` (cascarón autenticado, para el panel de cliente). Los
 * componentes no deben saber cuál de los dos los está sirviendo — cada padre de
 * ruta provee este token con su propio prefijo, y las plantillas lo usan en vez
 * de escribir la base a mano.
 *
 * El `factory` por defecto apunta al montaje público: así un test que monte
 * alguno de estos componentes suelto (sin pasar por app.routes.ts) no necesita
 * proveerlo explícitamente.
 */
export const CATALOGO_BASE_PATH = new InjectionToken<string>('CATALOGO_BASE_PATH', {
  factory: () => '/explorar'
});
