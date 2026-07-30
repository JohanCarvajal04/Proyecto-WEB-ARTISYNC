import { Routes } from '@angular/router';
import { hasPermissionGuard } from '../../core/guards/permission.guard';

export const ADMINISTRACION_ROUTES: Routes = [
  { path: '', redirectTo: 'users', pathMatch: 'full' },
  { path: 'users', loadComponent: () => import('./pages/users/users.component').then(m => m.UsersComponent) },
  { 
    path: 'roles-permissions', 
    canActivate: [hasPermissionGuard('ROL_GESTIONAR')],
    loadComponent: () => import('./pages/roles-permissions/roles-permissions.component').then(m => m.RolesPermissionsComponent) 
  },
  { path: 'settings', loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent) }
];
