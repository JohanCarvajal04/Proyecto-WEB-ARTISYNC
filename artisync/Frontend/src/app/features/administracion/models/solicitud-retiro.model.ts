/** Filtros de GET /api/v1/admin/retiros (espejo de FiltroSolicitudRetiro.java). */
export interface FiltroSolicitudRetiro {
  estado?: string;
  idUsuarioCreador?: number;
  desde?: string;
  hasta?: string;
}

/** Fila/detalle de la cola (espejo de RespuestaSolicitudRetiro.java). */
export interface SolicitudRetiro {
  idSolicitud: number;
  idUsuarioCreador: number;
  nombreCreador: string;
  montoSolicitado: number;
  correoPaypalDestino: string;
  estado: 'Pendiente' | 'Aprobado' | 'Pagado' | 'Rechazado' | 'Fallido';
  idPayoutPaypal: string | null;
  notaAdmin: string | null;
  mensajeError: string | null;
  fechaSolicitud: string;
  fechaDecision: string | null;
  fechaPago: string | null;
}
