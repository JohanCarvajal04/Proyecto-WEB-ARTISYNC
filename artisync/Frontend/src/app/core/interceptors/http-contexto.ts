import { HttpContext, HttpContextToken } from '@angular/common/http';

/**
 * Marca una petición cuyo error maneja el propio componente, para que el
 * interceptor global no la toque.
 *
 * Hace falta porque hay respuestas de error que son estados normales, no
 * fallos: el chat de un pedido devuelve 404 mientras no se haya firmado el
 * contrato, el briefing mientras el creador no lo envíe, y el entregable
 * mientras no se suba. Sin esto, abrir el detalle de un pedido recién creado
 * disparaba varios toasts de "error inesperado" a la vez, y un 403 de una
 * petición de fondo llegaba a navegar a /no-autorizado, sacando al usuario de
 * la pantalla que estaba abriendo.
 */
export const OMITIR_ERROR_GLOBAL = new HttpContextToken<boolean>(() => false);

/** Azúcar para pasarlo en las opciones de HttpClient. */
export function sinErrorGlobal(): { context: HttpContext } {
  return { context: new HttpContext().set(OMITIR_ERROR_GLOBAL, true) };
}
