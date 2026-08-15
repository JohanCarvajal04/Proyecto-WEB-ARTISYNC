import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BriefingEnviado, PeticionResponderBriefing } from '../models/comunicacion.model';

@Injectable({
  providedIn: 'root'
})
export class BriefingService {
  private apiUrl = '/api/v1/pedidos';

  constructor(private http: HttpClient) {}

  public obtenerBriefing(idPedido: number): Observable<BriefingEnviado> {
    return this.http.get<BriefingEnviado>(`${this.apiUrl}/${idPedido}/briefing`);
  }

  public responderBriefing(idPedido: number, peticion: PeticionResponderBriefing): Observable<any> {
    return this.http.post(`${this.apiUrl}/${idPedido}/briefing/respuestas`, peticion);
  }
}
