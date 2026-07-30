import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../../features/seguridad/services/auth.service';

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    const role = auth.primaryRole();
    const redirectMap: Record<string, string> = {
      'ADMINISTRADOR': '/admin/users',
      'CREADOR': '/creator/dashboard',
      'CLIENTE': '/client/explore'
    };
    router.navigate([redirectMap[role ?? ''] ?? '/admin/users']);
    return false;
  }

  return true;
};
