import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const PEDIDO_ROUTES: Routes = [
  {
    path: 'mis-pedidos',
    loadComponent: () => import('./pages/pedidos-lista/pedidos-lista.component').then(m => m.PedidosListaComponent),
    canActivate: [authGuard],
    data: { roles: ['CLIENTE', 'ADMIN', 'CREADOR'], modo: 'cliente' }
  },
  {
    path: 'mis-comisiones',
    loadComponent: () => import('./pages/pedidos-lista/pedidos-lista.component').then(m => m.PedidosListaComponent),
    canActivate: [authGuard],
    data: { roles: ['CREADOR', 'ADMIN'], modo: 'creador' }
  },
  {
    path: 'crear',
    loadComponent: () => import('./pages/pedido-crear/pedido-crear.component').then(m => m.PedidoCrearComponent),
    canActivate: [authGuard],
    data: { roles: ['CLIENTE', 'ADMIN'] }
  },
  {
    path: 'flujos',
    loadComponent: () => import('./pages/flujos-admin/flujos-admin.component').then(m => m.FlujosAdminComponent),
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'ADMINISTRADOR'] }
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/pedido-detalle/pedido-detalle.component').then(m => m.PedidoDetalleComponent),
    canActivate: [authGuard]
  },
  { path: '', redirectTo: 'mis-pedidos', pathMatch: 'full' }
];
