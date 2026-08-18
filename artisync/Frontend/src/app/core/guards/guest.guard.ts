import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../../features/seguridad/services/auth.service';

export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // Misma espera que authGuard: sin esto, un usuario ya autenticado que recarga
  // /auth/login veía el formulario en vez de ser redirigido a su dashboard.
  await auth.waitForSessionRestore();

  if (auth.isLoggedIn()) {
    // Mismo destino que usa el login: homeRoute() es la primera página que el
    // usuario puede ver según sus permisos. No sustituir por un mapa propio
    // aquí. El que hubo antes apuntaba a
    // '/creator/dashboard' y '/client/explore', rutas inexistentes en
    // app.routes.ts: caían en el comodín '**' -> '/auth/login', que vuelve a
    // entrar en este guard y produce un bucle infinito para CREADOR y CLIENTE.
    router.navigateByUrl(auth.homeRoute());
    return false;
  }

  return true;
};
