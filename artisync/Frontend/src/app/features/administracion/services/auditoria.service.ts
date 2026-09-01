import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { EventoAuditoria, EventoAuditoriaResumen, FiltroAuditoria } from '../models/auditoria.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { FormatoReporte } from '../../../shared/models/formato-reporte.model';
import { paramsDesdeFiltro } from '../../../shared/utils/params-desde-filtro';

/**
 * Bitácora inmutable de eventos del sistema (REQ-NF-013). Solo lectura: no
 * hay ningún método de escritura, coherente con que /api/v1/admin/auditoria
 * en el backend solo declara verbos GET.
 */
@Injectable({ providedIn: 'root' })
export class AuditoriaService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin/auditoria`;

  listar(filtro: FiltroAuditoria, page = 0, size = 20): Observable<Pagina<EventoAuditoriaResumen>> {
    const params = paramsDesdeFiltro(filtro).set('page', page).set('size', size);
    return this.http.get(this.API, { params })
      .pipe(map(crudo => normalizarPagina<EventoAuditoriaResumen>(crudo)));
  }

  obtener(idEvento: number): Observable<EventoAuditoria> {
    return this.http.get<EventoAuditoria>(`${this.API}/${idEvento}`);
  }

  listarAcciones(): Observable<string[]> {
    return this.http.get<string[]>(`${this.API}/acciones`);
  }

  /**
   * Un 422 (tope de filas del formato elegido superado) llega con cuerpo
   * Blob sin parsear; el errorInterceptor no lo sabe leer y mostraría el
   * mensaje genérico. Se marca con sinErrorGlobal() y el componente
   * decodifica el blob de error él mismo (ver mensajeErrorBlob).
   *
   * `observe: 'response'` para poder leer el nombre de archivo real del
   * header Content-Disposition en descargarRespuesta().
   */
  exportar(filtro: FiltroAuditoria, formato: FormatoReporte): Observable<HttpResponse<Blob>> {
    const params = paramsDesdeFiltro(filtro).set('formato', formato);
    return this.http.get(`${this.API}/exportar`, {
      ...sinErrorGlobal(), params, responseType: 'blob', observe: 'response'
    });
  }
}
