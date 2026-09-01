/** Filtros de GET /api/v1/admin/reportes/contratos (espejo de FiltroReporteContrato.java). */
export interface FiltroReporteContrato {
  desde?: string;
  hasta?: string;
  idPerfilCreador?: number;
  soloFirmados?: boolean;
}

/** Fila del reporte (espejo de FilaReporteContrato.java). */
export interface FilaReporteContrato {
  idContrato: number;
  idPedido: number;
  servicio: string;
  cliente: string;
  creador: string;
  precioPactado: number;
  limiteRevisiones: number;
  fechaFormalizacion: string | null;
  firmadoCliente: boolean;
  firmadoCreador: boolean;
}
