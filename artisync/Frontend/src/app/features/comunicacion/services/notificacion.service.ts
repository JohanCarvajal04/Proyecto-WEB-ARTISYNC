import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { ConteoNoLeidas, RespuestaNotificacion } from '../models/comunicacion.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';

@Injectable({ providedIn: 'root' })
export class NotificacionService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/notificaciones`;

  /**
   * Contador compartido: lo escribe tanto el sondeo de la campana como las
   * acciones de marcar leído, para que el badge no quede desfasado respecto a
   * la lista que el usuario acaba de tocar.
   */
  readonly noLeidas = signal<number>(0);

  listar(page = 0, size = 20): Observable<Pagina<RespuestaNotificacion>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(this.API, { params })
      .pipe(map(crudo => normalizarPagina<RespuestaNotificacion>(crudo)));
  }

  marcarComoLeida(id: number): Observable<RespuestaNotificacion> {
    return this.http.put<RespuestaNotificacion>(`${this.API}/${id}/leer`, {})
      .pipe(tap(() => this.noLeidas.update(n => Math.max(0, n - 1))));
  }

  marcarTodasLeidas(): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.API}/leer-todas`, {})
      .pipe(tap(() => this.noLeidas.set(0)));
  }

  /**
   * Sondeo de la campana: se ejecuta cada minuto en segundo plano, sin que el
   * usuario haya pedido nada. Va con `sinErrorGlobal()` para que un fallo
   * suyo no genere avisos: repetido cada 60 s sería ruido constante, y el
   * badge simplemente conserva el último valor conocido.
   */
  contarNoLeidas(): Observable<number> {
    return this.http.get<ConteoNoLeidas>(`${this.API}/no-leidas/count`, sinErrorGlobal()).pipe(
      map(res => res?.noLeidas ?? 0),
      tap(n => this.noLeidas.set(n))
    );
  }
}
