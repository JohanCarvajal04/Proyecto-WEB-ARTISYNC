import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaisResponse } from '../models/user.model';
import { PaisRequest } from '../../features/administracion/models/admin.model';
import { MessageResponse } from '../models/common.model';

@Injectable({
  providedIn: 'root'
})
export class PaisService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/v1/paises`;

  getPaises(): Observable<PaisResponse[]> {
    return this.http.get<PaisResponse[]>(this.apiUrl);
  }

  getPaisesActivos(): Observable<PaisResponse[]> {
    return this.http.get<PaisResponse[]>(`${this.apiUrl}/activos`);
  }

  getPaisById(id: number): Observable<PaisResponse> {
    return this.http.get<PaisResponse>(`${this.apiUrl}/${id}`);
  }

  createPais(request: PaisRequest): Observable<PaisResponse> {
    return this.http.post<PaisResponse>(this.apiUrl, request);
  }

  updatePais(id: number, request: PaisRequest): Observable<PaisResponse> {
    return this.http.put<PaisResponse>(`${this.apiUrl}/${id}`, request);
  }

  deletePais(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.apiUrl}/${id}`);
  }
}
