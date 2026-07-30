import { Routes } from '@angular/router';

export const PERFIL_ROUTES: Routes = [
  {
    path: 'change-password',
    loadComponent: () => import('./pages/change-password/change-password.component').then(m => m.ChangePasswordComponent)
  },
  {
    path: 'two-factor',
    loadComponent: () => import('./pages/two-factor/two-factor.component').then(m => m.TwoFactorSetupComponent)
  }
];
