import { Routes } from '@angular/router';

/**
 * Comunicación (M6). El chat y el briefing no tienen ruta propia: se embeben
 * como componentes dentro del detalle del pedido, que es su contexto.
 */
export const COMUNICACION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/notificaciones/notificaciones.component').then(m => m.NotificacionesComponent)
  }
];
