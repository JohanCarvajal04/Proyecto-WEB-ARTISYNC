import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { Infraccion } from '../models/moderacion.model';

/**
 * Infracciones detectadas por el filtro de mensajes y suspensiones derivadas
 * (RF-15). Los tres endpoints son `hasRole('ADMIN')`: ni el moderador ni
 * soporte pueden consultarlos.
 */
@Injectable({ providedIn: 'root' })
export class InfraccionService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin`;

  listarInfracciones(page = 0, size = 20): Observable<Pagina<Infraccion>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(`${this.API}/infracciones`, { params })
      .pipe(map(crudo => normalizarPagina<Infraccion>(crudo)));
  }

  historialPorUsuario(idUsuario: number, page = 0, size = 20): Observable<Pagina<Infraccion>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(`${this.API}/infracciones/usuario/${idUsuario}`, { params })
      .pipe(map(crudo => normalizarPagina<Infraccion>(crudo)));
  }

  revertirSuspension(idUsuario: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.API}/suspensiones/${idUsuario}`);
  }
}
