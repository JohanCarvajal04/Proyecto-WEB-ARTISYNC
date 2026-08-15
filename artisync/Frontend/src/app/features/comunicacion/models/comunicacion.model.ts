export interface Mensaje {
  idMensaje: number;
  idSala: number;
  idRemitente: number;
  nombreRemitente: string;
  cuerpoMensaje: string;
  fechaHoraEnvio: string;
  leido: boolean;
}

export interface SalaChat {
  idSala: number;
  idPedido: number;
  salaActiva: boolean;
  fechaApertura: string;
}

export interface PeticionEnviarMensaje {
  cuerpoMensaje: string;
}

// Modelos para Briefing
export interface BriefingPregunta {
  idPregunta: number;
  textoPregunta: string;
  numeroOrden: number;
}

export interface BriefingPlantilla {
  idBriefingPlantilla: number;
  nombrePlantilla: string;
  preguntas: BriefingPregunta[];
}

export interface BriefingEnviado {
  idBriefingEnviado: number;
  idPedido: number;
  plantilla: BriefingPlantilla;
  completado: boolean;
}

export interface RespuestaBriefing {
  idPregunta: number;
  textoRespuesta: string;
}

export interface PeticionResponderBriefing {
  respuestas: RespuestaBriefing[];
}

// Modelos para Notificaciones
export interface Notificacion {
  idNotificacion: number;
  idUsuario: number;
  tipoNotificacion: string;
  mensaje: string;
  fechaCreacion: string;
  leida: boolean;
}
