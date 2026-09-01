import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaEstadoLike } from '../models/social.model';

/**
 * "Me gusta" en ítems de portafolio. Dar/quitar like exige sesión; consultar
 * el estado es público (si no hay sesión, `meGusta` siempre viene en false).
 */
@Injectable({ providedIn: 'root' })
export class LikeService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/portafolio-items`;

  darLike(idItemPortafolio: number): Observable<RespuestaEstadoLike> {
    return this.http.post<RespuestaEstadoLike>(`${this.API}/${idItemPortafolio}/likes`, {});
  }

  quitarLike(idItemPortafolio: number): Observable<RespuestaEstadoLike> {
    return this.http.delete<RespuestaEstadoLike>(`${this.API}/${idItemPortafolio}/likes`);
  }

  obtenerEstado(idItemPortafolio: number): Observable<RespuestaEstadoLike> {
    return this.http.get<RespuestaEstadoLike>(`${this.API}/${idItemPortafolio}/likes`);
  }
}
