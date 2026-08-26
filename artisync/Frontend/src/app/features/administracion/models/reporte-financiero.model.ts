/** Filtros de GET /api/v1/admin/reportes/finanzas (espejo de FiltroReporteFinanciero.java). */
export interface FiltroReporteFinanciero {
  idPerfil?: number;
  desde?: string;
  hasta?: string;
  tasaComision?: number;
}

/** Fila de detalle (espejo de DetalleComision.java). */
export interface DetalleComision {
  idTransaccion: number;
  idPedido: number | null;
  servicio: string | null;
  tipo: string | null;
  monto: number;
  fechaEjecucion: string | null;
}

/** Cabecera + detalle del reporte (espejo de RespuestaReporteComisiones.java). */
export interface RespuestaReporteComisiones {
  idPerfil: number;
  fechaDesde: string | null;
  fechaHasta: string | null;
  tasaComision: number;
  totalPedidos: number;
  totalOperaciones: number;
  montoBruto: number;
  comision: number;
  montoNeto: number;
  detalle: DetalleComision[];
}
