import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaPerfil, PeticionCrearPerfil, PeticionActualizarPerfil } from '../models/creador.model';

@Injectable({ providedIn: 'root' })
export class PerfilCreadorService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/perfiles`;

  crear(peticion: PeticionCrearPerfil): Observable<RespuestaPerfil> {
    return this.http.post<RespuestaPerfil>(this.API, peticion);
  }

  obtenerPorId(idPerfil: number): Observable<RespuestaPerfil> {
    return this.http.get<RespuestaPerfil>(`${this.API}/${idPerfil}`);
  }

  obtenerPorUsuario(idUsuario: number): Observable<RespuestaPerfil> {
    return this.http.get<RespuestaPerfil>(`${this.API}/usuario/${idUsuario}`);
  }

  actualizar(idPerfil: number, peticion: PeticionActualizarPerfil): Observable<RespuestaPerfil> {
    return this.http.put<RespuestaPerfil>(`${this.API}/${idPerfil}`, peticion);
  }
}
