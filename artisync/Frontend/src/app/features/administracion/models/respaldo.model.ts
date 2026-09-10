/** Espejo de las entidades/DTOs de uteq.edu.ec.artisync.entity.respaldo / dto.*.respaldo. */

export type TipoRespaldo = 'FULL' | 'INCREMENTAL';
export type EstadoRespaldo = 'EN_PROGRESO' | 'COMPLETADO' | 'FALLIDO';
export type OrigenRespaldo = 'MANUAL' | 'PROGRAMADO';

export interface RespaldoResponse {
  idRespaldo: number;
  tipoRespaldo: TipoRespaldo;
  estadoRespaldo: EstadoRespaldo;
  origen: OrigenRespaldo;
  idProgramacion: number | null;
  idRespaldoFullBase: number | null;
  nombreArchivo: string | null;
  tamanoBytes: number | null;
  fechaInicio: string;
  fechaFin: string | null;
  duracionMs: number | null;
  mensajeError: string | null;
  correoSolicitante: string;
}

export interface CrearRespaldoRequest {
  tipoRespaldo: TipoRespaldo;
}

export interface FiltroRespaldo {
  tipoRespaldo?: TipoRespaldo;
  estadoRespaldo?: EstadoRespaldo;
  origen?: OrigenRespaldo;
  desde?: string;
  hasta?: string;
}

export interface ProgramacionRespaldoResponse {
  idProgramacion: number;
  nombre: string;
  tipoRespaldo: TipoRespaldo;
  expresionCron: string;
  retencionDias: number;
  activo: boolean;
  proximaEjecucion: string;
  ultimaEjecucion: string | null;
  creadoPor: string;
  fechaCreacion: string;
}

export interface CrearProgramacionRequest {
  nombre: string;
  tipoRespaldo: TipoRespaldo;
  expresionCron: string;
  retencionDias: number;
}

export type ActualizarProgramacionRequest = CrearProgramacionRequest;

/** Presets de cron para el formulario — el backend solo guarda el texto final. */
export const PRESETS_CRON: { label: string; valor: string }[] = [
  { label: 'Diario 03:00', valor: '0 0 3 * * *' },
  { label: 'Diario 00:00', valor: '0 0 0 * * *' },
  { label: 'Semanal (domingo 02:00)', valor: '0 0 2 * * SUN' },
  { label: 'Mensual (día 1, 02:00)', valor: '0 0 2 1 * *' }
];
