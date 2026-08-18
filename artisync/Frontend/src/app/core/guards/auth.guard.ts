import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { ToastService } from '../services/toast.service';
import { findNavLabel } from '../config/nav.config';

export const authGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);
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
      return denegar(auth, router, toast, state.url, requiredRoles);
    }
  }

  // Único mecanismo de autorización por permiso desde que se eliminó
  // hasPermissionGuard, que era síncrono y no esperaba a waitForSessionRestore():
  // al recargar sobre una ruta protegida dependía de que los permisos siguieran
  // en localStorage, y con el almacenamiento limpio denegaba el acceso.
  const requiredPermissions: string[] = route.data['permissions'] ?? [];
  if (requiredPermissions.length > 0) {
    const hasPerm = auth.hasAnyPermission(...requiredPermissions);
    // El bypass de ADMIN replica el que ya aplica el backend en sus
    // @PreAuthorize ("hasAuthority('X') or hasRole('ADMIN')"): sin él, el
    // frontend sería más restrictivo que la API y bloquearía pantallas que el
    // servidor sí sirve.
    const isAdmin = auth.userRoles().some(r => r === 'ADMIN' || r === 'ADMINISTRADOR' || r === 'ROLE_ADMIN');
    if (!hasPerm && !isAdmin) {
      return denegar(auth, router, toast, state.url, requiredPermissions);
    }
  }

  return true;
};

/**
 * Redacta el aviso SIN nombrar códigos de autorización.
 *
 * La primera versión los enumeraba, con la idea de que el administrador supiera
 * qué conceder. Dos problemas: en la puerta del panel se exigía "cualquiera de"
 * casi treinta permisos, así que el aviso era un muro de texto ilegible; y
 * sobre todo, esos códigos son nomenclatura interna del sistema de
 * autorización y no tienen por qué mostrarse a quien precisamente no los tiene.
 *
 * Se nombra la sección con la etiqueta que aparece en el menú, que no revela
 * nada: es el mismo texto que ven quienes sí tienen acceso, y la ruta ya está
 * en la barra de direcciones.
 */
function mensajeDenegacion(urlIntentada: string): string {
  const seccion = findNavLabel(urlIntentada);
  const destino = seccion ? `a ${seccion}` : 'a esta sección';
  return `No tienes acceso ${destino}. Pide a un administrador que revise los permisos de tu rol.`;
}

/**
 * Deniega el acceso avisando, sin dejar al usuario tirado.
 *
 * Antes cualquier denegación llevaba a /no-autorizado, una pantalla sin salida
 * cuyo único botón cerraba la sesión: quedarte sin UN permiso te echaba de la
 * aplicación entera. Ahora se vuelve a la primera página que el usuario sí
 * puede ver y se explica el motivo.
 *
 * /no-autorizado queda reservado para el único caso en el que no hay a dónde
 * ir: un rol sin ninguna página accesible. También se usa si el destino de
 * respaldo es la propia ruta que se acaba de denegar, para no entrar en bucle.
 */
function denegar(
  auth: AuthService,
  router: Router,
  toast: ToastService,
  urlIntentada: string,
  requeridos: string[]
): UrlTree {
  toast.error(mensajeDenegacion(urlIntentada));

  // El detalle queda solo en la consola, para diagnóstico de desarrollo: nunca
  // llega a la interfaz.
  console.warn('[authGuard] Acceso denegado a %s. Requería alguno de: %s', urlIntentada, requeridos.join(', '));

  const destino = auth.homeRoute();
  const sinSalida = destino === '/no-autorizado' || destino === urlIntentada;

  return router.createUrlTree([sinSalida ? '/no-autorizado' : destino]);
}
