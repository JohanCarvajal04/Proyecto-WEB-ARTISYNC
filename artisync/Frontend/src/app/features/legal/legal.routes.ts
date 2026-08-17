import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const LEGAL_ROUTES: Routes = [
  {
    path: 'contrato/pedido/:idPedido',
    loadComponent: () => import('./pages/contrato-vista/contrato-vista.component').then(m => m.ContratoVistaComponent),
    canActivate: [authGuard]
  },
  {
    path: 'contrato/:id',
    loadComponent: () => import('./pages/contrato-vista/contrato-vista.component').then(m => m.ContratoVistaComponent),
    canActivate: [authGuard]
  },
  {
    path: 'pago/:idPedido',
    loadComponent: () => import('./pages/pago-checkout/pago-checkout.component').then(m => m.PagoCheckoutComponent),
    canActivate: [authGuard]
  },
  {
    path: 'entregable/:idPedido',
    loadComponent: () => import('./pages/entregable-vista/entregable-vista.component').then(m => m.EntregableVistaComponent),
    canActivate: [authGuard]
  },
  { path: '', redirectTo: '/pedido/mis-pedidos', pathMatch: 'full' }
];
