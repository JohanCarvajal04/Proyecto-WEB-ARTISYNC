import { Routes } from '@angular/router';
import { hasPermissionGuard } from '../../core/guards/permission.guard';
import { authGuard } from '../../core/guards/auth.guard';

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
  },
  {
    path: 'paises',
    canActivate: [hasPermissionGuard('PAIS_VER')],
    loadComponent: () => import('./pages/paises/paises.component').then(m => m.PaisesComponent)
  },
  // Centro de notificaciones: los endpoints son `isAuthenticated()`, así que
  // aplica a cualquier rol del panel, no solo al moderador.
  {
    path: 'notificaciones',
    loadChildren: () => import('../comunicacion/comunicacion.routes').then(m => m.COMUNICACION_ROUTES)
  },
  // Infracciones: los tres endpoints son hasRole('ADMIN'), no hay permiso
  // granular que delegar, así que se guarda por rol.
  {
    path: 'infracciones',
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'ADMINISTRADOR'] },
    loadComponent: () => import('./pages/infracciones/infracciones.component').then(m => m.InfraccionesComponent)
  },
  // La pantalla vive en el módulo de pedidos (M4) pero solo la usa el admin;
  // montarla aquí la hace alcanzable desde el nav, que la concatena con /admin.
  {
    path: 'flujos',
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'ADMINISTRADOR'] },
    loadComponent: () => import('../pedido/pages/flujos-admin/flujos-admin.component').then(m => m.FlujosAdminComponent)
  }
];
