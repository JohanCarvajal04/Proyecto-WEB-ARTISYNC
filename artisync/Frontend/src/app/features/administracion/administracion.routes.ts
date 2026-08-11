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
  { path: 'settings', loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent) },

  // ─── Moderador ───
  {
    path: 'mod-overview',
    loadComponent: () => import('./pages/mod-overview/mod-overview.component').then(m => m.ModOverviewComponent)
  },
  {
    path: 'verificaciones',
    canActivate: [hasPermissionGuard('CERTIFICADO_REVISAR')],
    loadComponent: () => import('./pages/verificaciones/verificaciones.component').then(m => m.VerificacionesComponent)
  },
  {
    path: 'mod-portafolios',
    canActivate: [hasPermissionGuard('PORTAFOLIO_MODERAR')],
    loadComponent: () => import('./pages/mod-portafolios/mod-portafolios.component').then(m => m.ModPortafoliosComponent)
  },
  {
    path: 'mod-comentarios',
    canActivate: [hasPermissionGuard('COMENTARIO_MODERAR')],
    loadComponent: () => import('./pages/mod-comentarios/mod-comentarios.component').then(m => m.ModComentariosComponent)
  },
  {
    path: 'mod-categorias',
    canActivate: [hasPermissionGuard('CATEGORIA_GESTIONAR')],
    loadComponent: () => import('./pages/mod-categorias/mod-categorias.component').then(m => m.ModCategoriasComponent)
  }
];
