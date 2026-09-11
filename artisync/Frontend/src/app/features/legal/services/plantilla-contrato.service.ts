import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import {
  PeticionActualizarPlantillaAcuerdoPropia,
  PeticionActualizarPlantillaContrato,
  PeticionCrearPlantillaAcuerdoPropia,
  PeticionCrearPlantillaContrato,
  RespuestaPlantillaContrato,
  RespuestaPlantillaContratoResumen
} from '../models/legal.model';

/**
 * Catálogo de plantillas de contrato (REQ-F-017 ampliado, V45).
 *  - Lectura pública (autenticada): las plantillas activas visibles para el
 *    selector del creador al crear/editar un servicio (catálogo general +
 *    sus propias plantillas de acuerdo).
 *  - Administración: CRUD completo del catálogo general, solo con CONTRATO_PLANTILLA_GESTIONAR.
 *  - Autoservicio del creador (V45): CRUD de sus propias plantillas de
 *    acuerdo, solo con CONTRATO_PLANTILLA_PROPIA_GESTIONAR.
 */
@Injectable({ providedIn: 'root' })
export class PlantillaContratoService {

  private http = inject(HttpClient);
  private readonly API_PUBLICO = `${environment.apiUrl}/v1/plantillas-contrato`;
  private readonly API_ADMIN = `${environment.apiUrl}/v1/admin/plantillas-contrato`;
  private readonly API_CREADOR = `${environment.apiUrl}/v1/creador/plantillas-acuerdo`;

  listarActivas(): Observable<RespuestaPlantillaContratoResumen[]> {
    return this.http.get<RespuestaPlantillaContratoResumen[]>(`${this.API_PUBLICO}/activas`);
  }

  listarTodas(): Observable<RespuestaPlantillaContrato[]> {
    return this.http.get<RespuestaPlantillaContrato[]>(this.API_ADMIN);
  }

  crear(peticion: PeticionCrearPlantillaContrato): Observable<RespuestaPlantillaContrato> {
    return this.http.post<RespuestaPlantillaContrato>(this.API_ADMIN, peticion);
  }

  editar(idPlantilla: number, peticion: PeticionActualizarPlantillaContrato): Observable<RespuestaPlantillaContrato> {
    return this.http.put<RespuestaPlantillaContrato>(`${this.API_ADMIN}/${idPlantilla}`, peticion);
  }

  desactivar(idPlantilla: number): Observable<MessageResponse> {
    return this.http.patch<MessageResponse>(`${this.API_ADMIN}/${idPlantilla}/desactivar`, {});
  }

  // ── Autoservicio del creador (V45) ──

  listarPropias(): Observable<RespuestaPlantillaContrato[]> {
    return this.http.get<RespuestaPlantillaContrato[]>(this.API_CREADOR);
  }

  crearPropia(peticion: PeticionCrearPlantillaAcuerdoPropia): Observable<RespuestaPlantillaContrato> {
    return this.http.post<RespuestaPlantillaContrato>(this.API_CREADOR, peticion);
  }

  editarPropia(idPlantilla: number, peticion: PeticionActualizarPlantillaAcuerdoPropia): Observable<RespuestaPlantillaContrato> {
    return this.http.put<RespuestaPlantillaContrato>(`${this.API_CREADOR}/${idPlantilla}`, peticion);
  }

  desactivarPropia(idPlantilla: number): Observable<MessageResponse> {
    return this.http.patch<MessageResponse>(`${this.API_CREADOR}/${idPlantilla}/desactivar`, {});
  }
}
