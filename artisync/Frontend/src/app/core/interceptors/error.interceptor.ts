import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, switchMap } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { OMITIR_ERROR_GLOBAL } from './http-contexto';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Peticiones que declaran manejar su propio error: para ellas un 404 es
      // un estado normal (todavía no hay chat, briefing o entregable), no algo
      // que deba avisarse ni, mucho menos, provocar una navegación.
      //
      // El 401 se deja fuera de esta excepción a propósito: renovar la sesión
      // es responsabilidad del interceptor pase lo que pase, y saltárselo aquí
      // dejaría la sesión sin refrescar según qué petición fallara primero.
      const propioError = req.context.get(OMITIR_ERROR_GLOBAL);
      if (propioError && error.status !== 401) {
        return throwError(() => error);
      }

      // Evitar interceptar login/refresh (para no crear bucles) ni 2fa/verify:
      // un 401 aquí es "código inválido o ticket expirado" (§2.1/OBS-AUTO-05),
      // no una sesión caída — el propio componente ya lo maneja y muestra el
      // mensaje correcto. Sin esta excepción, el interceptor confundía este
      // 401 con una expiración de sesión y forzaba un refresh + el toast
      // genérico "Tu sesión ha expirado", ocultando el motivo real.
      if (req.url.includes('/auth/login') || req.url.includes('/auth/refresh') || req.url.includes('/auth/2fa/verify')) {
        return throwError(() => error);
      }

      // Un 401 sin sesión activa no es una expiración: es un visitante anónimo
      // topándose con un endpoint protegido (p. ej. el catálogo público
      // consultando "creadores seguidos"). Sin esta guarda, cada carga del
      // catálogo sin sesión disparaba un POST /auth/refresh inútil y el toast
      // "Tu sesión ha expirado", que es falso — nunca hubo sesión que expirara.
      if (error.status === 401 && !auth.isLoggedIn()) {
        return throwError(() => error);
      }

      switch (error.status) {
        case 401:
          // Intentar refrescar token o cerrar sesión
          return auth.refreshToken().pipe(
            switchMap(() => {
              const token = auth.accessToken();
              const authReq = req.clone({
                setHeaders: { Authorization: `Bearer ${token}` }
              });
              return next(authReq);
            }),
            catchError(refreshErr => {
              toast.error('Tu sesión ha expirado. Inicia sesión nuevamente.');
              return throwError(() => refreshErr);
            })
          );

        case 403:
          // Solo se avisa; NO se navega. Antes cualquier 403 mandaba la
          // aplicación entera a /no-autorizado, así que un widget secundario o
          // un sondeo de fondo —el contador de notificaciones se consulta cada
          // 60 s en ambos layouts— echaba al usuario de la pantalla en la que
          // estaba trabajando y le hacía perder lo que tuviera a medias.
          //
          // Que falte un permiso para UNA acción no invalida el resto de la
          // página. Bloquear la navegación es competencia de authGuard, que sí
          // sabe si el usuario pidió ir a un sitio al que no puede entrar.
          toast.error(error.error?.detail ?? 'No tienes permisos para realizar esta acción.');
          break;

        case 422:
        case 400:
          const fieldErrors = error.error?.fieldErrors;
          if (fieldErrors && typeof fieldErrors === 'object') {
            const messages = Object.values(fieldErrors).join(', ');
            toast.error(`Error de validación: ${messages}`);
          } else {
            toast.error(error.error?.detail ?? 'Datos de entrada inválidos.');
          }
          break;

        case 500:
          toast.error('Error interno del servidor. Intenta nuevamente más tarde.');
          break;

        default:
          if (error.error?.detail) {
            toast.error(error.error.detail);
          } else if (error.status !== 0) {
            toast.error(`Ocurrió un error inesperado (Código: ${error.status})`);
          }
      }

      return throwError(() => error);
    })
  );
};
