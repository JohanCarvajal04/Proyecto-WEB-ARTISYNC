import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { PAGE_PERMISSIONS } from '../../core/config/nav.config';

/**
 * Guardadas por permiso, no por nombre de rol: un rol nuevo al que se le
 * conceda PEDIDO_CREAR o PEDIDO_GESTIONAR entra sin tener que tocar el router.
 * `modo` no es autorización, solo le dice al componente qué listado pintar.
 */
export const PEDIDO_ROUTES: Routes = [
  {
    path: 'mis-pedidos',
    loadComponent: () => import('./pages/pedidos-lista/pedidos-lista.component').then(m => m.PedidosListaComponent),
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.pedidosCliente, modo: 'cliente' }
  },
  {
    path: 'mis-comisiones',
    loadComponent: () => import('./pages/pedidos-lista/pedidos-lista.component').then(m => m.PedidosListaComponent),
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.comisiones, modo: 'creador' }
  },
  {
    path: 'crear',
    loadComponent: () => import('./pages/pedido-crear/pedido-crear.component').then(m => m.PedidoCrearComponent),
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.pedidoCrear }
  },
  {
    path: 'flujos',
    loadComponent: () => import('./pages/flujos-admin/flujos-admin.component').then(m => m.FlujosAdminComponent),
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.flujos }
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/pedido-detalle/pedido-detalle.component').then(m => m.PedidoDetalleComponent),
    canActivate: [authGuard]
  },
  { path: '', redirectTo: 'mis-pedidos', pathMatch: 'full' }
];
