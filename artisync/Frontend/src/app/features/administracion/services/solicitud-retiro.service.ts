import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { FiltroSolicitudRetiro, SolicitudRetiro } from '../models/solicitud-retiro.model';

/**
 * Cola de revisión de retiros para el Auditor Financiero (RETIROS_GESTIONAR),
 * espejo de SolicitudRetiroAdminControlador.java.
 */
@Injectable({ providedIn: 'root' })
export class SolicitudRetiroService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin/retiros`;

  listar(filtro: FiltroSolicitudRetiro, page = 0, size = 20): Observable<Pagina<SolicitudRetiro>> {
    let params = this.aParams(filtro).set('page', page).set('size', size);
    return this.http.get(this.API, { params })
      .pipe(map(crudo => normalizarPagina<SolicitudRetiro>(crudo)));
  }

  aprobar(idSolicitud: number): Observable<SolicitudRetiro> {
    return this.http.post<SolicitudRetiro>(`${this.API}/${idSolicitud}/aprobar`, {});
  }

  rechazar(idSolicitud: number, notaAdmin: string): Observable<SolicitudRetiro> {
    return this.http.post<SolicitudRetiro>(`${this.API}/${idSolicitud}/rechazar`, { notaAdmin });
  }

  reintentar(idSolicitud: number): Observable<SolicitudRetiro> {
    return this.http.post<SolicitudRetiro>(`${this.API}/${idSolicitud}/reintentar`, {});
  }

  private aParams(filtro: FiltroSolicitudRetiro): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return params;
  }
}
