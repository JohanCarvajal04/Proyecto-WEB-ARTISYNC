import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { UserResponse } from '../../../shared/models/user.model';
import { UpdateUserRequest, ChangePasswordRequest } from '../models/perfil.model';
import { MessageResponse } from '../../../shared/models/common.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/v1/usuarios`;

  getCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`);
  }

  updateCurrentUser(request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/me`, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.apiUrl}/me/password`, request);
  }

  deleteOwnAccount(): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.apiUrl}/me`);
  }

  revokeAllMySessions(): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.apiUrl}/me/sesiones`);
  }

  /**
   * REQ-NF-018: solicita la supresión real (anonimización) de los datos
   * personales del usuario autenticado — distinta de deleteOwnAccount(), que
   * solo desactiva la cuenta sin tocar los datos. Es irreversible: el correo
   * queda libre para un registro nuevo y no hay forma de deshacerlo.
   *
   * @param codigo código TOTP o de respaldo — requerido solo si el usuario tiene 2FA activo (mismo shape que disable2fa)
   */
  solicitarSupresionDatos(codigo?: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/me/solicitud-supresion`, { codigo });
  }

  uploadProfilePicture(file: File): Observable<UserResponse> {
    const formData = new FormData();
    formData.append('foto', file);
    return this.http.post<UserResponse>(`${this.apiUrl}/me/foto`, formData);
  }
}
