import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { EventoAuditoria, EventoAuditoriaResumen, FiltroAuditoria } from '../models/auditoria.model';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';

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
    const params = this.aParams(filtro).set('page', page).set('size', size);
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
   * Un 422 (tope de 50 000 filas superado) llega con cuerpo Blob sin
   * parsear; el errorInterceptor no lo sabe leer y mostraría el mensaje
   * genérico. Se marca con sinErrorGlobal() y el componente decodifica el
   * blob de error él mismo.
   */
  exportarCsv(filtro: FiltroAuditoria): Observable<Blob> {
    const params = this.aParams(filtro);
    return this.http.get(`${this.API}/csv`, { ...sinErrorGlobal(), params, responseType: 'blob' });
  }

  private aParams(filtro: FiltroAuditoria): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return params;
  }
}
