export interface OpcionesPersonalizacion {
  primary: string;
  secondary: string;
  bg: string;
  text: string;
  surface: string;
}

/** Paleta usada cuando un creador no ha personalizado su portafolio. */
export const COLORES_POR_DEFECTO: OpcionesPersonalizacion = {
  primary: '#0F9B8E',
  secondary: '#203A43',
  bg: '#EFF2F7',
  text: '#1E293B',
  surface: '#FFFFFF'
};

/** Campos editables de personalización, en el orden en que se muestran. */
export const CAMPOS_COLOR_PORTAFOLIO: { clave: keyof OpcionesPersonalizacion; etiqueta: string; ayuda: string }[] = [
  { clave: 'primary', etiqueta: 'Color primario', ayuda: 'Botones y acentos' },
  { clave: 'secondary', etiqueta: 'Color secundario', ayuda: 'Cabeceras y detalles' },
  { clave: 'bg', etiqueta: 'Fondo', ayuda: 'Lienzo de la página' },
  { clave: 'surface', etiqueta: 'Superficie', ayuda: 'Tarjetas y bloques' },
  { clave: 'text', etiqueta: 'Texto', ayuda: 'Color principal de lectura' }
];

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
