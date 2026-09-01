import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { PAGE_PERMISSIONS } from '../../core/config/nav.config';

/**
 * Igual criterio que ADMINISTRACION_ROUTES: cada ruta hija se guarda con
 * `authGuard` + `data.permissions` cuando NAV_CATALOG le asocia un permiso
 * propio (servicios, comisiones, sorteos, portafolio). El resto de páginas
 * del panel (overview, briefings, notificaciones, reseñas, seguidores,
 * perfil) no tienen permiso individual en PAGE_PERMISSIONS
 * porque están abiertas a cualquiera que ya haya entrado a /creador — el
 * único guard que les aplica es el del padre en app.routes.ts
 * (panelGatePermissions('creador')). Antes solo el padre se guardaba, así que un
 * creador con un único permiso del panel (p. ej. SORTEO_CREAR) podía navegar
 * por URL a /creador/servicios/nuevo o /creador/comisiones sin tener
 * SERVICIO_CREAR ni PEDIDO_GESTIONAR.
 */
export const CREADOR_ROUTES: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  {
    path: 'overview',
    loadComponent: () => import('./pages/overview/creador-overview.component').then(m => m.CreadorOverviewComponent)
  },
  {
    path: 'servicios',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.servicios },
    loadComponent: () => import('./pages/servicios/mis-servicios.component').then(m => m.MisServiciosComponent)
  },
  {
    path: 'servicios/nuevo',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.servicios },
    loadComponent: () => import('./pages/servicio-form/servicio-form.component').then(m => m.ServicioFormComponent)
  },
  {
    path: 'servicios/:id/editar',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.servicios },
    loadComponent: () => import('./pages/servicio-form/servicio-form.component').then(m => m.ServicioFormComponent)
  },
  {
    path: 'comisiones',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.comisiones },
    loadComponent: () => import('./pages/comisiones/comisiones.component').then(m => m.ComisionesComponent)
  },
  {
    path: 'comisiones/:id',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.comisiones },
    loadComponent: () => import('./pages/comision-detalle/comision-detalle.component').then(m => m.ComisionDetalleComponent)
  },
  {
    path: 'resenas',
    loadComponent: () => import('./pages/resenas/resenas.component').then(m => m.ResenasComponent)
  },
  {
    path: 'seguidores',
    loadComponent: () => import('./pages/seguidores/seguidores.component').then(m => m.SeguidoresComponent)
  },
  {
    path: 'sorteos',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.sorteos },
    loadComponent: () => import('./pages/sorteos/sorteos.component').then(m => m.SorteosComponent)
  },
  {
    path: 'briefings',
    loadComponent: () => import('./pages/briefings/briefings.component').then(m => m.BriefingsComponent)
  },
  // Mismo centro de notificaciones que el cliente, pero bajo /creador para que
  // la navegación no saque al usuario de su panel.
  {
    path: 'notificaciones',
    loadChildren: () => import('../comunicacion/comunicacion.routes').then(m => m.COMUNICACION_ROUTES)
  },
  {
    path: 'portafolio',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.portafolioPropio },
    loadComponent: () => import('./pages/portafolio/portafolio-creador.component').then(m => m.PortafolioCreadorComponent)
  },
  // El backend (FlujoTrabajoControlador) ya filtra todo por el creador
  // logueado (ver comentario en nav.config.ts junto a esta misma entrada).
  {
    path: 'flujos',
    canActivate: [authGuard],
    data: { permissions: PAGE_PERMISSIONS.flujosPropios },
    loadComponent: () => import('../pedido/pages/flujos-admin/flujos-admin.component').then(m => m.FlujosAdminComponent)
  },
  {
    path: 'perfil',
    loadComponent: () => import('./pages/perfil/perfil-creador.component').then(m => m.PerfilCreadorComponent)
  },
  { path: '**', redirectTo: 'overview' }
];
