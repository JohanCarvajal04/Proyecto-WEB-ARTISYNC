import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable } from 'rxjs';
import { RespuestaMensajeChat, PeticionEnviarMensaje, RespuestaSalaChat } from '../models/comunicacion.model';
import { AuthService } from '../../seguridad/services/auth.service';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = `${environment.apiUrl}/v1/pedidos`;
  private authService = inject(AuthService);
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
      // El access token vive en memoria (AuthService), no en localStorage: ahí
      // nunca se guarda, así que este header salía siempre vacío y
      // WebSocketAuthInterceptor aceptaba la conexión sin autenticar a nadie.
      const token = this.authService.accessToken();
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
    // NG-01 (auditoria Angular): este servicio es providedIn:'root' -- sobrevive
    // al componente de chat -- y mensajesSubject es un BehaviorSubject: sin este
    // reset, el siguiente componente que se suscriba a mensajes$ (al abrir el
    // chat de OTRO pedido) recibe de inmediato la conversacion que quedo aqui,
    // antes de que llegue la respuesta de cargarHistorialMensajes. Son
    // contrapartes distintas, asi que era una fuga de confidencialidad entre
    // conversaciones, no solo un parpadeo visual.
    this.mensajesSubject.next([]);
  }

  public joinSala(idSala: number, idPedido: number): void {
    // Vaciar antes de pedir el historial nuevo: cubre entrar a otra sala sin
    // que el componente anterior llegara a destruirse (mismo motivo que en
    // disconnect() de arriba).
    this.mensajesSubject.next([]);
    // Cargar historial por REST no depende del WebSocket: se dispara ya.
    this.cargarHistorialMensajes(idPedido);

    const suscribirse = () => {
      // Si ya estábamos suscritos a otra sala, nos desuscribimos
      if (this.currentSubscription) {
        this.currentSubscription.unsubscribe();
      }

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
    };

    // stompClient.subscribe() exige una conexión ya establecida (CONNECTED),
    // no solo "activada": activate() dispara el handshake de forma asíncrona,
    // así que suscribirse en el mismo tick lanzaba
    // "There is no underlying STOMP connection". Se espera la conexión real.
    if (this.stompClient.connected) {
      suscribirse();
    } else {
      const sub = this.isConnected$.subscribe(conectado => {
        if (conectado) {
          suscribirse();
          sub.unsubscribe();
        }
      });
      this.connect();
    }
  }

  public enviarMensaje(idPedido: number, cuerpo: string): Observable<RespuestaMensajeChat> {
    const peticion: PeticionEnviarMensaje = { cuerpoMensaje: cuerpo };
    return this.http.post<RespuestaMensajeChat>(`${this.apiUrl}/${idPedido}/chat/mensajes`, peticion);
  }

  public enviarMensajeWs(idPedido: number, cuerpo: string): void {
    if (this.stompClient.active) {
      const peticion: PeticionEnviarMensaje = { idPedido, cuerpoMensaje: cuerpo };
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
        // NG-01: sin esto, un fallo de red dejaba en pantalla el historial del
        // pedido/sala anterior en vez de un estado vacio/de error.
        this.mensajesSubject.next([]);
      }
    });
  }

  public obtenerEstadoSala(idPedido: number): Observable<RespuestaSalaChat> {
    return this.http.get<RespuestaSalaChat>(`${this.apiUrl}/${idPedido}/chat/estado`);
  }
}
