import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, switchMap } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { AuthService } from '../../features/seguridad/services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Evitar interceptar login/refresh (para no crear bucles) ni 2fa/verify:
      // un 401 aquí es "código inválido o ticket expirado" (§2.1/OBS-AUTO-05),
      // no una sesión caída — el propio componente ya lo maneja y muestra el
      // mensaje correcto. Sin esta excepción, el interceptor confundía este
      // 401 con una expiración de sesión y forzaba un refresh + el toast
      // genérico "Tu sesión ha expirado", ocultando el motivo real.
      if (req.url.includes('/auth/login') || req.url.includes('/auth/refresh') || req.url.includes('/auth/2fa/verify')) {
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
          toast.error('No tienes permisos para realizar esta acción.');
          router.navigate(['/no-autorizado']);
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
