/**
 * Espejo de `FilePolicy.BOCETO` en el backend. Validar aquí evita subir 20 MB
 * para que el servidor los rechace; la validación real sigue siendo la del
 * backend, esta solo ahorra el viaje.
 */
export const TIPOS_BOCETO = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];

export const ACEPTA_BOCETO = TIPOS_BOCETO.join(',');

const MAX_BYTES = 20 * 1024 * 1024;

/** Devuelve el mensaje de error, o `null` si el archivo es válido. */
export function validarBoceto(archivo: File): string | null {
  if (!TIPOS_BOCETO.includes(archivo.type)) {
    return `Formato no soportado: ${archivo.type || 'desconocido'}. Se acepta imagen.`;
  }
  if (archivo.size > MAX_BYTES) {
    return 'El archivo supera el máximo de 20 MB.';
  }
  return null;
}
