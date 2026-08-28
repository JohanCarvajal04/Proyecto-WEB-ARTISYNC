import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { FiltroReporteFinanciero, RespuestaReporteComisiones } from '../models/reporte-financiero.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { FormatoReporte } from '../../../shared/models/formato-reporte.model';
import { paramsDesdeFiltro } from '../../../shared/utils/params-desde-filtro';

/**
 * Reporte de comisiones por creador (bruto, comisión, neto y detalle),
 * espejo de ReporteFinancieroControlador.java. Solo lectura + exportación.
 */
@Injectable({ providedIn: 'root' })
export class ReporteFinancieroService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin/reportes/finanzas`;

  obtener(filtro: FiltroReporteFinanciero): Observable<RespuestaReporteComisiones> {
    return this.http.get<RespuestaReporteComisiones>(this.API, { params: paramsDesdeFiltro(filtro) });
  }

  /**
   * Igual que AuditoriaService.exportar(): sinErrorGlobal() porque un 422
   * (tope de filas) llega como Blob y lo decodifica el propio componente.
   */
  exportar(filtro: FiltroReporteFinanciero, formato: FormatoReporte): Observable<HttpResponse<Blob>> {
    const params = paramsDesdeFiltro(filtro).set('formato', formato);
    return this.http.get(`${this.API}/exportar`, {
      ...sinErrorGlobal(), params, responseType: 'blob', observe: 'response'
    });
  }
}
