import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { PeticionCrearBriefingPlantilla, RespuestaBriefing } from '../models/comunicacion.model';

/**
 * Briefing (REQ-F-016 ampliado). El cuestionario se asigna a un servicio
 * (ver servicio-form) y el cliente lo responde al crear el pedido
 * (pedido-crear), no con un envío manual del creador después — por eso este
 * servicio ya no tiene enviarBriefing/responderBriefing.
 */
@Injectable({ providedIn: 'root' })
export class BriefingService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1`;

  /** Solo lectura: muestra el cuestionario ya respondido en el detalle del pedido. */
  obtenerBriefing(idPedido: number): Observable<RespuestaBriefing> {
    return this.http.get<RespuestaBriefing>(`${this.API}/pedidos/${idPedido}/briefing`, sinErrorGlobal());
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
}
