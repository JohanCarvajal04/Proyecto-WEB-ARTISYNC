import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  VerificacionCola, VerificacionDetalle, DecisionVerificacion,
  CertificadoIa, Portafolio, Categoria, CrearCategoria, ActualizarCategoria,
  Subcategoria, CrearSubcategoria, Etiqueta, Comentario
} from '../models/moderacion.model';
import { MessageResponse } from '../../../shared/models/common.model';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { PortafolioItem } from '../../perfil/models/portafolio.model';

@Injectable({
  providedIn: 'root'
})
export class ModeracionService {
  private http = inject(HttpClient);

  // ─── Verificaciones ───

  listarColaVerificaciones(estado?: string, limite = 20, offset = 0): Observable<VerificacionCola[]> {
    let params = new HttpParams()
      .set('limite', limite.toString())
      .set('offset', offset.toString());
    if (estado) params = params.set('estado', estado);
    return this.http.get<VerificacionCola[]>(`${environment.apiUrl}/v1/verificaciones`, { params });
  }

  obtenerVerificacion(id: number): Observable<VerificacionDetalle> {
    return this.http.get<VerificacionDetalle>(`${environment.apiUrl}/v1/verificaciones/${id}`);
  }

  analizarConIa(id: number): Observable<VerificacionDetalle> {
    return this.http.post<VerificacionDetalle>(`${environment.apiUrl}/v1/verificaciones/${id}/analisis-ia`, {});
  }

  registrarDecision(id: number, decision: DecisionVerificacion): Observable<VerificacionDetalle> {
    return this.http.patch<VerificacionDetalle>(`${environment.apiUrl}/v1/verificaciones/${id}/decision`, decision);
  }

  obtenerDocumento(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/v1/verificaciones/${id}/documento`, { responseType: 'blob' });
  }

  // ─── Certificados IA ───

  listarCertificados(): Observable<CertificadoIa[]> {
    return this.http.get<CertificadoIa[]>(`${environment.apiUrl}/v1/certificados`);
  }

  obtenerCertificado(id: number): Observable<CertificadoIa> {
    return this.http.get<CertificadoIa>(`${environment.apiUrl}/v1/certificados/${id}`);
  }

  /**
   * Historial de certificados de un usuario (Cliente o Creador). Da contexto
   * al moderador antes de decidir: si ese usuario ya tuvo rechazos previos,
   * importa saberlo.
   */
  listarCertificadosDeUsuario(idUsuario: number): Observable<CertificadoIa[]> {
    return this.http.get<CertificadoIa[]>(`${environment.apiUrl}/v1/certificados/usuario/${idUsuario}`);
  }

  // ─── Portafolios (solo lectura) ───

  listarPortafolios(): Observable<Portafolio[]> {
    return this.http.get<Portafolio[]>(`${environment.apiUrl}/v1/portafolios`);
  }

  obtenerPortafolio(id: number): Observable<Portafolio> {
    return this.http.get<Portafolio>(`${environment.apiUrl}/v1/portafolios/${id}`);
  }

  /**
   * Obras de un portafolio. Sin esto el panel solo mostraba id, visibilidad y
   * visitas: no se puede moderar contenido que no se ve.
   */
  listarObrasPortafolio(idPortafolio: number): Observable<PortafolioItem[]> {
    return this.http.get<PortafolioItem[]>(`${environment.apiUrl}/v1/portafolios/${idPortafolio}/items`);
  }

  /**
   * Única acción destructiva disponible sobre un portafolio, y es
   * `hasRole('ADMIN')`: el MODERADOR no puede ejecutarla pese a tener
   * PORTAFOLIO_MODERAR.
   */
  eliminarPortafolio(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${environment.apiUrl}/v1/portafolios/${id}`);
  }

  // ─── Comentarios (COMENTARIO_MODERAR) ───

  listarComentarios(page = 0, size = 20): Observable<Pagina<Comentario>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(`${environment.apiUrl}/v1/admin/comentarios`, { params })
      .pipe(map(crudo => normalizarPagina<Comentario>(crudo)));
  }

  ocultarComentario(idComentario: number): Observable<Comentario> {
    return this.http.patch<Comentario>(`${environment.apiUrl}/v1/admin/comentarios/${idComentario}/ocultar`, {});
  }

  reactivarComentario(idComentario: number): Observable<Comentario> {
    return this.http.patch<Comentario>(`${environment.apiUrl}/v1/admin/comentarios/${idComentario}/reactivar`, {});
  }

  /** Borrado definitivo, reservado a ADMIN en el backend. */
  eliminarComentario(idComentario: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/v1/admin/comentarios/${idComentario}`);
  }

  // ─── Categorías ───

  /** Solo las activas: es el listado público que alimenta el catálogo. */
  listarCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${environment.apiUrl}/v1/categorias`);
  }

  /**
   * Incluye las desactivadas. Es el listado que necesita el panel de
   * moderación: con el endpoint público, una categoría desactivada
   * desaparecía de la tabla y ya no había forma de volver a activarla.
   * Gateado en CATEGORIA_GESTIONAR, que es justo el permiso del MODERADOR.
   */
  listarTodasLasCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${environment.apiUrl}/v1/categorias/todas`);
  }

  crearCategoria(data: CrearCategoria): Observable<Categoria> {
    return this.http.post<Categoria>(`${environment.apiUrl}/v1/categorias`, data);
  }

  actualizarCategoria(id: number, data: ActualizarCategoria): Observable<Categoria> {
    return this.http.put<Categoria>(`${environment.apiUrl}/v1/categorias/${id}`, data);
  }

  eliminarCategoria(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${environment.apiUrl}/v1/categorias/${id}`);
  }

  // ─── Subcategorías y etiquetas (solo ADMIN) ───

  listarSubcategorias(): Observable<Subcategoria[]> {
    return this.http.get<Subcategoria[]>(`${environment.apiUrl}/v1/subcategorias`);
  }

  crearSubcategoria(data: CrearSubcategoria): Observable<Subcategoria> {
    return this.http.post<Subcategoria>(`${environment.apiUrl}/v1/subcategorias`, data);
  }

  eliminarSubcategoria(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${environment.apiUrl}/v1/subcategorias/${id}`);
  }

  listarEtiquetas(): Observable<Etiqueta[]> {
    return this.http.get<Etiqueta[]>(`${environment.apiUrl}/v1/etiquetas`);
  }

  eliminarEtiqueta(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${environment.apiUrl}/v1/etiquetas/${id}`);
  }
}
