import { Component, Input, OnDestroy, OnInit, inject, signal, computed, effect } from '@angular/core';
import { Subscription } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { RespuestaMensajeChat, RespuestaSalaChat } from '../../models/comunicacion.model';
import { AuthService } from '../../../seguridad/services/auth.service';

@Component({
  selector: 'app-chat-pedido',
  standalone: true,
  imports: [],
  templateUrl: './chat-pedido.component.html'
})
export class ChatPedidoComponent implements OnInit, OnDestroy {

  @Input({ required: true }) idPedido!: number;

  private chatService = inject(ChatService);
  private authService = inject(AuthService);

  readonly mensajes = signal<RespuestaMensajeChat[]>([]);
  readonly sala = signal<RespuestaSalaChat | null>(null);

  readonly isLoading = signal<boolean>(true);
  readonly enviando = signal<boolean>(false);
  readonly error = signal<string>('');
  readonly borrador = signal<string>('');

  readonly maxCaracteres = 500; // MAX_CARACTERES_MENSAJE

  private mensajesSub?: Subscription;

  readonly salaDisponible = computed(() => this.sala()?.salaActiva === true);

  readonly puedeEnviar = computed(() =>
    this.salaDisponible() && this.borrador().trim().length > 0 && !this.enviando());

  constructor() {
    this.chatService.connect();
  }

  ngOnInit(): void {
    this.chatService.obtenerEstadoSala(this.idPedido).subscribe({
      next: (sala) => {
        this.sala.set(sala);
        if (sala) {
          // Unirse a la sala para activar la suscripción STOMP
          this.chatService.joinSala(sala.idSala, this.idPedido);
          
          this.mensajesSub = this.chatService.mensajes$.subscribe(mensajes => {
            // El backend pagina el historial pero nosotros ahora usamos un BehaviorSubject
            // Ordenamos ascendente si vienen de WS
            this.mensajes.set(mensajes.sort((a, b) => new Date(a.fechaHoraEnvio).getTime() - new Date(b.fechaHoraEnvio).getTime()));
            this.isLoading.set(false);
          });
        } else {
          this.isLoading.set(false);
        }
      },
      error: () => {
        this.sala.set(null);
        this.isLoading.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.mensajesSub?.unsubscribe();
    this.chatService.disconnect();
  }

  onBorrador(evento: Event): void {
    this.borrador.set((evento.target as HTMLTextAreaElement).value);
  }

  enviar(): void {
    const texto = this.borrador().trim();
    if (!texto || this.enviando()) return;

    this.enviando.set(true);
    this.error.set('');

    this.chatService.enviarMensaje(this.idPedido, texto).subscribe({
      next: (mensaje) => {
        // No es necesario agregarlo manualmente porque llegará por el WebSocket
        this.borrador.set('');
        this.enviando.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo enviar el mensaje');
        this.enviando.set(false);
      }
    });
  }

  esMio(mensaje: RespuestaMensajeChat): boolean {
    const idActual = this.authService.getCurrentUserId();
    return idActual !== null && mensaje.idRemitente === idActual;
  }

  formatHora(fecha: string): string {
    if (!fecha) return '';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  }
}
