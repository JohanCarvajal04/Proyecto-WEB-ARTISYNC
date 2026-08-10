import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RespuestaTicketRevision, PeticionCrearTicketRevision } from '../models/pedido.model';

@Injectable({ providedIn: 'root' })
export class TicketRevisionService {

  private readonly API = `${environment.apiUrl}/v1`;

  constructor(private http: HttpClient) {}

  crearTicket(idPedido: number, peticion: PeticionCrearTicketRevision): Observable<RespuestaTicketRevision> {
    return this.http.post<RespuestaTicketRevision>(`${this.API}/pedidos/${idPedido}/tickets-revision`, peticion);
  }

  listarTickets(idPedido: number): Observable<RespuestaTicketRevision[]> {
    return this.http.get<RespuestaTicketRevision[]>(`${this.API}/pedidos/${idPedido}/tickets-revision`);
  }

  cambiarEstado(idTicket: number, nuevoEstado: string): Observable<RespuestaTicketRevision> {
    const params = new HttpParams().set('nuevoEstado', nuevoEstado);
    return this.http.put<RespuestaTicketRevision>(`${this.API}/tickets-revision/${idTicket}/estado`, null, { params });
  }
}
