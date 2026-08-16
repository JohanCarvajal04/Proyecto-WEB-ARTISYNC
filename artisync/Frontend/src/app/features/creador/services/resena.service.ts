import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaResena } from '../models/creador.model';

/** Respuesta del endpoint de promedio: `{ promedio, total }`. */
export interface PromedioResenas {
  [clave: string]: number | string;
}

@Injectable({ providedIn: 'root' })
export class ResenaService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/creadores`;

  listarPorCreador(idPerfil: number): Observable<RespuestaResena[]> {
    return this.http.get<RespuestaResena[]>(`${this.API}/${idPerfil}/resenas`);
  }

  obtenerPromedio(idPerfil: number): Observable<PromedioResenas> {
    return this.http.get<PromedioResenas>(`${this.API}/${idPerfil}/resenas/promedio`);
  }
}
