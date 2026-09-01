import { HttpResponse } from '@angular/common/http';

/**
 * Dispara la descarga de un blob de respuesta HTTP en el navegador.
 *
 * Antes esta lógica estaba triplicada (auditoría, entregables, contratos),
 * cada copia con el nombre de archivo hardcodeado en el cliente y revocando
 * el object URL de inmediato tras el click. El backend (RespuestaDocumento.java)
 * ya calcula el nombre final (slug_yyyyMMdd_HHmm.ext) y lo manda en
 * Content-Disposition: attachment — hay que leerlo de ahí, no reinventarlo.
 */
export function descargarRespuesta(respuesta: HttpResponse<Blob>, nombrePorDefecto: string): void {
  const blob = respuesta.body;
  if (!blob) return;

  const nombre = nombreDesdeContentDisposition(respuesta.headers.get('Content-Disposition')) ?? nombrePorDefecto;
  descargarBlob(blob, nombre);
}

/**
 * Variante para endpoints que aún devuelven `Observable<Blob>` puro (sin
 * `observe: 'response'`), como la descarga de contratos y entregables: el
 * nombre no se lee de un header, lo decide el llamador.
 */
export function descargarBlob(blob: Blob, nombre: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nombre;
  document.body.appendChild(a);
  a.click();
  a.remove();
  // Revocar de inmediato puede abortar la descarga en Firefox si el archivo
  // es grande y aún se está escribiendo a disco cuando se libera la URL.
  setTimeout(() => URL.revokeObjectURL(url), 0);
}

function nombreDesdeContentDisposition(header: string | null): string | null {
  if (!header) return null;

  // filename*=UTF-8''nombre%20con%20espacios.ext (RFC 5987)
  const extendido = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (extendido) {
    try {
      return decodeURIComponent(extendido[1].trim());
    } catch {
      // cae al filename simple si el valor no es un percent-encoding válido
    }
  }

  // filename="nombre.ext" o filename=nombre.ext
  const simple = /filename="?([^";]+)"?/i.exec(header);
  return simple ? simple[1].trim() : null;
}

/**
 * Decodifica el mensaje de error de una respuesta cuyo `responseType` era
 * 'blob'. Cuando el backend rechaza la exportación (p. ej. 422 por superar el
 * tope de filas), el cuerpo del error llega como Blob JSON en vez de objeto,
 * porque el cliente pidió `responseType: 'blob'` para el caso feliz.
 */
export async function mensajeErrorBlob(err: unknown, mensajePorDefecto: string): Promise<string> {
  const error = (err as { error?: unknown } | null)?.error;
  if (error instanceof Blob) {
    try {
      const detalle = JSON.parse(await error.text());
      return detalle?.detail ?? detalle?.message ?? mensajePorDefecto;
    } catch {
      return mensajePorDefecto;
    }
  }
  const detalle = error as { detail?: string; message?: string } | undefined;
  return detalle?.detail ?? detalle?.message ?? mensajePorDefecto;
}
