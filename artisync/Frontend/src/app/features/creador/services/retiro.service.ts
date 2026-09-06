import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SaldoCreador, SolicitudRetiro } from '../models/retiro.model';

/** Espejo de SolicitudRetiroControlador.java (lado creador). */
@Injectable({ providedIn: 'root' })
export class RetiroService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/retiros`;

  obtenerSaldo(): Observable<SaldoCreador> {
    return this.http.get<SaldoCreador>(`${this.API}/saldo`);
  }

  solicitar(montoSolicitado: number): Observable<SolicitudRetiro> {
    return this.http.post<SolicitudRetiro>(this.API, { montoSolicitado });
  }

  misSolicitudes(): Observable<SolicitudRetiro[]> {
    return this.http.get<SolicitudRetiro[]>(`${this.API}/mis-solicitudes`);
  }
}
