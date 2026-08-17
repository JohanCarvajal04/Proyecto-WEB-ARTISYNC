import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import {
  PeticionCrearBriefingPlantilla,
  PeticionEnviarBriefing,
  PeticionResponderBriefing,
  RespuestaBriefing,
  RespuestaItemBriefing
} from '../models/comunicacion.model';

/**
 * Briefing (RF-16). Cubre los dos lados:
 *  - Creador: gestiona sus plantillas y envía una a un pedido.
 *  - Cliente: consulta el formulario recibido y lo responde (inmutable).
 */
@Injectable({ providedIn: 'root' })
export class BriefingService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1`;

  // ── Lado cliente ──────────────────────────────────────────────────────────

  /**
   * 404 mientras el creador no haya enviado briefing a este pedido: estado
   * normal, lo maneja la vista y no el interceptor global.
   */
  obtenerBriefing(idPedido: number): Observable<RespuestaBriefing> {
    return this.http.get<RespuestaBriefing>(`${this.API}/pedidos/${idPedido}/briefing`, sinErrorGlobal());
  }

  responderBriefing(idPedido: number, respuestas: RespuestaItemBriefing[]): Observable<RespuestaBriefing> {
    const peticion: PeticionResponderBriefing = { respuestas };
    return this.http.post<RespuestaBriefing>(`${this.API}/pedidos/${idPedido}/briefing/responder`, peticion);
  }

  // ── Lado creador: plantillas ──────────────────────────────────────────────

  /** El backend resuelve el perfil del creador desde el JWT. */
  listarMisPlantillas(): Observable<RespuestaBriefing[]> {
    return this.http.get<RespuestaBriefing[]>(`${this.API}/briefing/plantillas`);
  }

  crearPlantilla(peticion: PeticionCrearBriefingPlantilla): Observable<RespuestaBriefing> {
    return this.http.post<RespuestaBriefing>(`${this.API}/briefing/plantillas`, peticion);
  }

  editarPlantilla(idPlantilla: number, peticion: PeticionCrearBriefingPlantilla): Observable<RespuestaBriefing> {
    return this.http.put<RespuestaBriefing>(`${this.API}/briefing/plantillas/${idPlantilla}`, peticion);
  }

  eliminarPlantilla(idPlantilla: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.API}/briefing/plantillas/${idPlantilla}`);
  }

  /** Envía una plantilla al cliente de un pedido concreto. */
  enviarBriefing(idPedido: number, idBriefingPlantilla: number): Observable<RespuestaBriefing> {
    const peticion: PeticionEnviarBriefing = { idBriefingPlantilla };
    return this.http.post<RespuestaBriefing>(`${this.API}/pedidos/${idPedido}/briefing`, peticion);
  }
}
