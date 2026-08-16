import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface RespuestaEstadoSeguimiento {
  estaSiguiendo: boolean;
  cantidadSeguidores: number;
}

export interface RespuestaSeguidor {
  idSeguimiento: number;
  idUsuarioSeguidor: number;
  nombreSeguidor: string;
  idPerfilCreador: number;
  notificacionesActivas: boolean;
  fechaSeguimiento: string;
}

export interface RespuestaMensaje {
  mensaje: string;
}

@Injectable({
  providedIn: 'root'
})
export class SeguidorService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/api/v1/creadores`;

  obtenerEstado(idPerfil: number): Observable<RespuestaEstadoSeguimiento> {
    return this.http.get<RespuestaEstadoSeguimiento>(`${this.url}/${idPerfil}/seguir/estado`);
  }

  seguir(idPerfil: number): Observable<RespuestaSeguidor> {
    return this.http.post<RespuestaSeguidor>(`${this.url}/${idPerfil}/seguir`, {});
  }

  dejarDeSeguir(idPerfil: number): Observable<RespuestaMensaje> {
    return this.http.delete<RespuestaMensaje>(`${this.url}/${idPerfil}/seguir`);
  }

  listarSeguidores(idPerfil: number): Observable<RespuestaSeguidor[]> {
    return this.http.get<RespuestaSeguidor[]>(`${this.url}/${idPerfil}/seguidores`);
  }
}
