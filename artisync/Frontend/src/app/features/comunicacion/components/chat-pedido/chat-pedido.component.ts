import { Component, Input, OnDestroy, OnInit, inject, signal, computed } from '@angular/core';
import { Subscription, interval, of, startWith, switchMap, catchError } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { MAX_CARACTERES_MENSAJE, RespuestaMensajeChat, RespuestaSalaChat } from '../../models/comunicacion.model';
import { AuthService } from '../../../seguridad/services/auth.service';

/** Sondeo del historial mientras no se consuma el canal STOMP. */
const INTERVALO_SONDEO_MS = 8_000;

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

  readonly maxCaracteres = MAX_CARACTERES_MENSAJE;

  private sondeoSub?: Subscription;

  /**
   * El chat solo existe tras la firma de ambas partes y se cierra al aprobar
   * la entrega; sin sala, no hay nada que mostrar ni dónde escribir.
   */
  readonly salaDisponible = computed(() => this.sala()?.salaActiva === true);

  readonly puedeEnviar = computed(() =>
    this.salaDisponible() && this.borrador().trim().length > 0 && !this.enviando());

  ngOnInit(): void {
    this.chatService.obtenerEstadoSala(this.idPedido).subscribe({
      next: (sala) => this.sala.set(sala),
      // Un 404 aquí significa "todavía no hay sala", no un fallo de la vista.
      error: () => this.sala.set(null)
    });

    this.sondeoSub = interval(INTERVALO_SONDEO_MS).pipe(
      startWith(0),
      switchMap(() => this.chatService.obtenerMensajes(this.idPedido).pipe(catchError(() => of(null))))
    ).subscribe(pagina => {
      if (pagina) {
        // El backend pagina el historial en orden descendente; la conversación
        // se lee de más antiguo a más reciente.
        this.mensajes.set([...pagina.contenido].reverse());
      }
      this.isLoading.set(false);
    });
  }

  ngOnDestroy(): void {
    this.sondeoSub?.unsubscribe();
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
        this.mensajes.update(ms => [...ms, mensaje]);
        this.borrador.set('');
        this.enviando.set(false);
      },
      error: (err) => {
        // RF-15: el backend puede rechazar el mensaje por filtrar datos de contacto.
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
