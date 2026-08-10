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
  }
];
