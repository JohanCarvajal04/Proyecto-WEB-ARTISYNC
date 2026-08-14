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
    // Usa homeRoute() en lugar de un mapa propio: el mapa anterior apuntaba a
    // '/creator/dashboard' y '/client/explore', rutas que no existen en
    // app.routes.ts. Al no existir caían en el comodín '**' -> '/auth/login',
    // que vuelve a entrar en este mismo guard: bucle de redirección infinito
    // para CREADOR y CLIENTE. Estaba latente sólo porque isLoggedIn() siempre
    // era falso aquí antes de arreglar la restauración de sesión.
    router.navigateByUrl(auth.homeRoute());
    return false;
  }

  return true;
};
