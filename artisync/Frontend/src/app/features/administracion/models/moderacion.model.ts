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

// ─── Subcategorías y etiquetas (alta/baja solo ADMIN) ───

export interface Subcategoria {
  idSubcategoria: number;
  idCategoria: number;
  nombreCategoria: string;
  nombreSubcategoria: string;
  actualizadoEn?: string;
}

export interface CrearSubcategoria {
  idCategoria: number;
  nombreSubcategoria: string;
}

export interface Etiqueta {
  idEtiqueta: number;
  nombreEtiqueta: string;
  actualizadoEn?: string;
}

// ─── Infracciones y suspensiones (solo ADMIN) ───

/** RespuestaInfraccion del backend: infracciones detectadas en el chat (RF-15). */
export interface Infraccion {
  idInfraccion: number;
  idUsuario: number;
  nombreUsuario: string;
  correoUsuario: string;
  idPedido: number | null;
  mensajeOriginal: string;
  patronDetectado: string;
  fechaInfraccion: string;
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
  /** Flujo que heredan los pedidos de esta categoría (RF-19); null si no se asignó. */
  idFlujo: number | null;
  nombreFlujo: string | null;
  actualizadoEn: string;
}

export interface CrearCategoria {
  nombreCategoria: string;
  estadoActiva?: boolean;
  idFlujo?: number | null;
}

/**
 * `nombreCategoria` no es opcional en la práctica: el backend lo valida con
 * @NotBlank, así que toda actualización —incluido el simple toggle de estado—
 * debe reenviar el nombre actual.
 */
export interface ActualizarCategoria {
  nombreCategoria: string;
  estadoActiva?: boolean;
  idFlujo?: number | null;
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
