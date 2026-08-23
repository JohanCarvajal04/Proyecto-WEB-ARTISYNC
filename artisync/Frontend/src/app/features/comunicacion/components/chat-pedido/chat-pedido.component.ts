import { Component, Input, OnDestroy, OnInit, inject, signal, computed, effect } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription, catchError, of } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { RespuestaMensajeChat, RespuestaSalaChat } from '../../models/comunicacion.model';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ContratoService } from '../../../legal/services/contrato.service';
import { RespuestaContrato } from '../../../legal/models/legal.model';

@Component({
  selector: 'app-chat-pedido',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './chat-pedido.component.html'
})
export class ChatPedidoComponent implements OnInit, OnDestroy {

  @Input({ required: true }) idPedido!: number;

  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private contratoService = inject(ContratoService);

  readonly mensajes = signal<RespuestaMensajeChat[]>([]);
  readonly sala = signal<RespuestaSalaChat | null>(null);

  readonly isLoading = signal<boolean>(true);
  readonly enviando = signal<boolean>(false);
  readonly error = signal<string>('');
  readonly borrador = signal<string>('');

  /**
   * Ventana flotante: arranca minimizada para no taparle el resto del
   * detalle del pedido al abrir la página; el badge de no leídos es lo que
   * avisa que conviene expandirla.
   */
  readonly panelAbierto = signal<boolean>(false);
  readonly noLeidos = signal<number>(0);

  // Atajo para generar el contrato sin salir del chat, una vez que cliente y
  // creador llegaron a un acuerdo negociando por acá.
  readonly contrato = signal<RespuestaContrato | null>(null);
  readonly generandoContrato = signal<boolean>(false);
  readonly errorContrato = signal<string>('');

  readonly maxCaracteres = 500; // MAX_CARACTERES_MENSAJE

  private mensajesSub?: Subscription;
  /** Evita contar como "no leído" el historial que se carga al entrar. */
  private primerLote = true;

  readonly salaDisponible = computed(() => this.sala()?.salaActiva === true);

  readonly puedeEnviar = computed(() =>
    this.salaDisponible() && this.borrador().trim().length > 0 && !this.enviando());

  ngOnInit(): void {
    // 404 = todavía no se generó el contrato: es el estado normal mientras
    // negocian por chat, no un error que mostrarle al usuario.
    this.contratoService.obtenerContratoPorPedido(this.idPedido)
      .pipe(catchError(() => of(null)))
      .subscribe(contrato => this.contrato.set(contrato));

    this.chatService.obtenerEstadoSala(this.idPedido).subscribe({
      next: (sala) => {
        this.sala.set(sala);
        if (sala) {
          // Solo se conecta el WebSocket cuando de verdad hay una sala:
          // antes se activaba en el constructor para todo pedido, incluso sin
          // contrato firmado, así que cada vista del pedido abría un socket
          // sin nada a lo que unirse.
          this.chatService.connect();
          // Unirse a la sala para activar la suscripción STOMP
          this.chatService.joinSala(sala.idSala, this.idPedido);

          this.mensajesSub = this.chatService.mensajes$.subscribe(mensajes => {
            // El backend pagina el historial pero nosotros ahora usamos un BehaviorSubject
            // Ordenamos ascendente si vienen de WS
            const ordenados = mensajes.sort((a, b) => new Date(a.fechaHoraEnvio).getTime() - new Date(b.fechaHoraEnvio).getTime());
            const cantidadAnterior = this.mensajes().length;
            this.mensajes.set(ordenados);
            this.isLoading.set(false);

            if (!this.primerLote && !this.panelAbierto() && ordenados.length > cantidadAnterior) {
              const nuevosDeOtros = ordenados.slice(cantidadAnterior).filter(m => !this.esMio(m));
              if (nuevosDeOtros.length > 0) this.noLeidos.update(n => n + nuevosDeOtros.length);
            }
            this.primerLote = false;
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

  togglePanel(): void {
    this.panelAbierto.update(v => !v);
    if (this.panelAbierto()) this.noLeidos.set(0);
  }

  generarContrato(): void {
    if (this.generandoContrato() || this.contrato()) return;

    this.generandoContrato.set(true);
    this.errorContrato.set('');

    this.contratoService.generarContrato(this.idPedido).subscribe({
      next: (contrato) => {
        this.contrato.set(contrato);
        this.generandoContrato.set(false);
      },
      error: (err) => {
        this.errorContrato.set(err.error?.message || 'No se pudo generar el contrato');
        this.generandoContrato.set(false);
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
