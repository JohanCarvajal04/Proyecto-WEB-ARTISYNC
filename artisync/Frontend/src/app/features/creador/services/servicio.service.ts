import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import {
  RespuestaServicio,
  RespuestaServicioResumido,
  RespuestaAtributo,
  PeticionCrearServicio,
  PeticionActualizarServicio,
  PeticionCrearAtributo,
  PeticionActualizarAtributo
} from '../models/creador.model';

@Injectable({ providedIn: 'root' })
export class ServicioService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/servicios`;

  crear(idPerfilCreador: number, peticion: PeticionCrearServicio): Observable<RespuestaServicio> {
    return this.http.post<RespuestaServicio>(`${this.API}/creador/${idPerfilCreador}`, peticion);
  }

  actualizar(idServicio: number, peticion: PeticionActualizarServicio): Observable<RespuestaServicio> {
    return this.http.put<RespuestaServicio>(`${this.API}/${idServicio}`, peticion);
  }

  obtenerPorId(idServicio: number): Observable<RespuestaServicio> {
    return this.http.get<RespuestaServicio>(`${this.API}/${idServicio}`);
  }

  eliminar(idServicio: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.API}/${idServicio}`);
  }

  listarPorCreador(idPerfilCreador: number, estadoPublicacion?: string): Observable<RespuestaServicioResumido[]> {
    let params = new HttpParams();
    if (estadoPublicacion) {
      params = params.set('estadoPublicacion', estadoPublicacion);
    }
    return this.http.get<RespuestaServicioResumido[]>(`${this.API}/creador/${idPerfilCreador}`, { params });
  }

  // ── Atributos del servicio ──

  listarAtributos(idServicio: number): Observable<RespuestaAtributo[]> {
    return this.http.get<RespuestaAtributo[]>(`${this.API}/${idServicio}/atributos`);
  }

  agregarAtributo(idServicio: number, peticion: PeticionCrearAtributo): Observable<RespuestaAtributo> {
    return this.http.post<RespuestaAtributo>(`${this.API}/${idServicio}/atributos`, peticion);
  }

  actualizarAtributo(idServicio: number, idAtributo: number, peticion: PeticionActualizarAtributo): Observable<RespuestaAtributo> {
    return this.http.put<RespuestaAtributo>(`${this.API}/${idServicio}/atributos/${idAtributo}`, peticion);
  }

  eliminarAtributo(idServicio: number, idAtributo: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.API}/${idServicio}/atributos/${idAtributo}`);
  }
}
