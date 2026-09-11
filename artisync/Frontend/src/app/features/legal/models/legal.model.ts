// ─── Modelos del Módulo Legal (M5) ──────────────────────────────

export interface RespuestaContrato {
  idContrato: number;
  idPedido: number;
  tituloServicio: string;
  idCreador: number;
  nombreCreador: string;
  idCliente: number;
  nombreCliente: string;
  versionLegal: string;
  contenidoHtml: string;
  hashFirmaCreador: string | null;
  hashFirmaCliente: string | null;
  limiteRevisiones: number;
  fechaFormalizacion: string | null;
  urlDocumentoPdf: string | null;
  ambasFirmasCompletas: boolean;
}

export interface RespuestaEstadoFirma {
  idContrato: number;
  firmaCreadorCompleta: boolean;
  firmaClienteCompleta: boolean;
  ambasFirmasCompletas: boolean;
  mensajeEstado: string;
}

export interface RespuestaEntregable {
  idEntregable: number;
  idPedido: number;
  urlVersionMarcaAgua: string;
  urlVersionLimpia: string | null;
  estaLiberado: boolean;
}

export interface RespuestaPago {
  idPago: number;
  idContrato: number;
  idOrdenPaypal: string;
  montoRetenido: number;
  estadoFondos: string;
  approvalUrl: string | null;
}

// ── Catálogo de plantillas de contrato (REQ-F-017 ampliado) ──
// Curado por ADMIN: el creador solo elige entre estas, no escribe texto legal libre.

export interface RespuestaPlantillaContrato {
  idPlantilla: number;
  nombrePlantilla: string;
  versionLegal: string;
  cuerpoHtmlPlantilla: string;
  esPredeterminada: boolean;
  activa: boolean;
  /** null: catálogo general (ADMIN). No null: plantilla privada de ese creador (V45). */
  idCreador: number | null;
}

/** DTO liviano para el selector del creador al crear/editar un servicio. */
export interface RespuestaPlantillaContratoResumen {
  idPlantilla: number;
  nombrePlantilla: string;
  /** true si es una plantilla privada del creador que consulta (V45). */
  esPropia: boolean;
}

export interface PeticionCrearPlantillaContrato {
  nombrePlantilla: string;
  versionLegal: string;
  cuerpoHtmlPlantilla: string;
  esPredeterminada: boolean;
}

export interface PeticionActualizarPlantillaContrato extends PeticionCrearPlantillaContrato {
  activa: boolean;
}

// ── Plantillas de acuerdo propias del creador (V45) ──
// Autoservicio aparte del catálogo general: sin versión legal ni
// "predeterminada" (conceptos de gobierno del catálogo de ADMIN que no
// aplican a una plantilla que solo usa su propio dueño).

export interface PeticionCrearPlantillaAcuerdoPropia {
  nombrePlantilla: string;
  cuerpoHtmlPlantilla: string;
}

export interface PeticionActualizarPlantillaAcuerdoPropia extends PeticionCrearPlantillaAcuerdoPropia {
  activa: boolean;
}
