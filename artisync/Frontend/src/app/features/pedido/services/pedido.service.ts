import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  RespuestaPedido,
  RespuestaPedidoResumido,
  RespuestaHistorialEstado,
  RespuestaSeguimientoPedido,
  PeticionCrearPedido,
  PeticionAvanzarEtapa
} from '../models/pedido.model';

@Injectable({ providedIn: 'root' })
export class PedidoService {

  private readonly API = `${environment.apiUrl}/v1/pedidos`;

  constructor(private http: HttpClient) {}

  crearPedido(peticion: PeticionCrearPedido): Observable<RespuestaPedido> {
    return this.http.post<RespuestaPedido>(this.API, peticion);
  }

  obtenerPedido(id: number): Observable<RespuestaPedido> {
    return this.http.get<RespuestaPedido>(`${this.API}/${id}`);
  }

  listarMisPedidos(): Observable<RespuestaPedidoResumido[]> {
    return this.http.get<RespuestaPedidoResumido[]>(`${this.API}/mis-pedidos`);
  }

  listarMisComisiones(): Observable<RespuestaPedidoResumido[]> {
    return this.http.get<RespuestaPedidoResumido[]>(`${this.API}/mis-comisiones`);
  }

  avanzarEtapa(id: number, peticion: PeticionAvanzarEtapa): Observable<RespuestaPedido> {
    return this.http.put<RespuestaPedido>(`${this.API}/${id}/avanzar`, peticion);
  }

  obtenerHistorial(id: number): Observable<RespuestaHistorialEstado[]> {
    return this.http.get<RespuestaHistorialEstado[]>(`${this.API}/${id}/historial`);
  }

  obtenerSeguimiento(id: number): Observable<RespuestaSeguimientoPedido> {
    return this.http.get<RespuestaSeguimientoPedido>(`${this.API}/${id}/seguimiento`);
  }
}
