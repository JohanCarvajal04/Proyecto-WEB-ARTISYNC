// ─── Modelos del Panel de Creador/Vendedor ──────────────────────
// Espejo de los DTO del backend (catálogo M2, perfil M3, social M6).

// ── Catálogo: etiquetas, categorías y subcategorías ──
export interface RespuestaEtiqueta {
  idEtiqueta: number;
  nombreEtiqueta: string;
  actualizadoEn?: string;
}

export interface RespuestaCategoria {
  idCategoria: number;
  nombreCategoria: string;
  estadoActiva: boolean;
  /** null = la creó un admin/moderador. */
  idUsuarioCreador?: number | null;
  nombreCreador?: string | null;
  revisado?: boolean;
  actualizadoEn?: string;
}

export interface RespuestaSubcategoria {
  idSubcategoria: number;
  idCategoria: number;
  nombreCategoria: string;
  nombreSubcategoria: string;
  idUsuarioCreador?: number | null;
  nombreCreador?: string | null;
  revisado?: boolean;
  actualizadoEn?: string;
}

export interface PeticionCrearCategoria {
  nombreCategoria: string;
}

export interface PeticionCrearSubcategoria {
  idCategoria: number;
  nombreSubcategoria: string;
}

// ── Catálogo: servicios del creador ──
export type EstadoPublicacion = 'ACTIVO' | 'PAUSADO' | 'BORRADOR';
export type TipoItem = 'PRODUCTO' | 'SERVICIO';

export interface RespuestaAtributo {
  idServicioAtributo: number;
  idAtributo: number;
  nombreAtributo: string;
  tipoDato: string;
  valorAsignado: string;
  actualizadoEn?: string;
}

export interface RespuestaServicio {
  idServicio: number;
  tituloServicio: string;
  descripcionDetallada: string;
  precioBase: number;
  tipoItem: TipoItem;
  estadoPublicacion: EstadoPublicacion;
  urlMiniatura: string | null;
  cargoRevisionAdicional: number | null;
  limiteRevisionesBase: number | null;
  subcategorias: RespuestaSubcategoria[];
  idPerfilCreador: number;
  nombreCreador: string;
  idFlujo: number | null;
  nombreFlujo: string | null;
  /** Plantilla de contrato asignada (catálogo curado por ADMIN); null = usa la predeterminada. */
  idPlantillaContrato: number | null;
  nombrePlantillaContrato: string | null;
  /** Cuestionario asignado, entre los propios del creador; null = crear un pedido no pide preguntas. */
  idBriefingPlantilla: number | null;
  nombreBriefingPlantilla: string | null;
  /** Preguntas del cuestionario asignado, para responderlas al crear el pedido. */
  preguntasBriefing: PreguntaBriefingServicio[] | null;
  atributos: RespuestaAtributo[];
  etiquetas: RespuestaEtiqueta[];
  actualizadoEn: string;
}

export interface PreguntaBriefingServicio {
  idPregunta: number;
  textoPregunta: string;
  numeroOrden: number;
}

export interface RespuestaServicioResumido {
  idServicio: number;
  tituloServicio: string;
  precioBase: number;
  tipoItem: TipoItem;
  estadoPublicacion: EstadoPublicacion;
  urlMiniatura: string | null;
  subcategorias: RespuestaSubcategoria[];
  idPerfilCreador: number;
  nombreCreador: string;
  etiquetas: RespuestaEtiqueta[];
}

export interface PeticionCrearServicio {
  tituloServicio: string;
  descripcionDetallada: string;
  precioBase: number;
  idsSubcategoria: number[];
  tipoItem: TipoItem;
  urlMiniatura?: string | null;
  cargoRevisionAdicional?: number | null;
  limiteRevisionesBase?: number | null;
  idFlujo?: number | null;
  /** Opcional: una plantilla del catálogo curado por ADMIN. Sin elegir, el contrato usa la predeterminada. */
  idPlantillaContrato?: number | null;
  /** Opcional: uno de los cuestionarios propios del creador. Sin elegir, crear un pedido no pide preguntas. */
  idBriefingPlantilla?: number | null;
  etiquetaIds?: number[];
}

export interface PeticionActualizarServicio extends PeticionCrearServicio {
  estadoPublicacion: EstadoPublicacion;
}

export interface PeticionCrearAtributo {
  nombreAtributo: string;
  valorAsignado: string;
  tipoDato: string;
}

export type PeticionActualizarAtributo = PeticionCrearAtributo;

// ── Perfil de creador ──
export interface RespuestaPerfil {
  idPerfil: number;
  idUsuario: number;
  nombresUsuario: string;
  apellidosUsuario: string;
  biografia: string | null;
  urlRedSocial: string | null;
  urlPortada?: string | null;
  urlFotoPerfil?: string | null;
  tituloProfesional?: string | null;
  /** Verificación de identidad real (CertificadoIa tipo IDENTIDAD, estado APROBADO). */
  identidadVerificada?: boolean;
}

export interface PeticionCrearPerfil {
  idUsuario: number;
  biografia?: string | null;
  urlRedSocial?: string | null;
  tituloProfesional?: string | null;
}

export interface PeticionActualizarPerfil {
  biografia?: string | null;
  urlRedSocial?: string | null;
  tituloProfesional?: string | null;
}

// ── Certificados de uso de IA ──
export interface RespuestaCertificadoIa {
  idCertificado: number;
  idPerfil: number;
  idEstadoVerificacion: number;
  nombreEstadoVerificacion: string;
  urlDocumentoS3: string;
  puntajeConfianzaIa: number | null;
  fechaAnalisis: string;
}

export interface PeticionCrearCertificadoIa {
  idPerfil: number;
  idEstadoVerificacion: number;
  urlDocumentoS3: string;
  puntajeConfianzaIa?: number | null;
}

// ── Reseñas recibidas ──
export interface RespuestaResena {
  idResena: number;
  calificacionEstrellas: number;
  textoResena: string | null;
  fechaResena: string;
  nombreCliente: string;
  tituloServicio: string;
}

// ── Sorteos ──
export interface RespuestaGanador {
  idParticipacion: number;
  idUsuario: number;
  nombreUsuario: string;
  fechaNotificacionPremio: string | null;
  idPremio: number | null;
  descripcionPremio: string | null;
}

/** Un premio individual del sorteo, con su ganador si ya se sorteó. */
export interface RespuestaPremio {
  idPremio: number;
  descripcionPremio: string;
  orden: number;
  ganador: RespuestaGanador | null;
}

export interface RespuestaParticipante {
  idParticipacion: number;
  idUsuario: number;
  nombreUsuario: string;
  fechaInscripcion: string;
  esGanador: boolean;
}

export interface RespuestaSorteo {
  idSorteo: number;
  tituloSorteo: string;
  cantidadGanadores: number;
  fechaInicio: string;
  fechaCierre: string;
  estadoSorteo: string;
  requiereSeguidor: boolean;
  idPerfilCreador: number;
  nombreCreador: string;
  totalParticipantes: number;
  yoParticipo: boolean;
  ganadores: RespuestaGanador[] | null;
  premios: RespuestaPremio[];
}

export interface PeticionCrearSorteo {
  tituloSorteo: string;
  /** Uno por ganador; cantidadGanadores debe ser igual a premios.length. */
  premios: string[];
  cantidadGanadores: number;
  fechaInicio: string;
  fechaCierre: string;
  requiereSeguidor: boolean;
}

export interface PeticionActualizarSorteo {
  tituloSorteo?: string;
  premios?: string[];
  cantidadGanadores?: number;
  fechaCierre?: string;
}
