import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notificacion } from '../models/comunicacion.model';

@Injectable({
  providedIn: 'root'
})
export class NotificacionService {
  private apiUrl = '/api/v1/notificaciones';

  constructor(private http: HttpClient) {}

  public obtenerNotificaciones(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  public marcarComoLeida(idNotificacion: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${idNotificacion}/leida`, {});
  }
}
