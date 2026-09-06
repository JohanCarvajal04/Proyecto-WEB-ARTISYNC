import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DatosPagoCreador } from '../models/retiro.model';

/**
 * Correo de PayPal donde el creador recibe sus retiros, espejo de
 * DatosPagoControlador.java. Ruta separada del perfil público a propósito
 * (ver justificación en el propio controlador del backend).
 */
@Injectable({ providedIn: 'root' })
export class DatosPagoService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/perfiles/mis-datos-pago`;

  obtener(): Observable<DatosPagoCreador> {
    return this.http.get<DatosPagoCreador>(this.API);
  }

  actualizarCorreoPaypal(correoPaypal: string): Observable<DatosPagoCreador> {
    return this.http.put<DatosPagoCreador>(this.API, { correoPaypal });
  }
}
