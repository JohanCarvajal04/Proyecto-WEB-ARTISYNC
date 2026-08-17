import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  RespuestaFlujoTrabajo,
  PeticionCrearFlujoTrabajo,
  PeticionEtapaConfig
} from '../models/pedido.model';

@Injectable({ providedIn: 'root' })
export class FlujoTrabajoService {

  private readonly API = `${environment.apiUrl}/v1/flujos`;

  constructor(private http: HttpClient) {}

  listarFlujos(): Observable<RespuestaFlujoTrabajo[]> {
    return this.http.get<RespuestaFlujoTrabajo[]>(this.API);
  }

  obtenerFlujo(id: number): Observable<RespuestaFlujoTrabajo> {
    return this.http.get<RespuestaFlujoTrabajo>(`${this.API}/${id}`);
  }

  crearFlujo(peticion: PeticionCrearFlujoTrabajo): Observable<RespuestaFlujoTrabajo> {
    return this.http.post<RespuestaFlujoTrabajo>(this.API, peticion);
  }

  actualizarFlujo(id: number, peticion: PeticionCrearFlujoTrabajo): Observable<RespuestaFlujoTrabajo> {
    return this.http.put<RespuestaFlujoTrabajo>(`${this.API}/${id}`, peticion);
  }

  agregarEtapa(idFlujo: number, peticion: PeticionEtapaConfig): Observable<RespuestaFlujoTrabajo> {
    return this.http.post<RespuestaFlujoTrabajo>(`${this.API}/${idFlujo}/etapas`, peticion);
  }

  actualizarEtapa(idFlujo: number, idEtapa: number, peticion: PeticionEtapaConfig): Observable<RespuestaFlujoTrabajo> {
    return this.http.put<RespuestaFlujoTrabajo>(`${this.API}/${idFlujo}/etapas/${idEtapa}`, peticion);
  }

  eliminarEtapa(idFlujo: number, idEtapa: number): Observable<any> {
    return this.http.delete(`${this.API}/${idFlujo}/etapas/${idEtapa}`);
  }
}
