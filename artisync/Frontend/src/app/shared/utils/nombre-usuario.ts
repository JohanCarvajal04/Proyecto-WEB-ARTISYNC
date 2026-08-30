/**
 * Nombre a mostrar para un usuario: preferir nombres/apellidos reales y solo
 * caer al prefijo del correo (capitalizado) cuando el perfil todavía no los
 * tiene cargados. Antes esta lógica de fallback estaba duplicada en media
 * docena de pantallas usando *solo* el correo, ignorando nombres/apellidos
 * aunque ya estuvieran disponibles.
 */
export function nombreUsuario(
  datos: { nombres?: string | null; apellidos?: string | null } | null | undefined,
  correoFallback: string,
  porDefecto = 'Usuario'
): string {
  if (datos && (datos.nombres || datos.apellidos)) {
    return `${datos.nombres || ''} ${datos.apellidos || ''}`.trim();
  }
  const prefijo = correoFallback.split('@')[0];
  if (!prefijo || correoFallback === '—') return porDefecto;
  return prefijo.charAt(0).toUpperCase() + prefijo.slice(1);
}
