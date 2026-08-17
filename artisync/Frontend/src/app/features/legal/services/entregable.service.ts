import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaEntregable } from '../models/legal.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';

@Injectable({ providedIn: 'root' })
export class EntregableService {

  private readonly API = `${environment.apiUrl}/v1/pedidos`;

  constructor(private http: HttpClient) {}

  /**
   * El backend recibe las dos versiones como multipart, cada una en su propia
   * parte (`versionMarcaAgua` y `versionLimpia`), y las persiste con el
   * servicio de almacenamiento. Lo que se guarda es una referencia interna, no
   * una URL que el creador pueda teclear.
   */
  subirEntregable(idPedido: number, versionMarcaAgua: File, versionLimpia: File): Observable<RespuestaEntregable> {
    const formData = new FormData();
    formData.append('versionMarcaAgua', versionMarcaAgua);
    formData.append('versionLimpia', versionLimpia);

    // Sin Content-Type explícito: el navegador debe fijarlo junto al boundary.
    return this.http.post<RespuestaEntregable>(`${this.API}/${idPedido}/entregable`, formData);
  }

  /**
   * 404 mientras el creador no haya subido nada: estado normal, lo maneja la
   * vista y no el interceptor global.
   */
  obtenerEntregable(idPedido: number): Observable<RespuestaEntregable> {
    return this.http.get<RespuestaEntregable>(`${this.API}/${idPedido}/entregable`, sinErrorGlobal());
  }

  aprobarEntrega(idPedido: number): Observable<any> {
    return this.http.post(`${this.API}/${idPedido}/aprobar`, {});
  }

  descargarVersionLimpia(idPedido: number): Observable<Blob> {
    return this.http.get(`${this.API}/${idPedido}/entregable/descargar`, { responseType: 'blob' });
  }

  /**
   * La previsualización pasa por aquí y no por un `<img src>` directo: cuando
   * el almacenamiento es local, `urlVersionMarcaAgua` es la ruta de este mismo
   * endpoint, que exige el JWT y responde como attachment.
   */
  descargarVersionMarcaAgua(idPedido: number): Observable<Blob> {
    return this.http.get(`${this.API}/${idPedido}/entregable/descargar/marca-agua`, { responseType: 'blob' });
  }
}
