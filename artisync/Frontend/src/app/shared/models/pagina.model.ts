/**
 * Forma normalizada de una página, independiente de cómo la serialice el
 * backend.
 *
 * Hacen falta las tres variantes porque el proyecto mezcla dos estilos:
 *  - `util.PagedResponse` (usuarios, países) publica `number` + `pageSize`.
 *  - Los controladores que devuelven `Page<T>` directo (catálogo, chat,
 *    notificaciones) dependen del `PageSerializationMode` de Spring Data, que
 *    según la versión emite los campos en la raíz o anidados bajo `page`.
 *
 * Normalizar aquí evita que un cambio de default en el backend rompa la vista
 * en silencio, mostrando una lista vacía sin ningún error.
 */
export interface Pagina<T> {
  contenido: T[];
  numero: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
}

interface PaginaCruda {
  content?: unknown;
  number?: number;
  size?: number;
  pageNumber?: number;
  pageSize?: number;
  totalElements?: number;
  totalPages?: number;
  last?: boolean;
  page?: {
    number?: number;
    size?: number;
    totalElements?: number;
    totalPages?: number;
  };
}

export function normalizarPagina<T>(crudo: unknown): Pagina<T> {
  const p = (crudo ?? {}) as PaginaCruda;
  const anidada = p.page ?? {};

  const contenido = Array.isArray(p.content) ? (p.content as T[]) : [];
  const numero = primerNumero(p.number, p.pageNumber, anidada.number) ?? 0;
  const tamano = primerNumero(p.size, p.pageSize, anidada.size) ?? contenido.length;
  const totalElementos = primerNumero(p.totalElements, anidada.totalElements) ?? contenido.length;
  const totalPaginas = primerNumero(p.totalPages, anidada.totalPages)
    ?? (tamano > 0 ? Math.ceil(totalElementos / tamano) : 1);

  return {
    contenido,
    numero,
    tamano,
    totalElementos,
    totalPaginas,
    // `last` solo viene en algunas variantes; derivarlo es más fiable que asumir false.
    ultima: typeof p.last === 'boolean' ? p.last : numero >= totalPaginas - 1
  };
}

function primerNumero(...valores: (number | undefined)[]): number | undefined {
  return valores.find(v => typeof v === 'number' && !Number.isNaN(v));
}

export function paginaVacia<T>(): Pagina<T> {
  return { contenido: [], numero: 0, tamano: 0, totalElementos: 0, totalPaginas: 0, ultima: true };
}
