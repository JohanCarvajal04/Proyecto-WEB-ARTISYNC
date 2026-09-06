/** Espejo de RespuestaSaldoCreador.java. */
export interface SaldoCreador {
  saldoDisponible: number;
  montoMinimoRetiro: number;
  tieneCorreoPaypalConfigurado: boolean;
  tieneSolicitudPendiente: boolean;
}

/** Espejo de RespuestaSolicitudRetiro.java. */
export interface SolicitudRetiro {
  idSolicitud: number;
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

/** Espejo de RespuestaDatosPago.java. */
export interface DatosPagoCreador {
  correoPaypal: string | null;
  fechaActualizacion: string | null;
}
