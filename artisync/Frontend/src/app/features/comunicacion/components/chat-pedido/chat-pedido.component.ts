import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap, interval, EMPTY } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { RespuestaMensajeChat, RespuestaSalaChat } from '../../models/comunicacion.model';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ContratoService } from '../../../legal/services/contrato.service';
import { RespuestaContrato } from '../../../legal/models/legal.model';
import { PedidoService } from '../../../pedido/services/pedido.service';
import { RespuestaPedido, RespuestaPropuestaTerminos } from '../../../pedido/models/pedido.model';
import { MonedaPipe } from '../../../../shared/pipes/moneda.pipe';

@Component({
  selector: 'app-chat-pedido',
  standalone: true,
  imports: [RouterLink, MonedaPipe],
  templateUrl: './chat-pedido.component.html'
})
export class ChatPedidoComponent implements OnInit, OnDestroy {

  @Input({ required: true }) idPedido!: number;

  /** Avisa al detalle del pedido que precio/fecha cambiaron al aceptar una propuesta. */
  @Output() pedidoActualizado = new EventEmitter<RespuestaPedido>();

  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private contratoService = inject(ContratoService);
  private pedidoService = inject(PedidoService);
  private destroyRef = inject(DestroyRef);

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

  // Atajo para revisar/generar el contrato sin salir del chat.
  readonly contrato = signal<RespuestaContrato | null>(null);

  // Propone/acepta precio y fecha final: el cambio solo se aplica al pedido
  // (y genera el contrato si aún no existe) cuando la CONTRAPARTE del
  // proponente acepta (ver PedidoServicioImpl#aceptarPropuestaTerminos).
  readonly propuestaPendiente = signal<RespuestaPropuestaTerminos | null>(null);
  readonly mostrarFormPropuesta = signal<boolean>(false);
  readonly precioPropuesto = signal<number | null>(null);
  readonly fechaPropuesta = signal<string>('');
  readonly proponiendo = signal<boolean>(false);
  readonly resolviendoPropuesta = signal<boolean>(false);
  readonly errorPropuesta = signal<string>('');

  readonly maxCaracteres = 500; // MAX_CARACTERES_MENSAJE

  /** Evita contar como "no leído" el historial que se carga al entrar. */
  private primerLote = true;

  readonly salaDisponible = computed(() => this.sala()?.salaActiva === true);

  readonly puedeEnviar = computed(() =>
    this.salaDisponible() && this.borrador().trim().length > 0 && !this.enviando());

  /**
   * Los términos quedan congelados apenas hay una firma: el contrato ya
   * renderiza precio/fecha en vivo desde el pedido, así que cambiarlos
   * después de que alguien firmó reescribiría en silencio lo que esa
   * persona ya aceptó (misma regla que aplica el backend).
   */
  readonly contratoFirmadoAlguna = computed(() =>
    !!this.contrato()?.hashFirmaCreador || !!this.contrato()?.hashFirmaCliente);

  readonly esElProponente = computed(() =>
    this.propuestaPendiente()?.idUsuarioPropuso === this.authService.getCurrentUserId());

  /** `min` del input datetime-local: el backend igual la rechaza con @Future si se fuerza por fuera del UI. */
  get minFechaEntrega(): string {
    const ahora = new Date(Date.now() - new Date().getTimezoneOffset() * 60000);
    return ahora.toISOString().slice(0, 16);
  }

