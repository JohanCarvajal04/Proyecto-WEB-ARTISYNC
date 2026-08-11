import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  VerificacionCola, VerificacionDetalle, DecisionVerificacion,
  CertificadoIa, Portafolio, Categoria, CrearCategoria, ActualizarCategoria
} from '../models/moderacion.model';
import { MessageResponse } from '../../../shared/models/common.model';

@Injectable({
  providedIn: 'root'
})
export class ModeracionService {
  private http = inject(HttpClient);

  // ─── Verificaciones ───

  listarColaVerificaciones(estado?: string, limite = 20, offset = 0): Observable<VerificacionCola[]> {
    let params = new HttpParams()
      .set('limite', limite.toString())
      .set('offset', offset.toString());
    if (estado) params = params.set('estado', estado);
    return this.http.get<VerificacionCola[]>(`${environment.apiUrl}/v1/verificaciones`, { params });
  }

  obtenerVerificacion(id: number): Observable<VerificacionDetalle> {
    return this.http.get<VerificacionDetalle>(`${environment.apiUrl}/v1/verificaciones/${id}`);
  }

  analizarConIa(id: number): Observable<VerificacionDetalle> {
    return this.http.post<VerificacionDetalle>(`${environment.apiUrl}/v1/verificaciones/${id}/analisis-ia`, {});
  }

  registrarDecision(id: number, decision: DecisionVerificacion): Observable<VerificacionDetalle> {
    return this.http.patch<VerificacionDetalle>(`${environment.apiUrl}/v1/verificaciones/${id}/decision`, decision);
  }

  obtenerDocumento(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/v1/verificaciones/${id}/documento`, { responseType: 'blob' });
  }

  // ─── Certificados IA ───

  listarCertificados(): Observable<CertificadoIa[]> {
    return this.http.get<CertificadoIa[]>(`${environment.apiUrl}/v1/certificados`);
  }

  obtenerCertificado(id: number): Observable<CertificadoIa> {
    return this.http.get<CertificadoIa>(`${environment.apiUrl}/v1/certificados/${id}`);
  }

  // ─── Portafolios (solo lectura) ───

  listarPortafolios(): Observable<Portafolio[]> {
    return this.http.get<Portafolio[]>(`${environment.apiUrl}/v1/portafolios`);
  }

  obtenerPortafolio(id: number): Observable<Portafolio> {
    return this.http.get<Portafolio>(`${environment.apiUrl}/v1/portafolios/${id}`);
  }

  // ─── Categorías ───

  listarCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${environment.apiUrl}/v1/categorias`);
  }

  crearCategoria(data: CrearCategoria): Observable<Categoria> {
    return this.http.post<Categoria>(`${environment.apiUrl}/v1/categorias`, data);
  }

  actualizarCategoria(id: number, data: ActualizarCategoria): Observable<Categoria> {
    return this.http.put<Categoria>(`${environment.apiUrl}/v1/categorias/${id}`, data);
  }

  eliminarCategoria(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${environment.apiUrl}/v1/categorias/${id}`);
  }
}
