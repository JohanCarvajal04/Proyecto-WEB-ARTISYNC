import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import {
  FiltrosCatalogo,
  RespuestaServicio,
  RespuestaServicioResumido,
  RespuestaCategoria,
  RespuestaSubcategoria,
  RespuestaEtiqueta,
  RespuestaPerfil
} from '../models/catalogo.model';

@Injectable({ providedIn: 'root' })
export class CatalogoPublicoService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1`;

  /** GET /api/v1/catalogo — búsqueda con filtros y paginación. */
  buscarCatalogo(filtros: FiltrosCatalogo): Observable<Pagina<RespuestaServicioResumido>> {
    let params = new HttpParams();

    if (filtros.categoria != null) params = params.set('categoria', filtros.categoria);
    if (filtros.subcategoria != null) params = params.set('subcategoria', filtros.subcategoria);
    if (filtros.precioMin != null) params = params.set('precioMin', filtros.precioMin);
    if (filtros.precioMax != null) params = params.set('precioMax', filtros.precioMax);
    if (filtros.q) params = params.set('q', filtros.q);
    if (filtros.sort) params = params.set('sort', filtros.sort);
    params = params.set('page', filtros.page ?? 0);
    params = params.set('size', filtros.size ?? 12);

    // `etiquetas` es List<Long> en el backend: se repite el parámetro.
    for (const idEtiqueta of filtros.etiquetas ?? []) {
      params = params.append('etiquetas', idEtiqueta);
    }

    return this.http.get(`${this.API}/catalogo`, { params })
      .pipe(map(crudo => normalizarPagina<RespuestaServicioResumido>(crudo)));
  }

  obtenerServicio(idServicio: number): Observable<RespuestaServicio> {
    return this.http.get<RespuestaServicio>(`${this.API}/servicios/${idServicio}`);
  }

  /** GET /api/v1/creadores/{id}/servicios — vitrina de un creador. */
  listarServiciosPorCreador(idPerfilCreador: number, estadoPublicacion?: string): Observable<RespuestaServicioResumido[]> {
    let params = new HttpParams();
    if (estadoPublicacion) params = params.set('estadoPublicacion', estadoPublicacion);
    return this.http.get<RespuestaServicioResumido[]>(
      `${this.API}/creadores/${idPerfilCreador}/servicios`, { params });
  }

  obtenerPerfilCreador(idPerfil: number): Observable<RespuestaPerfil> {
    return this.http.get<RespuestaPerfil>(`${this.API}/perfiles/${idPerfil}`);
  }

  /** GET /api/v1/perfiles/activos — directorio público de creadores activos. */
  listarCreadoresActivos(): Observable<RespuestaPerfil[]> {
    return this.http.get<RespuestaPerfil[]>(`${this.API}/perfiles/activos`);
  }

  listarCategorias(): Observable<RespuestaCategoria[]> {
    return this.http.get<RespuestaCategoria[]>(`${this.API}/categorias`);
  }

  listarSubcategoriasDeCategoria(idCategoria: number): Observable<RespuestaSubcategoria[]> {
    return this.http.get<RespuestaSubcategoria[]>(`${this.API}/categorias/${idCategoria}/subcategorias`);
  }

  listarEtiquetas(): Observable<RespuestaEtiqueta[]> {
    return this.http.get<RespuestaEtiqueta[]>(`${this.API}/etiquetas`);
  }

  /** POST /api/v1/portafolios/{id}/visita — contador de visitas del portafolio. */
  registrarVisitaPortafolio(idPortafolio: number): Observable<unknown> {
    return this.http.post(`${this.API}/portafolios/${idPortafolio}/visita`, {});
  }
}
