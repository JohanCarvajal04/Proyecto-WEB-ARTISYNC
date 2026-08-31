import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { FiltroPagoGarantia, PagoGarantia, PagoGarantiaDetalle, ResumenEscrow } from '../models/pago-garantia.model';

/**
 * Supervisión de Pagos y Garantías (Escrow) para el Auditor Financiero
 * (PAGO_AUDITAR), espejo de PagoGarantiaAuditoriaControlador.java. Solo
 * lectura: no hay endpoint para liberar fondos manualmente desde aquí, eso
 * sigue siendo automático vía aprobación del entregable.
 */
@Injectable({ providedIn: 'root' })
export class PagoGarantiaService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin/pagos-garantia`;

  listar(filtro: FiltroPagoGarantia, page = 0, size = 20): Observable<Pagina<PagoGarantia>> {
    let params = this.aParams(filtro).set('page', page).set('size', size);
    return this.http.get(this.API, { params })
      .pipe(map(crudo => normalizarPagina<PagoGarantia>(crudo)));
  }

  obtenerDetalle(idPago: number): Observable<PagoGarantiaDetalle> {
    return this.http.get<PagoGarantiaDetalle>(`${this.API}/${idPago}`);
  }

  obtenerResumen(): Observable<ResumenEscrow[]> {
    return this.http.get<ResumenEscrow[]>(`${this.API}/resumen`);
  }

  private aParams(filtro: FiltroPagoGarantia): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return params;
  }
}
