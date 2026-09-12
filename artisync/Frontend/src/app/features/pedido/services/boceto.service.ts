import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaBoceto } from '../models/pedido.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';

@Injectable({ providedIn: 'root' })
export class BocetoService {

  private readonly API = `${environment.apiUrl}/v1/pedidos`;

  constructor(private http: HttpClient) {}

  /**
   * Sube (o resube, reemplazando el anterior) el boceto del pedido. Es un
   * singleton por pedido, igual que el entregable final.
   */
  subirBoceto(idPedido: number, imagen: File): Observable<RespuestaBoceto> {
    const formData = new FormData();
    formData.append('imagen', imagen);

    return this.http.post<RespuestaBoceto>(`${this.API}/${idPedido}/boceto`, formData);
  }

  /**
   * 404 mientras el creador no haya subido nada: estado normal, lo maneja la
   * vista y no el interceptor global.
   */
  obtenerBoceto(idPedido: number): Observable<RespuestaBoceto> {
    return this.http.get<RespuestaBoceto>(`${this.API}/${idPedido}/boceto`, sinErrorGlobal());
  }

  /**
   * Igual que la previsualización del entregable: pasa por aquí y no por un
   * `<img src>` directo, porque en almacenamiento local la URL exige el JWT.
   */
  descargarBoceto(idPedido: number): Observable<Blob> {
    return this.http.get(`${this.API}/${idPedido}/boceto/descargar`, { responseType: 'blob' });
  }
}
