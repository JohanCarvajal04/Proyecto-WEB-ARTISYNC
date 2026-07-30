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
    path: 'no-autorizado',
    loadComponent: () => import('./pages/public/unauthorized/unauthorized.component').then(m => m.UnauthorizedComponent)
  },
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/auth/login' }
];