  ngOnInit(): void {
    // 404 = todavía no se generó el contrato: es el estado normal mientras
    // negocian por chat, no un error que mostrarle al usuario.
    this.contratoService.obtenerContratoPorPedido(this.idPedido)
      .pipe(catchError(() => of(null)), takeUntilDestroyed(this.destroyRef))
      .subscribe(contrato => this.contrato.set(contrato));

    this.pedidoService.obtenerPropuestaPendiente(this.idPedido)
      .pipe(catchError(() => of(null)), takeUntilDestroyed(this.destroyRef))
      .subscribe(propuesta => this.propuestaPendiente.set(propuesta));

    // Polling cada 5s (mismo mecanismo RF-19 que pedido-detalle): así la
    // contraparte ve una propuesta nueva, o su resolución, sin recargar.
    interval(5000).pipe(
      switchMap(() => this.contratoService.obtenerContratoPorPedido(this.idPedido).pipe(catchError(() => of(null)))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(contrato => this.contrato.set(contrato));

    interval(5000).pipe(
      switchMap(() => this.pedidoService.obtenerPropuestaPendiente(this.idPedido).pipe(catchError(() => of(null)))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(propuesta => this.propuestaPendiente.set(propuesta));

    // Aplanado con switchMap en vez de anidar el subscribe a mensajes$ dentro
    // del subscribe de obtenerEstadoSala: la suscripción anidada se creaba
    // dentro del callback de una petición HTTP, así que si el componente se
    // destruía antes de que esa petición respondiera, el unsubscribe() de
    // ngOnDestroy corría sobre un campo todavía `undefined` -- y la
    // suscripción a mensajes$ (un BehaviorSubject de un servicio root) nacía
    // después, huérfana, manteniendo vivo el componente destruido por el
    // closure y siguiendo recibiendo mensajes de conversaciones ajenas.
    // takeUntilDestroyed cierra toda la cadena de una vez, sin depender de un
    // campo asignado a tiempo.
    this.chatService.obtenerEstadoSala(this.idPedido)
      .pipe(
        switchMap(sala => {
          this.sala.set(sala);
          if (!sala) {
            this.isLoading.set(false);
            return EMPTY;
          }
          // Solo se conecta el WebSocket cuando de verdad hay una sala: antes
          // se activaba en el constructor para todo pedido, incluso sin
          // contrato firmado, así que cada vista del pedido abría un socket
          // sin nada a lo que unirse.
          this.chatService.connect();
          this.chatService.joinSala(sala.idSala, this.idPedido);
          return this.chatService.mensajes$;
        }),
        catchError(() => {
          this.sala.set(null);
          this.isLoading.set(false);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(mensajes => {
        // El backend pagina el historial pero nosotros ahora usamos un BehaviorSubject
        // Ordenamos ascendente si vienen de WS. [...mensajes] copia antes de
        // ordenar: sort() muta en sitio, y mensajes es el mismo array que
        // ChatService guarda en su BehaviorSubject (NG-04) -- mutarlo aqui
        // corrompe el estado bajo cualquier otro suscriptor futuro.
        const ordenados = [...mensajes].sort((a, b) => new Date(a.fechaHoraEnvio).getTime() - new Date(b.fechaHoraEnvio).getTime());
        const cantidadAnterior = this.mensajes().length;
        this.mensajes.set(ordenados);
        this.isLoading.set(false);

        if (!this.primerLote && !this.panelAbierto() && ordenados.length > cantidadAnterior) {
          const nuevosDeOtros = ordenados.slice(cantidadAnterior).filter(m => !this.esMio(m));
          if (nuevosDeOtros.length > 0) this.noLeidos.update(n => n + nuevosDeOtros.length);
        }
        this.primerLote = false;
      });
  }

  togglePanel(): void {
    this.panelAbierto.update(v => !v);
    if (this.panelAbierto()) this.noLeidos.set(0);
  }

  proponerTerminos(): void {
    if (this.proponiendo()) return;

    const peticion: { precioPropuesto?: number; fechaEntregaPropuesta?: string } = {};
    if (this.precioPropuesto() != null) peticion.precioPropuesto = this.precioPropuesto()!;
    if (this.fechaPropuesta()) peticion.fechaEntregaPropuesta = this.fechaPropuesta();

    if (peticion.precioPropuesto == null && !peticion.fechaEntregaPropuesta) {
      this.errorPropuesta.set('Indica un precio o una fecha de entrega para proponer.');
      return;
    }

    this.proponiendo.set(true);
    this.errorPropuesta.set('');

    this.pedidoService.proponerTerminos(this.idPedido, peticion).subscribe({
      next: (propuesta) => {
        this.propuestaPendiente.set(propuesta);
        this.mostrarFormPropuesta.set(false);
        this.precioPropuesto.set(null);
        this.fechaPropuesta.set('');
        this.proponiendo.set(false);
      },
      error: (err) => {
        // El backend responde ProblemDetail (RFC 7807): el mensaje va en
        // `detail`, y una falla de @Valid trae además `fieldErrors`.
        const erroresPorCampo = err.error?.fieldErrors;
        this.errorPropuesta.set((erroresPorCampo && Object.values(erroresPorCampo).join(', '))
          || err.error?.detail
          || 'No se pudo enviar la propuesta');
        this.proponiendo.set(false);
      }
    });
  }

  aceptarPropuesta(): void {
    const propuesta = this.propuestaPendiente();
    if (!propuesta || this.resolviendoPropuesta()) return;

    this.resolviendoPropuesta.set(true);
    this.errorPropuesta.set('');

    this.pedidoService.aceptarPropuestaTerminos(this.idPedido, propuesta.idPropuesta).subscribe({
      next: (pedido) => {
        this.propuestaPendiente.set(null);
        this.resolviendoPropuesta.set(false);
        this.pedidoActualizado.emit(pedido);
        // Refresca el atajo de contrato: si esta era la primera propuesta
        // aceptada, el contrato se acaba de generar en el mismo paso.
        this.contratoService.obtenerContratoPorPedido(this.idPedido)
          .pipe(catchError(() => of(null)))
          .subscribe(contrato => this.contrato.set(contrato));
      },
      error: (err) => {
        this.errorPropuesta.set(err.error?.detail || 'No se pudo aceptar la propuesta');
        this.resolviendoPropuesta.set(false);
      }
    });
  }

  rechazarPropuesta(): void {
    const propuesta = this.propuestaPendiente();
    if (!propuesta || this.resolviendoPropuesta()) return;

    this.resolviendoPropuesta.set(true);
    this.errorPropuesta.set('');

    this.pedidoService.rechazarPropuestaTerminos(this.idPedido, propuesta.idPropuesta).subscribe({
      next: () => {
        this.propuestaPendiente.set(null);
        this.resolviendoPropuesta.set(false);
      },
      error: (err) => {
        this.errorPropuesta.set(err.error?.detail || 'No se pudo rechazar la propuesta');
        this.resolviendoPropuesta.set(false);
      }
    });
  }

  cancelarPropuesta(): void {
    const propuesta = this.propuestaPendiente();
    if (!propuesta || this.resolviendoPropuesta()) return;

    this.resolviendoPropuesta.set(true);
    this.errorPropuesta.set('');

    this.pedidoService.cancelarPropuestaTerminos(this.idPedido, propuesta.idPropuesta).subscribe({
      next: () => {
        this.propuestaPendiente.set(null);
        this.resolviendoPropuesta.set(false);
      },
      error: (err) => {
        this.errorPropuesta.set(err.error?.detail || 'No se pudo cancelar la propuesta');
        this.resolviendoPropuesta.set(false);
      }
    });
  }

  onPrecioPropuesto(evento: Event): void {
    const valor = (evento.target as HTMLInputElement).value;
    this.precioPropuesto.set(valor ? Number(valor) : null);
  }

  onFechaPropuesta(evento: Event): void {
    this.fechaPropuesta.set((evento.target as HTMLInputElement).value);
  }

  ngOnDestroy(): void {
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
