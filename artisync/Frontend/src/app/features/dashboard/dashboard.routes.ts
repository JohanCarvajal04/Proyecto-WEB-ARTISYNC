import { Routes } from '@angular/router';
import { CATALOGO_BASE_PATH } from '../catalogo/catalogo.config';
import { authGuard } from '../../core/guards/auth.guard';
import { PAGE_PERMISSIONS } from '../../core/config/nav.config';

export const DASHBOARD_ROUTES: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  {
    path: 'overview',
    loadComponent: () => import('./pages/overview/overview.component').then(m => m.OverviewComponent)
  },
  {
    // Antes sin guard, pese a llamar al mismo listarMisPedidos() que
    // /pedido/mis-pedidos (que sí exigía este permiso): la comprobación de
    // la otra ruta era decorativa mientras esta quedara abierta.
    path: 'mis-pedidos',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.pedidosCliente },
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
  // El mismo árbol de rutas se monta también sin sesión bajo /explorar (ver
  // app.routes.ts); aquí se provee el prefijo de este montaje para que los
  // routerLink internos del catálogo se queden dentro del panel.
  {
    path: 'explorar',
    providers: [{ provide: CATALOGO_BASE_PATH, useValue: '/dashboard/explorar' }],
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
