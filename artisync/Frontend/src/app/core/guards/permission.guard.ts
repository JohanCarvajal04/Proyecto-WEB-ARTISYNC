import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../features/seguridad/services/auth.service';

export const hasPermissionGuard = (requiredPermission: string): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.hasPermission(requiredPermission)) {
      return true;
    }

    console.warn(`Acceso denegado: falta el permiso requerido [${requiredPermission}]`);
    return router.parseUrl('/no-autorizado');
  };
};
