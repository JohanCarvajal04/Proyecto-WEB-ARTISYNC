import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface RespuestaComentario {
  idComentario: number;
  idItemPortafolio: number;
  idUsuarioAutor: number;
  nombreAutor: string;
  textoComentario: string;
  estadoModeracion: string;
  fechaPublicacion: string;
}

export interface PeticionCrearComentario {
  textoComentario: string;
}

export interface RespuestaMensaje {
  mensaje: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class ComentarioService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/api/v1`;

  listarComentarios(idItem: number, page: number = 0, size: number = 10): Observable<Page<RespuestaComentario>> {
    return this.http.get<Page<RespuestaComentario>>(`${this.url}/portafolio/items/${idItem}/comentarios?page=${page}&size=${size}`);
  }

  agregarComentario(idItem: number, peticion: PeticionCrearComentario): Observable<RespuestaComentario> {
    return this.http.post<RespuestaComentario>(`${this.url}/portafolio/items/${idItem}/comentarios`, peticion);
  }

  eliminarComentario(idComentario: number): Observable<RespuestaMensaje> {
    return this.http.delete<RespuestaMensaje>(`${this.url}/comentarios/${idComentario}`);
  }
}
