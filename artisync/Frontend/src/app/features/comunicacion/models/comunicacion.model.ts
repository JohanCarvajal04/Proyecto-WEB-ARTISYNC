// ─── Modelos del Módulo Comunicación (M6) ───────────────────────

// ── Notificaciones ──
export interface RespuestaNotificacion {
  idNotificacion: number;
  tipoEvento: string;
  mensaje: string;
  estaLeida: boolean;
  fechaEmision: string;
}

/** GET /api/v1/notificaciones/no-leidas/count → `{ noLeidas: n }` */
export interface ConteoNoLeidas {
  noLeidas: number;
}

// ── Chat ──
export interface RespuestaMensajeChat {
  idMensaje: number;
  idSala: number;
  idRemitente: number;
  nombreRemitente: string;
  cuerpoMensaje: string;
  fechaHoraEnvio: string;
  leido: boolean;
}

export interface RespuestaSalaChat {
  idSala: number;
  idPedido: number;
  salaActiva: boolean;
  fechaApertura: string;
}

export interface PeticionEnviarMensaje {
  /** Solo requerido al enviar por WebSocket (STOMP); ignorado por el endpoint REST. */
  idPedido?: number;
  cuerpoMensaje: string;
}

/** Límite declarado por @Size en PeticionEnviarMensaje. */
export const MAX_CARACTERES_MENSAJE = 5000;

// ── Briefing ──
export interface PreguntaRespuestaItem {
  idPregunta: number;
  textoPregunta: string;
  numeroOrden: number;
  /** `null` mientras el cliente no haya respondido. */
  textoRespuesta: string | null;
  fechaRespuesta: string | null;
}

export interface RespuestaBriefing {
  idBriefingEnviado: number;
  idPedido: number;
  idPlantilla: number;
  nombrePlantilla: string;
  fechaEnvio: string;
  completado: boolean;
  preguntas: PreguntaRespuestaItem[];
}

export interface RespuestaItemBriefing {
  idPregunta: number;
  textoRespuesta: string;
}

export interface PeticionResponderBriefing {
  respuestas: RespuestaItemBriefing[];
}

// ── Briefing: plantillas del creador ──
export interface PreguntaPlantilla {
  textoPregunta: string;
  numeroOrden: number;
}

export interface PeticionCrearBriefingPlantilla {
  nombrePlantilla: string;
  preguntas: PreguntaPlantilla[];
}

export interface PeticionEnviarBriefing {
  idBriefingPlantilla: number;
}

/** Topes declarados por @Size en PeticionCrearBriefingPlantilla (RF-16). */
export const MAX_PREGUNTAS_PLANTILLA = 10;
export const MAX_NOMBRE_PLANTILLA = 150;
