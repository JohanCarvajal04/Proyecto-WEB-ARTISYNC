import { Routes } from '@angular/router';

/**
 * Panel de cuenta: la única zona alcanzable sin ningún permiso asignado.
 *
 * Es el destino al que `resolvePanel()` manda a un usuario sin permisos —
 * un rol recién creado desde "Roles y Permisos" al que aún no se le ha
 * asignado nada, o un rol conocido (p. ej. MODERADOR) al que se le han
 * retirado todos. Antes ese usuario se colaba en el panel de CLIENTE (rol
 * personalizado) o quedaba atrapado en un rebote contra la puerta de su
 * propio panel (rol conocido): ver nav.config.ts#resolvePanel.
 *
 * No depende de PAGE_PERMISSIONS: nadie necesita un permiso para ver sus
 * propias notificaciones o gestionar su cuenta.
 */
export const CUENTA_ROUTES: Routes = [
  { path: '', redirectTo: 'notificaciones', pathMatch: 'full' },
  {
    path: 'notificaciones',
    loadChildren: () => import('../comunicacion/comunicacion.routes').then(m => m.COMUNICACION_ROUTES)
  },
  {
    path: 'configuracion',
    loadComponent: () => import('./pages/configuracion/configuracion-cuenta.component').then(m => m.ConfiguracionCuentaComponent)
  }
];
