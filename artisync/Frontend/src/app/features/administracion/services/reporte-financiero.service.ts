import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { FiltroReporteFinanciero, RespuestaReporteComisiones } from '../models/reporte-financiero.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { FormatoReporte } from '../../../shared/models/formato-reporte.model';

/**
 * Reporte de comisiones por creador (bruto, comisión, neto y detalle),
 * espejo de ReporteFinancieroControlador.java. Solo lectura + exportación.
 */
@Injectable({ providedIn: 'root' })
export class ReporteFinancieroService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin/reportes/finanzas`;

  obtener(filtro: FiltroReporteFinanciero): Observable<RespuestaReporteComisiones> {
    return this.http.get<RespuestaReporteComisiones>(this.API, { params: this.aParams(filtro) });
  }

  /**
   * Igual que AuditoriaService.exportar(): sinErrorGlobal() porque un 422
   * (tope de filas) llega como Blob y lo decodifica el propio componente.
   */
  exportar(filtro: FiltroReporteFinanciero, formato: FormatoReporte): Observable<HttpResponse<Blob>> {
    const params = this.aParams(filtro).set('formato', formato);
    return this.http.get(`${this.API}/exportar`, {
      ...sinErrorGlobal(), params, responseType: 'blob', observe: 'response'
    });
  }

  private aParams(filtro: FiltroReporteFinanciero): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return params;
  }
}
