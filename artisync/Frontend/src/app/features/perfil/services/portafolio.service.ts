import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Portafolio,
  PeticionActualizarPortafolio,
  PeticionCrearPortafolio,
  PortafolioItem,
  PeticionCrearPortafolioItem
} from '../models/portafolio.model';

@Injectable({
  providedIn: 'root'
})
export class PortafolioService {
  // El controlador está mapeado en /api/v1/portafolios; sin el segmento `v1`
  // todas las llamadas respondían 404.
  private apiUrl = `${environment.apiUrl}/v1/portafolios`;

  constructor(private http: HttpClient) {}

  obtenerPorId(id: number): Observable<Portafolio> {
    return this.http.get<Portafolio>(`${this.apiUrl}/${id}`);
  }

  obtenerPorPerfil(idPerfil: number): Observable<Portafolio> {
    return this.http.get<Portafolio>(`${this.apiUrl}/perfil/${idPerfil}`);
  }

  crear(peticion: PeticionCrearPortafolio): Observable<Portafolio> {
    return this.http.post<Portafolio>(this.apiUrl, peticion);
  }

  actualizar(id: number, peticion: PeticionActualizarPortafolio): Observable<Portafolio> {
    return this.http.put<Portafolio>(`${this.apiUrl}/${id}`, peticion);
  }

  // ── Obras del portafolio ───────────────────────────────────────────────────

  listarItems(idPortafolio: number): Observable<PortafolioItem[]> {
    return this.http.get<PortafolioItem[]>(`${this.apiUrl}/${idPortafolio}/items`);
  }

  /**
   * El backend recibe la obra como multipart con dos partes: `datos` con los
   * metadatos y `archivo` con el binario.
   *
   * Los metadatos van envueltos en un Blob de tipo application/json a
   * propósito: `@RequestPart` necesita que la parte declare ese content-type
   * para deserializarla. Un `append('datos', JSON.stringify(...))` la enviaría
   * como text/plain y el backend respondería 415.
   */
  subirItem(idPortafolio: number, datos: PeticionCrearPortafolioItem, archivo: File): Observable<PortafolioItem> {
    const formData = new FormData();
    formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
    formData.append('archivo', archivo);

    // Sin Content-Type explícito: el navegador debe fijarlo junto al boundary.
    return this.http.post<PortafolioItem>(`${this.apiUrl}/${idPortafolio}/items`, formData);
  }

  eliminarItem(idItem: number): Observable<{ mensaje: string }> {
    return this.http.delete<{ mensaje: string }>(`${this.apiUrl}/items/${idItem}`);
  }
}
