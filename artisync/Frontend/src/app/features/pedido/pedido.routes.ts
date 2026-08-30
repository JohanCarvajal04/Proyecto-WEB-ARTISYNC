import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { PAGE_PERMISSIONS } from '../../core/config/nav.config';

/**
 * Guardadas por permiso, no por nombre de rol: un rol nuevo al que se le
 * conceda PEDIDO_CREAR o PEDIDO_GESTIONAR entra sin tener que tocar el router.
 */
export const PEDIDO_ROUTES: Routes = [
  // PedidosListaComponent duplicaba, con un mismo componente conmutado por
  // `modo`, a MisPedidosDashboardComponent (panel cliente) y a
  // ComisionesComponent (panel creador) -- dos listas de pedidos paralelas
  // que llamaban al mismo endpoint pero nunca se devolvían la una a la otra.
  // Se retiró; estas dos rutas quedan como redirect por compatibilidad de
  // enlaces existentes. Cada destino ya lleva su propio guard de permisos.
  { path: 'mis-pedidos', redirectTo: '/dashboard/mis-pedidos', pathMatch: 'full' },
  { path: 'mis-comisiones', redirectTo: '/creador/comisiones', pathMatch: 'full' },
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
  { path: '', redirectTo: '/dashboard/mis-pedidos', pathMatch: 'full' }
];
