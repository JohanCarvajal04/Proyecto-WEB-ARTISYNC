/**
 * Espejo de `PoliticaArchivo.ENTREGABLE` en el backend. Validar aquí evita
 * subir 100 MB para que el servidor los rechace; la validación real sigue
 * siendo la del backend, esta solo ahorra el viaje.
 */
export const TIPOS_ENTREGABLE = [
  'image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif',
  'video/mp4', 'video/webm', 'video/quicktime',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
];

export const ACEPTA_ENTREGABLE = TIPOS_ENTREGABLE.join(',');

const MAX_BYTES = 100 * 1024 * 1024;

/** Devuelve el mensaje de error, o `null` si el archivo es válido. */
export function validarEntregable(archivo: File): string | null {
  if (!TIPOS_ENTREGABLE.includes(archivo.type)) {
    return `Formato no soportado: ${archivo.type || 'desconocido'}. Se acepta imagen, video o documento.`;
  }
  if (archivo.size > MAX_BYTES) {
    return 'El archivo supera el máximo de 100 MB.';
  }
  return null;
}

export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
