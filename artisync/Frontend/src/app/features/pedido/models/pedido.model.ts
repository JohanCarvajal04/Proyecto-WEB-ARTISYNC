// ─── Modelos del Módulo Pedido (M4) ─────────────────────────────

// ── Flujos de Trabajo ──
export interface RespuestaFlujoTrabajo {
  idFlujo: number;
  nombreFlujo: string;
  descripcionFlujo: string;
  etapas: RespuestaEtapaConfig[];
}

export interface RespuestaEtapaConfig {
  idFlujoEtapa: number;
  idEtapa: number;
  nombreEtapa: string;
  numeroOrden: number;
  esEtapaFinal: boolean;
  requiereEntregable: boolean;
}

export interface PeticionCrearFlujoTrabajo {
  nombreFlujo: string;
  descripcionFlujo: string;
  etapas: PeticionEtapaConfig[];
}

export interface PeticionEtapaConfig {
  nombreEtapa: string;
  numeroOrden: number;
  esEtapaFinal: boolean;
  requiereEntregable: boolean;
}

// ── Pedidos ──
export interface RespuestaPedido {
  idPedido: number;
  idServicio: number;
  tituloServicio: string;
  idCliente: number;
  nombreCliente: string;
  idCreador: number;
  nombreCreador: string;
  etapaActual: string;
  precioPactado: number;
  fechaInicio: string;
  fechaEntregaEstimada: string;
  nombreFlujo: string;
  historial: RespuestaHistorialEstado[];
}

export interface RespuestaPedidoResumido {
  idPedido: number;
  tituloServicio: string;
  etapaActual: string;
  precioPactado: number;
  fechaInicio: string;
  fechaEntregaEstimada: string;
  nombreCreador: string;
  nombreCliente: string;
}

export interface RespuestaSeguimientoPedido {
  idPedido: number;
  tituloServicio: string;
  etapaActual: string;
  etapaActualOrden: number;
  totalEtapas: number;
  porcentajeProgreso: number;
  fechaUltimaActualizacion: string;
  etapasDelFlujo: RespuestaEtapaConfig[];
  historial: RespuestaHistorialEstado[];
  bloqueadoPorEntregable: boolean;
}

export interface RespuestaHistorialEstado {
  idHistorial: number;
  nombreEtapa: string;
  fechaTransicion: string;
  observacion: string;
}

export interface PeticionCrearPedido {
  idServicio: number;
  precioOfrecido: number | null;
  fechaEntregaEstimada: string | null;
}

export interface PeticionAvanzarEtapa {
  observacion: string;
}

/**
 * Ajuste de precio y/o fecha negociado por chat antes de firmar el contrato.
 * Al menos uno de los dos debe venir; el backend rechaza el resto de casos
 * (ver PedidoServicioImpl#actualizarTerminos).
 */
export interface PeticionActualizarTerminosPedido {
  precioPactado?: number | null;
  fechaEntregaEstimada?: string | null;
}

// ── Tickets de Revisión ──
export interface RespuestaTicketRevision {
  idTicket: number;
  idPedido: number;
  descripcionMotivo: string;
  descripcionCliente: string;
  estadoTicket: string;
  costoAdicionalGenerado: number | null;
  urlPagoAdicional: string | null;
}

export interface PeticionCrearTicketRevision {
  idMotivo: number;
  descripcionCliente: string;
}
