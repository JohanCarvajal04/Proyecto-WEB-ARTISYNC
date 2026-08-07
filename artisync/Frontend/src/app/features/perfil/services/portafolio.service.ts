import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Portafolio, PeticionActualizarPortafolio, PeticionCrearPortafolio } from '../models/portafolio.model';

@Injectable({
  providedIn: 'root'
})
export class PortafolioService {
  private apiUrl = `${environment.apiUrl}/portafolios`;

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
}
