// ─── Modelos del Módulo Pedido (M4) ─────────────────────────────

// ── Flujos de Trabajo ──
export interface RespuestaFlujoTrabajo {
  idFlujo: number;
  nombreFlujo: string;
  descripcionFlujo: string;
  etapas: RespuestaEtapaConfig[];
  /** Dueño del flujo. Relevante con FLUJO_MODERAR: la lista incluye flujos de varios creadores. */
  idUsuarioCreador: number;
  nombreCreador: string;
}

export interface RespuestaEtapaConfig {
  idFlujoEtapa: number;
  idEtapa: number;
  nombreEtapa: string;
  numeroOrden: number;
  esEtapaFinal: boolean;
  requiereEntregable: boolean;
  requiereBoceto: boolean;
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
  requiereBoceto: boolean;
}

/** Swap atómico de numeroOrden entre dos etapas — lo usa "mover etapa arriba/abajo". */
export interface PeticionSwapEtapas {
  idFlujoEtapaA: number;
  idFlujoEtapaB: number;
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
  bloqueadoPorBoceto: boolean;
}

// ── Boceto ──
export interface RespuestaBoceto {
  idBoceto: number;
  idPedido: number;
  urlImagen: string;
  fechaSubida: string;
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
  /**
   * Obligatorio solo si el servicio tiene un cuestionario asignado
   * (RespuestaServicio.preguntasBriefing no vacío): una respuesta por cada
   * pregunta. REQ-F-016 ampliado — antes se respondía después, con un envío
   * manual del creador; ahora va aquí mismo, al crear el pedido.
   */
  respuestasBriefing?: RespuestaItemBriefingPedido[];
}

export interface RespuestaItemBriefingPedido {
  idPregunta: number;
  textoRespuesta: string;
}

export interface PeticionAvanzarEtapa {
  observacion: string;
}

/**
 * Propuesta de precio y/o fecha final, negociada por chat antes de firmar el
 * contrato. Al menos uno de los dos debe venir; el backend rechaza el resto
 * de casos (ver PedidoServicioImpl#proponerTerminos). El cambio no se aplica
 * al pedido hasta que la contraparte del proponente la acepta.
 */
export interface PeticionCrearPropuestaTerminos {
  precioPropuesto?: number | null;
  fechaEntregaPropuesta?: string | null;
}

export type EstadoPropuestaTerminos = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'CANCELADA';

export interface RespuestaPropuestaTerminos {
  idPropuesta: number;
  idPedido: number;
  idUsuarioPropuso: number;
  nombrePropuso: string;
  precioPropuesto: number | null;
  fechaEntregaPropuesta: string | null;
  estado: EstadoPropuestaTerminos;
  fechaCreacion: string;
  fechaResolucion: string | null;
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
