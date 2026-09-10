import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { sinErrorGlobal } from '../../../core/interceptors/http-contexto';
import { paramsDesdeFiltro } from '../../../shared/utils/params-desde-filtro';
import { PagedResponse } from '../../../shared/models/common.model';
import {
  ActualizarProgramacionRequest,
  CrearProgramacionRequest,
  CrearRespaldoRequest,
  FiltroRespaldo,
  ProgramacionRespaldoResponse,
  RespaldoResponse
} from '../models/respaldo.model';

/**
 * Respaldos de base de datos (REQ-NF-024), espejo de RespaldoControlador.java.
 * La descarga sigue el mismo patrón que ReporteFinancieroService.exportar():
 * sinErrorGlobal() porque el componente decodifica su propio error.
 */
@Injectable({ providedIn: 'root' })
export class RespaldoService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/admin/respaldos`;

  crear(request: CrearRespaldoRequest): Observable<RespaldoResponse> {
    return this.http.post<RespaldoResponse>(this.API, request);
  }

  listar(filtro: FiltroRespaldo, page = 0, size = 10): Observable<PagedResponse<RespaldoResponse>> {
    const params = paramsDesdeFiltro(filtro).set('page', page).set('size', size);
    return this.http.get<PagedResponse<RespaldoResponse>>(this.API, { params });
  }

  eliminar(idRespaldo: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${idRespaldo}`);
  }

  descargar(idRespaldo: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.API}/${idRespaldo}/descargar`, {
      ...sinErrorGlobal(), responseType: 'blob', observe: 'response'
    });
  }

  listarProgramaciones(): Observable<ProgramacionRespaldoResponse[]> {
    return this.http.get<ProgramacionRespaldoResponse[]>(`${this.API}/programaciones`);
  }

  crearProgramacion(request: CrearProgramacionRequest): Observable<ProgramacionRespaldoResponse> {
    return this.http.post<ProgramacionRespaldoResponse>(`${this.API}/programaciones`, request);
  }

  actualizarProgramacion(idProgramacion: number, request: ActualizarProgramacionRequest): Observable<ProgramacionRespaldoResponse> {
    return this.http.put<ProgramacionRespaldoResponse>(`${this.API}/programaciones/${idProgramacion}`, request);
  }

  cambiarEstadoProgramacion(idProgramacion: number, activo: boolean): Observable<ProgramacionRespaldoResponse> {
    return this.http.patch<ProgramacionRespaldoResponse>(`${this.API}/programaciones/${idProgramacion}/estado`, { activo });
  }

  eliminarProgramacion(idProgramacion: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/programaciones/${idProgramacion}`);
  }
}
