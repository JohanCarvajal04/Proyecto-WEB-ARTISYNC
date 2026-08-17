import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { UserResponse, PaisResponse } from '../../../shared/models/user.model';
import {
  CreateUserRequest, AdminUpdateUserRequest,
  AssignRolesRequest, ChangeEstadoRequest
} from '../models/admin.model';
import { PagedResponse, MessageResponse } from '../../../shared/models/common.model';

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/admin/usuarios`;

  getUsers(page = 0, size = 10, sortBy = 'idUsuario', direction = 'asc'): Observable<PagedResponse<UserResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);

    return this.http.get<PagedResponse<UserResponse>>(this.apiUrl, { params });
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  createUser(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, request);
  }

  updateUser(id: number, request: AdminUpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, request);
  }

  changeEstado(id: number, request: ChangeEstadoRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${id}/estado`, request);
  }

  assignRoles(id: number, request: AssignRolesRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}/roles`, request);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Cierra todas las sesiones activas de otro usuario (SESION_REVOCAR). No
   * confundir con `/usuarios/me/sesiones`, que solo afecta a la propia cuenta.
   */
  revokeSessions(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.apiUrl}/${id}/sesiones`);
  }

  getPaises(): Observable<PaisResponse[]> {
    return this.http.get<PaisResponse[]>(`${environment.apiUrl}/paises`);
  }
}
