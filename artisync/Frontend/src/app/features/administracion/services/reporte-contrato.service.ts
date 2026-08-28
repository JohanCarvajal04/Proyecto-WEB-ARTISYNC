import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { FilaReporteContrato, FiltroReporteContrato } from '../models/reporte-contrato.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { FormatoReporte } from '../../../shared/models/formato-reporte.model';
import { paramsDesdeFiltro } from '../../../shared/utils/params-desde-filtro';

/** Reporte de contratos formalizados, espejo de ReporteContratoControlador.java. */
@Injectable({ providedIn: 'root' })
export class ReporteContratoService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin/reportes/contratos`;

  listar(filtro: FiltroReporteContrato, page = 0, size = 20): Observable<Pagina<FilaReporteContrato>> {
    const params = paramsDesdeFiltro(filtro).set('page', page).set('size', size);
    return this.http.get(this.API, { params })
      .pipe(map(crudo => normalizarPagina<FilaReporteContrato>(crudo)));
  }

  exportar(filtro: FiltroReporteContrato, formato: FormatoReporte): Observable<HttpResponse<Blob>> {
    const params = paramsDesdeFiltro(filtro).set('formato', formato);
    return this.http.get(`${this.API}/exportar`, {
      ...sinErrorGlobal(), params, responseType: 'blob', observe: 'response'
    });
  }
}
