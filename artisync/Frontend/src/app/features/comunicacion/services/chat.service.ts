import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable } from 'rxjs';
import { RespuestaMensajeChat, PeticionEnviarMensaje, RespuestaSalaChat } from '../models/comunicacion.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = '/api/v1/pedidos';
  private stompClient: Client;
  private currentSubscription?: StompSubscription;
  
  private mensajesSubject = new BehaviorSubject<RespuestaMensajeChat[]>([]);
  public mensajes$ = this.mensajesSubject.asObservable();
  
  private connectionStateSubject = new BehaviorSubject<boolean>(false);
  public isConnected$ = this.connectionStateSubject.asObservable();

  constructor(private http: HttpClient) {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        // console.log(str);
      }
    });

    this.stompClient.onConnect = (frame) => {
      this.connectionStateSubject.next(true);
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };
    
    this.stompClient.onWebSocketClose = () => {
      this.connectionStateSubject.next(false);
    };
  }

  public connect(): void {
    if (!this.stompClient.active) {
      // Obtenemos el token JWT del local storage para enviarlo
      const token = localStorage.getItem('token');
      if (token) {
        this.stompClient.connectHeaders = {
          Authorization: `Bearer ${token}`
        };
      }
      this.stompClient.activate();
    }
  }

  public disconnect(): void {
    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = undefined;
    }
    if (this.stompClient.active) {
      this.stompClient.deactivate();
    }
  }

  public joinSala(idSala: number, idPedido: number): void {
    if (!this.stompClient.active) {
      console.warn('STOMP no está activo, conectando primero...');
      this.connect();
    }
    
    // Si ya estábamos suscritos a otra sala, nos desuscribimos
    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
    }
    
    // Cargar historial por REST
    this.cargarHistorialMensajes(idPedido);
    
    // Suscribirse a la sala de STOMP
    const topic = `/topic/sala.${idSala}`;
    this.currentSubscription = this.stompClient.subscribe(topic, (message: IMessage) => {
      if (message.body) {
        try {
          const body = JSON.parse(message.body);
          if (body.tipo === 'SALA_CERRADA') {
            // Manejar sala cerrada
            console.log('La sala fue cerrada');
          } else {
            // Es un Mensaje nuevo
            const nuevoMensaje = body as RespuestaMensajeChat;
            const actuales = this.mensajesSubject.value;
            // Evitar duplicados por si acaso el REST y el WS traen el mismo
            if (!actuales.find(m => m.idMensaje === nuevoMensaje.idMensaje)) {
              this.mensajesSubject.next([...actuales, nuevoMensaje]);
            }
          }
        } catch (e) {
          console.error('Error parseando mensaje WS:', e);
        }
      }
    });
  }

  public enviarMensaje(idPedido: number, cuerpo: string): Observable<RespuestaMensajeChat> {
    const peticion: PeticionEnviarMensaje = { cuerpoMensaje: cuerpo };
    return this.http.post<RespuestaMensajeChat>(`${this.apiUrl}/${idPedido}/chat/mensajes`, peticion);
  }

  public enviarMensajeWs(idPedido: number, cuerpo: string): void {
    if (this.stompClient.active) {
      const peticion: PeticionEnviarMensaje = { cuerpoMensaje: cuerpo };
      this.stompClient.publish({
        destination: '/app/chat.enviar',
        body: JSON.stringify(peticion)
      });
    } else {
      console.error('No se puede enviar mensaje por WS: cliente inactivo');
    }
  }

  private cargarHistorialMensajes(idPedido: number): void {
    this.http.get<any>(`${this.apiUrl}/${idPedido}/chat/mensajes?size=100`).subscribe({
      next: (res) => {
        this.mensajesSubject.next(res.content || []);
      },
      error: (err) => {
        console.error('Error cargando historial de mensajes:', err);
      }
    });
  }

  public obtenerEstadoSala(idPedido: number): Observable<RespuestaSalaChat> {
    return this.http.get<RespuestaSalaChat>(`${this.apiUrl}/${idPedido}/chat/estado`);
  }
}
