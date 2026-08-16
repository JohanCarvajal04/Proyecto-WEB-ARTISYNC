import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { PeticionEnviarMensaje, RespuestaMensajeChat, RespuestaSalaChat } from '../models/comunicacion.model';

/**
 * Chat por pedido (RF-14).
 *
 * Usa la ruta REST, que el backend documenta como «fallback sin WebSocket».
 * El transporte STOMP en tiempo real existe en el servidor, pero consumirlo
 * exigiría añadir `@stomp/stompjs` + `sockjs-client` al proyecto; mientras esa
 * dependencia no se apruebe, la vista sondea el historial.
 */
@Injectable({ providedIn: 'root' })
export class ChatService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/pedidos`;

  /**
   * Mientras no se firme el contrato no hay sala y el backend responde 404.
   * Es un estado normal, así que el error lo maneja la vista y no el
   * interceptor global.
   */
  obtenerMensajes(idPedido: number, page = 0, size = 50): Observable<Pagina<RespuestaMensajeChat>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(`${this.API}/${idPedido}/chat/mensajes`, { params, ...sinErrorGlobal() })
      .pipe(map(crudo => normalizarPagina<RespuestaMensajeChat>(crudo)));
  }

  enviarMensaje(idPedido: number, cuerpoMensaje: string): Observable<RespuestaMensajeChat> {
    const peticion: PeticionEnviarMensaje = { cuerpoMensaje };
    return this.http.post<RespuestaMensajeChat>(`${this.API}/${idPedido}/chat/mensajes`, peticion);
  }

  obtenerEstadoSala(idPedido: number): Observable<RespuestaSalaChat> {
    return this.http.get<RespuestaSalaChat>(`${this.API}/${idPedido}/chat/estado`, sinErrorGlobal());
  }
}
