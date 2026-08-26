import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  RespuestaPedido,
  RespuestaPedidoResumido,
  RespuestaHistorialEstado,
  RespuestaSeguimientoPedido,
  PeticionCrearPedido,
  PeticionAvanzarEtapa,
  PeticionActualizarTerminosPedido
} from '../models/pedido.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { FormatoReporte } from '../../../shared/models/formato-reporte.model';

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

  /**
   * Exportación "propia": sin permiso aparte, hereda el guard del propio
   * listado (ver PedidoControlador.exportarMisPedidos).
   */
  exportarMisPedidos(formato: FormatoReporte): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.API}/mis-pedidos/exportar`, {
      ...sinErrorGlobal(), params: { formato }, responseType: 'blob', observe: 'response'
    });
  }

  exportarMisComisiones(formato: FormatoReporte): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.API}/mis-comisiones/exportar`, {
      ...sinErrorGlobal(), params: { formato }, responseType: 'blob', observe: 'response'
    });
  }

  avanzarEtapa(id: number, peticion: PeticionAvanzarEtapa): Observable<RespuestaPedido> {
    return this.http.put<RespuestaPedido>(`${this.API}/${id}/avanzar`, peticion);
  }

  /** Negociación pre-firma: solo funciona mientras el contrato no tenga ninguna firma. */
  actualizarTerminos(id: number, peticion: PeticionActualizarTerminosPedido): Observable<RespuestaPedido> {
    return this.http.patch<RespuestaPedido>(`${this.API}/${id}/terminos`, peticion);
  }

  obtenerHistorial(id: number): Observable<RespuestaHistorialEstado[]> {
    return this.http.get<RespuestaHistorialEstado[]>(`${this.API}/${id}/historial`);
  }

  obtenerSeguimiento(id: number): Observable<RespuestaSeguimientoPedido> {
    return this.http.get<RespuestaSeguimientoPedido>(`${this.API}/${id}/seguimiento`);
  }
}
