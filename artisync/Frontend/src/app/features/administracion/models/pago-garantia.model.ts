/** Filtros de GET /api/v1/admin/pagos-garantia (espejo de FiltroPagoGarantia.java). */
export interface FiltroPagoGarantia {
  estadoFondos?: string;
  idPerfilCreador?: number;
  idUsuarioCliente?: number;
  desde?: string;
  hasta?: string;
}

/** Fila del listado (espejo de RespuestaPagoGarantia.java). */
export interface PagoGarantia {
  idPago: number;
  idContrato: number;
  idPedido: number;
  tituloServicio: string;
  idUsuarioCliente: number;
  nombreCliente: string;
  idPerfilCreador: number;
  nombreCreador: string;
  idOrdenPaypal: string | null;
  montoRetenido: number;
  estadoFondos: string;
  fechaFormalizacion: string | null;
}

export interface TransaccionPago {
  idTransaccion: number;
  tipoTransaccion: string;
  monto: number;
  fechaEjecucion: string | null;
}

/** Detalle (espejo de RespuestaPagoGarantiaDetalle.java). */
export interface PagoGarantiaDetalle extends PagoGarantia {
  correoCliente: string;
  transacciones: TransaccionPago[];
}

/** Tarjeta de resumen agregado (espejo de RespuestaResumenEscrow.java). */
export interface ResumenEscrow {
  estadoFondos: string;
  cantidad: number;
  montoTotal: number;
}
