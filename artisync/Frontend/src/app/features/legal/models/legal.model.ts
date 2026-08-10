// ─── Modelos del Módulo Legal (M5) ──────────────────────────────

export interface RespuestaContrato {
  idContrato: number;
  idPedido: number;
  tituloServicio: string;
  nombreCreador: string;
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
