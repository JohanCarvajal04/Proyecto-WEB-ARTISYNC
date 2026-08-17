// Helpers de presentación compartidos por las vistas del panel de creador.

export function formatPrice(valor: number | null | undefined): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(valor || 0);
}

export function formatDate(fecha: string | null | undefined): string {
  if (!fecha) return '—';
  return new Date(fecha).toLocaleDateString('es-EC', {
    day: '2-digit', month: 'short', year: 'numeric'
  });
}

export function formatDateTime(fecha: string | null | undefined): string {
  if (!fecha) return '—';
  return new Date(fecha).toLocaleString('es-EC', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
  });
}

/** Una etapa se considera cerrada cuando es final, entregada o cancelada. */
export function esEtapaActiva(etapa: string | null | undefined): boolean {
  const lower = (etapa || '').toLowerCase();
  return !lower.includes('completado') && !lower.includes('entregado')
    && !lower.includes('cancelado') && !lower.includes('final');
}

/** Modificador de `.cr-badge` acorde al estado de una etapa de pedido. */
export function badgeEtapa(etapa: string | null | undefined): string {
  const lower = (etapa || '').toLowerCase();
  if (!lower) return 'cr-badge';
  if (lower.includes('completado') || lower.includes('entregado') || lower.includes('final')) return 'cr-badge cr-badge--ok';
  if (lower.includes('revision') || lower.includes('pendiente')) return 'cr-badge cr-badge--warn';
  if (lower.includes('cancelado') || lower.includes('rechazado')) return 'cr-badge cr-badge--danger';
  return 'cr-badge cr-badge--info';
}

/** Modificador de `.cr-badge` acorde al estado de publicación de un servicio. */
export function badgePublicacion(estado: string | null | undefined): string {
  switch ((estado || '').toUpperCase()) {
    case 'ACTIVO': return 'cr-badge cr-badge--ok';
    case 'PAUSADO': return 'cr-badge cr-badge--warn';
    case 'BORRADOR': return 'cr-badge';
    default: return 'cr-badge cr-badge--info';
  }
}

/** Mensaje de error legible a partir de una respuesta HTTP fallida. */
export function mensajeError(err: unknown, porDefecto: string): string {
  const e = err as { error?: { message?: string; mensaje?: string } };
  return e?.error?.message || e?.error?.mensaje || porDefecto;
}
