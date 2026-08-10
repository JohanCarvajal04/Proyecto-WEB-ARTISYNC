import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaPago } from '../models/legal.model';

@Injectable({ providedIn: 'root' })
export class PagoService {

  private readonly API = `${environment.apiUrl}/v1/pedidos`;

  constructor(private http: HttpClient) {}

  crearOrdenPago(idPedido: number): Observable<RespuestaPago> {
    return this.http.post<RespuestaPago>(`${this.API}/${idPedido}/pago`, {});
  }

  obtenerEstadoPago(idPedido: number): Observable<RespuestaPago> {
    return this.http.get<RespuestaPago>(`${this.API}/${idPedido}/pago/estado`);
  }
}
