import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { PeticionCrearComentario, RespuestaComentario } from '../models/social.model';

/**
 * Comentarios sobre ítems de portafolio. Publicar y eliminar exigen sesión;
 * listar y contar son públicos, igual que las reseñas.
 */
@Injectable({ providedIn: 'root' })
export class ComentarioService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/portafolio-items`;

  crearComentario(idItemPortafolio: number, peticion: PeticionCrearComentario): Observable<RespuestaComentario> {
    return this.http.post<RespuestaComentario>(`${this.API}/${idItemPortafolio}/comentarios`, peticion);
  }

  listarComentarios(idItemPortafolio: number, page = 0, size = 20): Observable<Pagina<RespuestaComentario>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'fechaPublicacion,desc');
    return this.http.get(`${this.API}/${idItemPortafolio}/comentarios`, { params })
      .pipe(map(crudo => normalizarPagina<RespuestaComentario>(crudo)));
  }

  contarComentarios(idItemPortafolio: number): Observable<{ idItemPortafolio: number; total: number }> {
    return this.http.get<{ idItemPortafolio: number; total: number }>(`${this.API}/${idItemPortafolio}/comentarios/conteo`);
  }

  eliminarComentario(idComentario: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/comentarios/${idComentario}`);
  }
}
