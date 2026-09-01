import { Router } from '@angular/router';
import { AuthService } from '../../features/seguridad/services/auth.service';

/** Motivo por el que se exige sesión, usado por /acceso-requerido para adaptar el texto. */
export type MotivoAccesoRequerido = 'contratar' | 'seguir' | 'sorteo';

/**
 * Guarda de punto de uso para las acciones del catálogo público (contratar,
 * seguir a un creador, participar en un sorteo).
 *
 * A diferencia de `authGuard` —que protege una RUTA completa y manda directo a
 * `/auth/login`—, esta función protege una ACCIÓN dentro de una página que ya es
 * pública: el usuario puede seguir viendo el catálogo, y solo al pulsar "Solicitar
 * servicio" o "Seguir" se le explica por qué hace falta una cuenta.
 *
 * Devuelve `true` si ya hay sesión (el llamador continúa con su acción); si no,
 * navega a /acceso-requerido con el destino y el motivo, y devuelve `false`.
 */
export function exigirSesion(
  auth: AuthService,
  router: Router,
  returnUrl: string,
  motivo: MotivoAccesoRequerido
): boolean {
  if (auth.isLoggedIn()) return true;

  router.navigate(['/acceso-requerido'], { queryParams: { returnUrl, motivo } });
  return false;
}
