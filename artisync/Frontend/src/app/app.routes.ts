import { Routes } from '@angular/router';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';
import { hasPermissionGuard } from './core/guards/permission.guard';

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
    path: 'admin',
    canActivate: [authGuard],
    data: { roles: ['ADMINISTRADOR', 'ADMIN', 'MODERADOR', 'SOPORTE', 'AUDITOR_FINANCIERO'] },
    loadComponent: () => import('./layouts/dashboard-layout/dashboard-layout.component').then(m => m.DashboardLayoutComponent),
    loadChildren: () => import('./features/administracion/administracion.routes').then(m => m.ADMINISTRACION_ROUTES)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    data: { roles: ['CLIENTE', 'CREADOR'] },
    loadComponent: () => import('./layouts/client-dashboard-layout/client-dashboard-layout.component').then(m => m.ClientDashboardLayoutComponent),
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    path: 'creador',
    canActivate: [authGuard],
    data: { roles: ['CREADOR'] },
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
