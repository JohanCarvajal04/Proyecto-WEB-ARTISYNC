import { Routes } from '@angular/router';

/**
 * Catálogo público (M3). Se monta bajo `/dashboard/explorar` para que quede
 * dentro del layout y la navegación del cliente.
 */
export const CATALOGO_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/explorar/explorar.component').then(m => m.ExplorarComponent)
  },
  {
    path: 'creadores',
    loadComponent: () => import('./pages/creadores/creadores.component').then(m => m.CreadoresComponent)
  },
  {
    path: 'servicio/:id',
    loadComponent: () => import('./pages/servicio-detalle/servicio-detalle.component').then(m => m.ServicioDetalleComponent)
  },
  {
    path: 'creador/:idPerfil',
    loadComponent: () => import('./pages/creador-publico/creador-publico.component').then(m => m.CreadorPublicoComponent)
  }
];
