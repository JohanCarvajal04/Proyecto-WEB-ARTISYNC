import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaCategoria, RespuestaSubcategoria, RespuestaEtiqueta } from '../models/creador.model';

/** Catálogos auxiliares que alimentan el formulario de servicios. */
@Injectable({ providedIn: 'root' })
export class CatalogoService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1`;

  listarCategorias(): Observable<RespuestaCategoria[]> {
    return this.http.get<RespuestaCategoria[]>(`${this.API}/categorias`);
  }

  listarSubcategorias(): Observable<RespuestaSubcategoria[]> {
    return this.http.get<RespuestaSubcategoria[]>(`${this.API}/subcategorias`);
  }

  listarSubcategoriasPorCategoria(idCategoria: number): Observable<RespuestaSubcategoria[]> {
    return this.http.get<RespuestaSubcategoria[]>(`${this.API}/categorias/${idCategoria}/subcategorias`);
  }

  listarEtiquetas(): Observable<RespuestaEtiqueta[]> {
    return this.http.get<RespuestaEtiqueta[]>(`${this.API}/etiquetas`);
  }

  /**
   * POST /api/v1/etiquetas está permitido a CREADOR y ADMIN: el creador puede
   * dar de alta la etiqueta que le falte sin depender de un administrador.
   */
  crearEtiqueta(nombreEtiqueta: string): Observable<RespuestaEtiqueta> {
    return this.http.post<RespuestaEtiqueta>(`${this.API}/etiquetas`, { nombreEtiqueta });
  }
}
