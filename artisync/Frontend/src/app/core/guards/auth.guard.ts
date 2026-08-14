import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../../features/seguridad/services/auth.service';

export const authGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requiredRoles: string[] = route.data['roles'] ?? [];

  // Espera a que termine el intento de restaurar sesión (vía cookie de refresh) antes
  // de leer isLoggedIn(): si no se espera, un F5 en una ruta protegida rebota siempre
  // a /auth/login porque el guard es síncrono y evalúa antes de que responda /auth/refresh.
  await auth.waitForSessionRestore();

  if (!auth.isLoggedIn()) {
    router.navigate(['/auth/login'], {
      queryParams: { returnUrl: state.url }
    });
    return false;
  }

  if (requiredRoles.length > 0) {
    const userRoles = auth.userRoles();
    const hasRole = requiredRoles.some(r => userRoles.includes(r) || userRoles.includes(`ROLE_${r}`));
    if (!hasRole) {
      router.navigate(['/no-autorizado']);
      return false;
    }
  }

  const requiredPermissions: string[] = route.data['permissions'] ?? [];
  if (requiredPermissions.length > 0) {
    const hasPerm = auth.hasAnyPermission(...requiredPermissions);
    const isAdmin = auth.userRoles().some(r => r === 'ADMIN' || r === 'ADMINISTRADOR' || r === 'ROLE_ADMIN');
    if (!hasPerm && !isAdmin) {
      router.navigate(['/no-autorizado']);
      return false;
    }
  }

  return true;
};
