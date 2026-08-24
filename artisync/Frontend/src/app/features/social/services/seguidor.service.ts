import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface RespuestaEstadoSeguimiento {
  esSeguidor: boolean;
  totalSeguidores: number;
  esPropioPerfil: boolean;
}

export interface RespuestaSeguidorInfo {
  idUsuarioSeguidor: number;
  nombreSeguidor: string;
}

export interface RespuestaCreadorSeguidoNovedad {
  idPerfil: number;
  idUsuario: number;
  nombreCreador: string;
  handle: string;
  urlFotoPerfil?: string;
  tituloProfesional?: string;
  resumenNovedad: string;
  tipoNovedad: string;
  fechaNovedad: string;
}

@Injectable({
  providedIn: 'root'
})
export class SeguidorService {

  private http = inject(HttpClient);
  private API = `${environment.apiUrl}/v1/creadores`;

  seguir(idPerfil: number): Observable<RespuestaEstadoSeguimiento> {
    return this.http.post<RespuestaEstadoSeguimiento>(`${this.API}/${idPerfil}/seguir`, {});
  }

  dejarDeSeguir(idPerfil: number): Observable<RespuestaEstadoSeguimiento> {
    return this.http.delete<RespuestaEstadoSeguimiento>(`${this.API}/${idPerfil}/seguir`);
  }

  obtenerEstado(idPerfil: number): Observable<RespuestaEstadoSeguimiento> {
    return this.http.get<RespuestaEstadoSeguimiento>(`${this.API}/${idPerfil}/es-seguidor`);
  }

  listarSeguidores(idPerfil: number): Observable<RespuestaSeguidorInfo[]> {
    return this.http.get<RespuestaSeguidorInfo[]>(`${this.API}/${idPerfil}/seguidores`);
  }

  listarNovedadesSiguiendo(): Observable<RespuestaCreadorSeguidoNovedad[]> {
    return this.http.get<RespuestaCreadorSeguidoNovedad[]>(`${this.API}/siguiendo/novedades`);
  }

  actualizarPortada(urlPortada: string, tituloProfesional?: string): Observable<{ mensaje: string }> {
    const params: Record<string, string> = {};
    if (urlPortada) params['urlPortada'] = urlPortada;
    if (tituloProfesional) params['tituloProfesional'] = tituloProfesional;
    return this.http.put<{ mensaje: string }>(`${this.API}/mi-perfil/portada`, null, { params });
  }
}
