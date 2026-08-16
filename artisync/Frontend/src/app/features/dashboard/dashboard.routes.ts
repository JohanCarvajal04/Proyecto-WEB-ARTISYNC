import { Routes } from '@angular/router';

export const DASHBOARD_ROUTES: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  {
    path: 'overview',
    loadComponent: () => import('./pages/overview/overview.component').then(m => m.OverviewComponent)
  },
  {
    path: 'mis-pedidos',
    loadComponent: () => import('./pages/mis-pedidos/mis-pedidos-dashboard.component').then(m => m.MisPedidosDashboardComponent)
  },
  {
    path: 'seguimiento/:id',
    loadComponent: () => import('./pages/seguimiento/seguimiento.component').then(m => m.SeguimientoComponent)
  },
  {
    path: 'perfil',
    loadComponent: () => import('./pages/perfil/perfil-cliente.component').then(m => m.PerfilClienteComponent)
  },
  // Catálogo público (M3): explorar, ficha de servicio y perfil del creador.
  {
    path: 'explorar',
    loadChildren: () => import('../catalogo/catalogo.routes').then(m => m.CATALOGO_ROUTES)
  },
  // Centro de notificaciones (M6).
  {
    path: 'notificaciones',
    loadChildren: () => import('../comunicacion/comunicacion.routes').then(m => m.COMUNICACION_ROUTES)
  },
  // Sorteos abiertos a participación (M7).
  {
    path: 'sorteos',
    loadChildren: () => import('../social/social.routes').then(m => m.SOCIAL_ROUTES)
  }
];
