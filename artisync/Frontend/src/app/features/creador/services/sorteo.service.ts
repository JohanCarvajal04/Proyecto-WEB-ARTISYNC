import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import {
  RespuestaSorteo,
  RespuestaParticipante,
  RespuestaGanador,
  PeticionCrearSorteo,
  PeticionActualizarSorteo
} from '../models/creador.model';

@Injectable({ providedIn: 'root' })
export class SorteoService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1`;

  crear(peticion: PeticionCrearSorteo): Observable<RespuestaSorteo> {
    return this.http.post<RespuestaSorteo>(`${this.API}/sorteos`, peticion);
  }

  obtener(idSorteo: number): Observable<RespuestaSorteo> {
    return this.http.get<RespuestaSorteo>(`${this.API}/sorteos/${idSorteo}`);
  }

  actualizar(idSorteo: number, peticion: PeticionActualizarSorteo): Observable<RespuestaSorteo> {
    return this.http.put<RespuestaSorteo>(`${this.API}/sorteos/${idSorteo}`, peticion);
  }

  eliminar(idSorteo: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.API}/sorteos/${idSorteo}`);
  }

  listarPorCreador(idPerfil: number): Observable<RespuestaSorteo[]> {
    return this.http.get<RespuestaSorteo[]>(`${this.API}/creadores/${idPerfil}/sorteos`);
  }

  listarParticipantes(idSorteo: number): Observable<RespuestaParticipante[]> {
    return this.http.get<RespuestaParticipante[]>(`${this.API}/sorteos/${idSorteo}/participantes`);
  }

  listarGanadores(idSorteo: number): Observable<RespuestaGanador[]> {
    return this.http.get<RespuestaGanador[]>(`${this.API}/sorteos/${idSorteo}/ganadores`);
  }
}
