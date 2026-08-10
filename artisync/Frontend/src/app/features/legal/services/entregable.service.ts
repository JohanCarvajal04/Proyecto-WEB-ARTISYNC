import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaEntregable } from '../models/legal.model';

@Injectable({ providedIn: 'root' })
export class EntregableService {

  private readonly API = `${environment.apiUrl}/v1/pedidos`;

  constructor(private http: HttpClient) {}

  subirEntregable(idPedido: number, urlMarcaAgua: string, urlLimpia: string): Observable<RespuestaEntregable> {
    const params = { urlMarcaAgua, urlLimpia };
    return this.http.post<RespuestaEntregable>(`${this.API}/${idPedido}/entregable`, null, { params });
  }

  obtenerEntregable(idPedido: number): Observable<RespuestaEntregable> {
    return this.http.get<RespuestaEntregable>(`${this.API}/${idPedido}/entregable`);
  }

  aprobarEntrega(idPedido: number): Observable<any> {
    return this.http.post(`${this.API}/${idPedido}/aprobar`, {});
  }

  descargarVersionLimpia(idPedido: number): Observable<Blob> {
    return this.http.get(`${this.API}/${idPedido}/entregable/descargar`, { responseType: 'blob' });
  }
}
