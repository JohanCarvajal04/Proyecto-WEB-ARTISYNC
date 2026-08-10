import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaContrato, RespuestaEstadoFirma } from '../models/legal.model';

@Injectable({ providedIn: 'root' })
export class ContratoService {

  private readonly API = `${environment.apiUrl}/v1/contratos`;

  constructor(private http: HttpClient) {}

  generarContrato(idPedido: number): Observable<RespuestaContrato> {
    return this.http.post<RespuestaContrato>(`${this.API}/pedido/${idPedido}`, {});
  }

  obtenerContrato(id: number): Observable<RespuestaContrato> {
    return this.http.get<RespuestaContrato>(`${this.API}/${id}`);
  }

  obtenerContratoPorPedido(idPedido: number): Observable<RespuestaContrato> {
    return this.http.get<RespuestaContrato>(`${this.API}/pedido/${idPedido}`);
  }

  firmarContrato(id: number): Observable<RespuestaContrato> {
    return this.http.post<RespuestaContrato>(`${this.API}/${id}/firmar`, {});
  }

  obtenerEstadoFirma(id: number): Observable<RespuestaEstadoFirma> {
    return this.http.get<RespuestaEstadoFirma>(`${this.API}/${id}/estado-firma`);
  }

  descargarPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.API}/${id}/pdf`, { responseType: 'blob' });
  }
}
