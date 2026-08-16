import { Routes } from '@angular/router';

/**
 * Social (M7). El formulario de reseña no tiene ruta propia: se embebe en el
 * detalle del pedido, que es donde el cliente puede calificar tras la entrega.
 */
export const SOCIAL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/sorteos/sorteos-cliente.component').then(m => m.SorteosClienteComponent)
  }
];
