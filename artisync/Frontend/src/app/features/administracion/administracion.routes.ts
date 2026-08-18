import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { PAGE_PERMISSIONS } from '../../core/config/nav.config';

/**
 * Todas las rutas se guardan con `authGuard` + `data.permissions`. Antes
 * convivían dos mecanismos: `hasPermissionGuard('X')` (síncrono, un solo
 * permiso, sin esperar a que se restaurase la sesión) y `data.roles` para las
 * dos pantallas que no tenían permiso propio. Ahora hay uno solo, y los
 * permisos que faltaban los añade V10__permisos_navegacion.sql.
 *
 * El permiso declarado aquí es el mismo que declara el ítem correspondiente en
 * NAV_CATALOG: esa correspondencia es la que hace que conceder un permiso
 * muestre la página y permita entrar en ella.
 */
export const ADMINISTRACION_ROUTES: Routes = [
  { path: '', redirectTo: 'users', pathMatch: 'full' },
  {
    path: 'users',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.users },
    loadComponent: () => import('./pages/users/users.component').then(m => m.UsersComponent)
  },
  {
    path: 'roles-permissions',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.rolesPermisos },
    loadComponent: () => import('./pages/roles-permissions/roles-permissions.component').then(m => m.RolesPermissionsComponent)
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.configuracion },
    loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent)
  },

  // ─── Moderador ───
  {
    path: 'mod-overview',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.panelModeracion },
    loadComponent: () => import('./pages/mod-overview/mod-overview.component').then(m => m.ModOverviewComponent)
  },
  {
    path: 'verificaciones',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.verificaciones },
    loadComponent: () => import('./pages/verificaciones/verificaciones.component').then(m => m.VerificacionesComponent)
  },
  {
    path: 'mod-portafolios',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.portafoliosModeracion },
    loadComponent: () => import('./pages/mod-portafolios/mod-portafolios.component').then(m => m.ModPortafoliosComponent)
  },
  {
    path: 'mod-comentarios',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.comentariosModeracion },
    loadComponent: () => import('./pages/mod-comentarios/mod-comentarios.component').then(m => m.ModComentariosComponent)
  },
  {
    path: 'mod-categorias',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.categorias },
    loadComponent: () => import('./pages/mod-categorias/mod-categorias.component').then(m => m.ModCategoriasComponent)
  },
  {
    path: 'paises',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.paises },
    loadComponent: () => import('./pages/paises/paises.component').then(m => m.PaisesComponent)
  },
  // Centro de notificaciones: los endpoints son `isAuthenticated()`, así que
  // aplica a cualquier rol del panel, no solo al moderador.
  {
    path: 'notificaciones',
    loadChildren: () => import('../comunicacion/comunicacion.routes').then(m => m.COMUNICACION_ROUTES)
  },
  {
    path: 'infracciones',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.infracciones },
    loadComponent: () => import('./pages/infracciones/infracciones.component').then(m => m.InfraccionesComponent)
  },
  // La pantalla vive en el módulo de pedidos (M4) pero solo la usa el admin;
  // montarla aquí la hace alcanzable desde el nav, que la concatena con /admin.
  {
    path: 'flujos',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.flujos },
    loadComponent: () => import('../pedido/pages/flujos-admin/flujos-admin.component').then(m => m.FlujosAdminComponent)
  }
];
