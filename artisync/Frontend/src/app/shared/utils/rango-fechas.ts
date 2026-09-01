/**
 * Los filtros "Desde"/"Hasta" de auditoría, reportes de contratos/financieros
 * y pagos en garantía no validaban que el rango no viniera invertido
 * (revisión técnica, 2026-09-01): un admin que se equivocaba de campo no
 * recibía ningún aviso, solo una lista vacía silenciosa del backend.
 */
export function rangoFechasInvertido(filtro: { desde?: string | null; hasta?: string | null }): boolean {
  if (!filtro.desde || !filtro.hasta) return false;
  return new Date(filtro.desde) > new Date(filtro.hasta);
}
