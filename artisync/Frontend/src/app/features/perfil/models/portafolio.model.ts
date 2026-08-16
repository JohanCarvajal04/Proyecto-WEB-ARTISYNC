export interface OpcionesPersonalizacion {
  primary: string;
  secondary: string;
  bg: string;
  text: string;
  surface: string;
}

export interface Portafolio {
  idPortafolio: number;
  idPerfil: number;
  fechaCreacion: string;
  totalVisitasAcumuladas: number;
  esPublico: boolean;
  opcionesPersonalizacion: OpcionesPersonalizacion;
}

export interface PeticionActualizarPortafolio {
  esPublico?: boolean;
  opcionesPersonalizacion?: OpcionesPersonalizacion;
}

export interface PeticionCrearPortafolio {
  idPerfil: number;
  esPublico?: boolean;
  opcionesPersonalizacion?: OpcionesPersonalizacion;
}

/**
 * Obra del portafolio. `urlArchivo` no es la referencia interna que guarda el
 * backend, sino algo que el navegador puede pedir directamente: una URL firmada
 * de Azure, o la ruta del endpoint que sirve los bytes.
 */
export interface PortafolioItem {
  idItemPortafolio: number;
  idPortafolio: number;
  tituloObra: string;
  descripcionObra: string | null;
  urlArchivo: string;
  fechaSubida: string;
}

export interface PeticionCrearPortafolioItem {
  tituloObra: string;
  descripcionObra?: string;
}

/** Lo que acepta el backend en PoliticaArchivo.PORTAFOLIO. */
export const TIPOS_OBRA_ACEPTADOS = [
  'image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif',
  'video/mp4', 'video/webm', 'video/quicktime'
];

export const MAX_BYTES_OBRA = 100 * 1024 * 1024;
