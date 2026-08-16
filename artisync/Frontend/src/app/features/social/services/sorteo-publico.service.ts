import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import { RespuestaGanador, RespuestaSorteo } from '../models/social.model';

/**
 * Lado público/participante de los sorteos (RF-23). El CRUD del creador ya vive
 * en `features/creador/services/sorteo.service`; aquí está lo que el cliente
 * necesita: descubrir sorteos activos e inscribirse.
 */
@Injectable({ providedIn: 'root' })
export class SorteoPublicoService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1`;

  listarActivos(): Observable<RespuestaSorteo[]> {
    return this.http.get<RespuestaSorteo[]>(`${this.API}/sorteos/activos`);
  }

  obtenerSorteo(idSorteo: number): Observable<RespuestaSorteo> {
    return this.http.get<RespuestaSorteo>(`${this.API}/sorteos/${idSorteo}`);
  }

  listarPorCreador(idPerfil: number): Observable<RespuestaSorteo[]> {
    return this.http.get<RespuestaSorteo[]>(`${this.API}/creadores/${idPerfil}/sorteos`);
  }

  participar(idSorteo: number): Observable<unknown> {
    return this.http.post(`${this.API}/sorteos/${idSorteo}/participar`, {});
  }

  cancelarParticipacion(idSorteo: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.API}/sorteos/${idSorteo}/participar`);
  }

  listarGanadores(idSorteo: number): Observable<RespuestaGanador[]> {
    return this.http.get<RespuestaGanador[]>(`${this.API}/sorteos/${idSorteo}/ganadores`);
  }
}
