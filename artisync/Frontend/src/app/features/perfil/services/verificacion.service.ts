import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type TipoDocumentoVerificacion = 'IDENTIDAD' | 'CERTIFICADO';

/** Espejo de RespuestaVerificacion (record de Java). */
export interface RespuestaVerificacion {
  idCertificado: number;
  idPerfil: number | null;
  tipoDocumento: string;
  nombreEstadoVerificacion: string;
  veredictoIa: string | null;
  puntajeConfianzaIa: number | null;
  razonIa: string | null;
  datosExtraidosIa: string | null;
  fechaDictamenIa: string | null;
  idModerador: number | null;
  fechaDecision: string | null;
  notaModerador: string | null;
  fechaAnalisis: string | null;
}

/**
 * Solicitud de verificación por parte del propio usuario (REQ-F-006/007).
 *
 * Nota: el backend no expone un endpoint tipo «mis verificaciones»; solo
 * `GET /{id}`. Por eso la vista únicamente puede mostrar el estado de la
 * solicitud recién enviada, no un histórico.
 */
@Injectable({ providedIn: 'root' })
export class VerificacionService {

  private http = inject(HttpClient);
  private readonly API = `${environment.apiUrl}/v1/verificaciones`;

  solicitar(tipo: TipoDocumentoVerificacion, documento: File): Observable<RespuestaVerificacion> {
    const formData = new FormData();
    formData.append('tipo', tipo);
    formData.append('documento', documento);

    // Sin Content-Type explícito: el navegador debe fijarlo junto al boundary.
    return this.http.post<RespuestaVerificacion>(this.API, formData);
  }

  obtenerPorId(id: number): Observable<RespuestaVerificacion> {
    return this.http.get<RespuestaVerificacion>(`${this.API}/${id}`);
  }
}
