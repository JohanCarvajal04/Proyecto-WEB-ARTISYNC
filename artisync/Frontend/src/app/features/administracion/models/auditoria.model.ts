export type ResultadoAuditoria = 'EXITO' | 'FALLIDO' | 'DENEGADO';

/**
 * Fila de la tabla de auditoría. Sin detalleCambio: el backend no lo envía en
 * el listado (ver RespuestaEventoAuditoriaResumen), solo en el detalle.
 */
export interface EventoAuditoriaResumen {
  idEventoAuditoria: number;
  fechaEvento: string;
  idUsuarioActor: number | null;
  correoActor: string;
  moduloAuditoria: string;
  accionAuditoria: string;
  resultadoEvento: ResultadoAuditoria;
  entidadAfectada: string | null;
  idEntidadAfectada: number | null;
  direccionIp: string | null;
}

/** Vista completa de un evento, para el modal de detalle. */
export interface EventoAuditoria extends EventoAuditoriaResumen {
  detalleCambio: Record<string, unknown> | null;
  mensajeError: string | null;
  agenteUsuario: string | null;
  metodoHttp: string | null;
  rutaSolicitud: string | null;
  duracionMs: number | null;
}

/** Los 8 módulos de negocio del backend (ModuloAuditoria). */
export const MODULOS_AUDITORIA: readonly string[] = [
  'SEGURIDAD', 'SISTEMA', 'PORTAFOLIO', 'CATALOGO',
  'PEDIDOS', 'FINANZAS', 'COMUNICACION', 'SOCIAL'
];

export const RESULTADOS_AUDITORIA: readonly ResultadoAuditoria[] = ['EXITO', 'FALLIDO', 'DENEGADO'];

export interface FiltroAuditoria {
  correoActor?: string;
  accion?: string;
  modulo?: string;
  resultado?: ResultadoAuditoria | '';
  desde?: string;
  hasta?: string;
}
