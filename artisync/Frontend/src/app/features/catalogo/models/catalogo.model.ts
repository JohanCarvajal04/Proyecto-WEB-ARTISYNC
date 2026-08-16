// ─── Modelos del Catálogo Público (M3) ──────────────────────────
// Los DTO de servicio, categoría y etiqueta ya estaban modelados para el panel
// del creador; se reutilizan aquí en vez de duplicarlos, porque el backend
// devuelve exactamente las mismas clases en los endpoints públicos.
export type {
  RespuestaServicio,
  RespuestaServicioResumido,
  RespuestaCategoria,
  RespuestaSubcategoria,
  RespuestaEtiqueta,
  RespuestaPerfil,
  RespuestaResena,
  RespuestaSorteo,
  EstadoPublicacion,
  TipoItem
} from '../../creador/models/creador.model';

/** Filtros aceptados por GET /api/v1/catalogo. Todos son opcionales. */
export interface FiltrosCatalogo {
  categoria?: number | null;
  subcategoria?: number | null;
  precioMin?: number | null;
  precioMax?: number | null;
  etiquetas?: number[];
  q?: string | null;
  sort?: string;
  page?: number;
  size?: number;
}

export const ORDEN_CATALOGO: { valor: string; etiqueta: string }[] = [
  { valor: 'idServicio,desc', etiqueta: 'Más recientes' },
  { valor: 'precioBase,asc', etiqueta: 'Precio: menor a mayor' },
  { valor: 'precioBase,desc', etiqueta: 'Precio: mayor a menor' },
  { valor: 'tituloServicio,asc', etiqueta: 'Título A-Z' }
];
