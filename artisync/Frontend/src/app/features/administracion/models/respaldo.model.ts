export interface Respaldo {
  idRespaldo: number;
  nombreArchivo: string;
  tipo: 'AUTOMATICO' | 'MANUAL';
  categoria: 'FULL' | 'DIARIO';
  tamanoBytes: number;
  tamanoFormateado: string;
  estado: 'EN_PROGRESO' | 'COMPLETADO' | 'FALLIDO';
  mensajeError: string | null;
  creadoPor: string | null;
  fechaCreacion: string;
  fechaExpiracion: string | null;
  hashSha256: string | null;
}

export interface ResumenRespaldos {
  totalRespaldos: number;
  totalAutomaticos: number;
  totalManuales: number;
  totalFull: number;
  totalDiarios: number;
  ultimoRespaldoFecha: string | null;
  proximoFullEstimado: string | null;
  proximoDiarioEstimado: string | null;
  cronFull: string;
  retencionDiasFull: number;
  cronDiario: string;
  retencionDiasDiario: number;
  espacioTotalUsado: string;
  ultimoFullAutomaticoFecha: string | null;
  ultimoDiarioAutomaticoFecha: string | null;
  fullAtrasado: boolean;
  diarioAtrasado: boolean;
}

export interface RespaldoPolitica {
  idPolitica: number;
  cronFull: string;
  retencionDiasFull: number;
  cronDiario: string;
  retencionDiasDiario: number;
}

export interface PeticionActualizarPolitica {
  cronFull: string;
  retencionDiasFull: number;
  cronDiario: string;
  retencionDiasDiario: number;
}
