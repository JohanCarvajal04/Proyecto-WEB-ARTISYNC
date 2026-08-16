import { Routes } from '@angular/router';

export const CREADOR_ROUTES: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  {
    path: 'overview',
    loadComponent: () => import('./pages/overview/creador-overview.component').then(m => m.CreadorOverviewComponent)
  },
  {
    path: 'servicios',
    loadComponent: () => import('./pages/servicios/mis-servicios.component').then(m => m.MisServiciosComponent)
  },
  {
    path: 'servicios/nuevo',
    loadComponent: () => import('./pages/servicio-form/servicio-form.component').then(m => m.ServicioFormComponent)
  },
  {
    path: 'servicios/:id/editar',
    loadComponent: () => import('./pages/servicio-form/servicio-form.component').then(m => m.ServicioFormComponent)
  },
  {
    path: 'comisiones',
    loadComponent: () => import('./pages/comisiones/comisiones.component').then(m => m.ComisionesComponent)
  },
  {
    path: 'comisiones/:id',
    loadComponent: () => import('./pages/comision-detalle/comision-detalle.component').then(m => m.ComisionDetalleComponent)
  },
  {
    path: 'resenas',
    loadComponent: () => import('./pages/resenas/resenas.component').then(m => m.ResenasComponent)
  },
  {
    path: 'sorteos',
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
    loadComponent: () => import('./pages/portafolio/portafolio-creador.component').then(m => m.PortafolioCreadorComponent)
  },
  {
    path: 'certificados',
    loadComponent: () => import('./pages/certificados/certificados.component').then(m => m.CertificadosComponent)
  },
  {
    path: 'perfil',
    loadComponent: () => import('./pages/perfil/perfil-creador.component').then(m => m.PerfilCreadorComponent)
  },
  { path: '**', redirectTo: 'overview' }
];
