import { Routes } from '@angular/router';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';
import { ADMIN_PANEL_PERMISSIONS, CREADOR_PANEL_PERMISSIONS } from './core/config/nav.config';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/seguridad/seguridad.routes').then(m => m.SEGURIDAD_ROUTES)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadChildren: () => import('./features/perfil/perfil.routes').then(m => m.PERFIL_ROUTES)
  },
  {
    // La puerta del panel era una lista fija de cinco nombres de rol, así que
    // un rol creado por el administrador no podía entrar aunque se le hubieran
    // concedido permisos administrativos. Ahora basta con tener alguno: qué
    // páginas ve dentro lo deciden los guards de cada ruta hija.
    path: 'admin',
    canActivate: [authGuard],
    data: { permissions: ADMIN_PANEL_PERMISSIONS },
    loadComponent: () => import('./layouts/dashboard-layout/dashboard-layout.component').then(m => m.DashboardLayoutComponent),
    loadChildren: () => import('./features/administracion/administracion.routes').then(m => m.ADMINISTRACION_ROUTES)
  },
  {
    // Panel base de la aplicación (catálogo público, pedidos propios, perfil):
    // cualquier usuario autenticado. Antes exigía el rol CLIENTE o CREADOR, lo
    // que dejaba fuera a cualquier rol nuevo sin necesidad.
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./layouts/client-dashboard-layout/client-dashboard-layout.component').then(m => m.ClientDashboardLayoutComponent),
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    path: 'creador',
    canActivate: [authGuard],
    data: { permissions: CREADOR_PANEL_PERMISSIONS },
    loadComponent: () => import('./layouts/client-dashboard-layout/client-dashboard-layout.component').then(m => m.ClientDashboardLayoutComponent),
    loadChildren: () => import('./features/creador/creador.routes').then(m => m.CREADOR_ROUTES)
  },
  {
    path: 'pedido',
    canActivate: [authGuard],
    loadChildren: () => import('./features/pedido/pedido.routes').then(m => m.PEDIDO_ROUTES)
  },
  {
    path: 'legal',
    canActivate: [authGuard],
    loadChildren: () => import('./features/legal/legal.routes').then(m => m.LEGAL_ROUTES)
  },
  {
    path: 'no-autorizado',
    loadComponent: () => import('./pages/public/unauthorized/unauthorized.component').then(m => m.UnauthorizedComponent)
  },
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/auth/login' }
];
