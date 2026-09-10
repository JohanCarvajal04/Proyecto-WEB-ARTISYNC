import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pagina, normalizarPagina } from '../../../shared/models/pagina.model';
import { map } from 'rxjs';
import { Respaldo, ResumenRespaldos, RespaldoPolitica, PeticionActualizarPolitica } from '../models/respaldo.model';

@Injectable({
  providedIn: 'root'
})
export class RespaldoService {
  private http = inject(HttpClient);
  private base = '/api/v1/admin/respaldos';

  listar(page: number = 0, tipo?: string, categoria?: string): Observable<Pagina<Respaldo>> {
    let params = `?page=${page}`;
    if (tipo && tipo !== 'TODOS') {
      params += `&tipo=${tipo}`;
    }
    if (categoria && categoria !== 'TODAS') {
      params += `&categoria=${categoria}`;
    }
    return this.http.get<unknown>(`${this.base}${params}`).pipe(
      map(res => normalizarPagina<Respaldo>(res))
    );
  }

  obtenerResumen(): Observable<ResumenRespaldos> {
    return this.http.get<ResumenRespaldos>(`${this.base}/resumen`);
  }

  obtenerPorId(id: number): Observable<Respaldo> {
    return this.http.get<Respaldo>(`${this.base}/${id}`);
  }

  crearManual(categoria: 'FULL' | 'DIARIO' = 'FULL'): Observable<Respaldo> {
    return this.http.post<Respaldo>(`${this.base}?categoria=${categoria}`, {});
  }

  importarExterno(archivo: File): Observable<Respaldo> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<Respaldo>(`${this.base}/importar`, formData);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  restaurar(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/restaurar`, {});
  }

  descargar(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.base}/${id}/descargar`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  obtenerPolitica(): Observable<RespaldoPolitica> {
    return this.http.get<RespaldoPolitica>(`${this.base}/politica`);
  }

  actualizarPolitica(peticion: PeticionActualizarPolitica): Observable<RespaldoPolitica> {
    return this.http.put<RespaldoPolitica>(`${this.base}/politica`, peticion);
  }
}
