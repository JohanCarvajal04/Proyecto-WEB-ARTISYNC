import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageResponse } from '../../../shared/models/common.model';
import {
  PeticionActualizarPlantillaContrato,
  PeticionCrearPlantillaContrato,
  RespuestaPlantillaContrato,
  RespuestaPlantillaContratoResumen
} from '../models/legal.model';

/**
 * Catálogo de plantillas de contrato (REQ-F-017 ampliado).
 *  - Lectura pública (autenticada): las plantillas activas, para el selector
 *    del creador al crear/editar un servicio.
 *  - Administración: CRUD completo, solo con CONTRATO_PLANTILLA_GESTIONAR.
 */
@Injectable({ providedIn: 'root' })
export class PlantillaContratoService {

  private http = inject(HttpClient);
  private readonly API_PUBLICO = `${environment.apiUrl}/v1/plantillas-contrato`;
  private readonly API_ADMIN = `${environment.apiUrl}/v1/admin/plantillas-contrato`;

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
}
