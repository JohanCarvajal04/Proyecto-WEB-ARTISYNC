// ─── Verificaciones (CERTIFICADO_REVISAR) ───

export interface VerificacionCola {
  idCertificado: number;
  idPerfil: number;
  nombreCreador: string;
  tipoDocumento: string;
  nombreEstado: string;
  veredictoIa: string | null;
  puntajeConfianzaIa: number | null;
  fechaAnalisis: string | null;
}

export interface VerificacionDetalle {
  idCertificado: number;
  idPerfil: number;
  tipoDocumento: string;
  nombreEstadoVerificacion: string;
  veredictoIa: string | null;
  puntajeConfianzaIa: number | null;
  razonIa: string | null;
  datosExtraidosIa: string | null;
  fechaDictamenIa: string | null;
  idModerador: number | null;
  fechaDecision: string | null;
  notaModerador: string | null;
  fechaAnalisis: string | null;
}

export interface DecisionVerificacion {
  idEstadoVerificacion: number;
  notaModerador: string;
}

// ─── Certificados IA ───

export interface CertificadoIa {
  idCertificado: number;
  idPerfil: number;
  idEstadoVerificacion: number;
  nombreEstadoVerificacion: string;
  urlDocumentoS3: string;
  puntajeConfianzaIa: number | null;
  fechaAnalisis: string | null;
}

// ─── Portafolios (PORTAFOLIO_MODERAR) — solo lectura ───

export interface Portafolio {
  idPortafolio: number;
  idPerfil: number;
  fechaCreacion: string;
  totalVisitasAcumuladas: number;
  esPublico: boolean;
  opcionesPersonalizacion: Record<string, string> | null;
}

// ─── Categorías (CATEGORIA_GESTIONAR) ───

export interface Categoria {
  idCategoria: number;
  nombreCategoria: string;
  estadoActiva: boolean;
  actualizadoEn: string;
}

export interface CrearCategoria {
  nombreCategoria: string;
}

export interface ActualizarCategoria {
  nombreCategoria?: string;
  estadoActiva?: boolean;
}

// ─── Servicios del catálogo (SERVICIO_MODERAR) — solo lectura ───

export interface ServicioResumido {
  idServicio: number;
  nombreServicio: string;
  descripcion: string;
  precioBase: number;
  estadoPublicacion: string;
  idCategoria: number;
  nombreCategoria: string;
}
