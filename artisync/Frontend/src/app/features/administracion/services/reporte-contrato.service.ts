import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { FilaReporteContrato, FiltroReporteContrato } from '../models/reporte-contrato.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { FormatoReporte } from '../../../shared/models/formato-reporte.model';

/** Reporte de contratos formalizados, espejo de ReporteContratoControlador.java. */
@Injectable({ providedIn: 'root' })
export class ReporteContratoService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin/reportes/contratos`;

  listar(filtro: FiltroReporteContrato, page = 0, size = 20): Observable<Pagina<FilaReporteContrato>> {
    const params = this.aParams(filtro).set('page', page).set('size', size);
    return this.http.get(this.API, { params })
      .pipe(map(crudo => normalizarPagina<FilaReporteContrato>(crudo)));
  }

  exportar(filtro: FiltroReporteContrato, formato: FormatoReporte): Observable<HttpResponse<Blob>> {
    const params = this.aParams(filtro).set('formato', formato);
    return this.http.get(`${this.API}/exportar`, {
      ...sinErrorGlobal(), params, responseType: 'blob', observe: 'response'
    });
  }

  private aParams(filtro: FiltroReporteContrato): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return params;
  }
}
