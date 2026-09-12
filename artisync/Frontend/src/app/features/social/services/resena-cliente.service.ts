import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PeticionCrearResena, RespuestaResena } from '../models/social.model';

/**
 * Lado cliente de las reseñas (RF-09). Las lecturas públicas ya las cubre
 * `features/creador/services/resena.service`; aquí solo vive la escritura, que
 * es lo que el cliente puede hacer y no estaba conectado.
 */
@Injectable({ providedIn: 'root' })
export class ResenaClienteService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/pedidos`;

  crearResena(idPedido: number, peticion: PeticionCrearResena): Observable<RespuestaResena> {
    return this.http.post<RespuestaResena>(`${this.API}/${idPedido}/resena`, peticion);
  }

  /** Devuelve la reseña del pedido si ya existe, o null (404 → sin reseña aún). */
  obtenerMiResena(idPedido: number): Observable<RespuestaResena | null> {
    return this.http.get<RespuestaResena>(`${this.API}/${idPedido}/resena`).pipe(
      catchError(() => of(null))
    );
  }

  actualizarResena(idPedido: number, peticion: PeticionCrearResena): Observable<RespuestaResena> {
    return this.http.put<RespuestaResena>(`${this.API}/${idPedido}/resena`, peticion);
  }

  eliminarResena(idPedido: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${idPedido}/resena`);
  }
}
