import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { interval, Subscription, switchMap, of, catchError } from 'rxjs';
import { PedidoService } from '../../services/pedido.service';
import { TicketRevisionService } from '../../services/ticket-revision.service';
import { RespuestaPedido, RespuestaSeguimientoPedido, RespuestaTicketRevision, PeticionAvanzarEtapa, PeticionCrearTicketRevision } from '../../models/pedido.model';
import { AuthService } from '../../../seguridad/services/auth.service';
import { EntregableService } from '../../../legal/services/entregable.service';
import { ContratoService } from '../../../legal/services/contrato.service';
import { RespuestaContrato } from '../../../legal/models/legal.model';
import { ChatPedidoComponent } from '../../../comunicacion/components/chat-pedido/chat-pedido.component';
import { BriefingPedidoComponent } from '../../../comunicacion/components/briefing-pedido/briefing-pedido.component';
import { ResenaFormComponent } from '../../../social/components/resena-form/resena-form.component';
import { ToastService } from '../../../../core/services/toast.service';
import { MonedaPipe } from '../../../../shared/pipes/moneda.pipe';

@Component({
  selector: 'app-pedido-detalle',
  standalone: true,
  imports: [FormsModule, RouterLink, ChatPedidoComponent, BriefingPedidoComponent, ResenaFormComponent, MonedaPipe],
  templateUrl: './pedido-detalle.component.html'
})
export class PedidoDetalleComponent implements OnInit, OnDestroy {
  pedido: RespuestaPedido | null = null;
  seguimiento: RespuestaSeguimientoPedido | null = null;
  tickets: RespuestaTicketRevision[] = [];
  contrato: RespuestaContrato | null = null;
  loading = true;
  error = '';

  /**
   * La reseña solo se habilita tras la aprobación del entregable, que es la
   * misma condición que impone el backend (RF-09).
   */
  entregaAprobada = false;

  // Avanzar etapa
  observacion = '';
  avanzando = false;

  // Ticket
  showTicketForm = false;
  nuevoTicket: PeticionCrearTicketRevision = { idMotivo: 1, descripcionCliente: '' };
  creandoTicket = false;

  private pollingSubscription?: Subscription;
  /** Público: los componentes embebidos (chat, briefing, reseña) lo reciben como @Input. */
  pedidoId = 0;

  constructor(
    private pedidoService: PedidoService,
    private ticketService: TicketRevisionService,
    private entregableService: EntregableService,
    private contratoService: ContratoService,
    public authService: AuthService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.pedidoId = Number(this.route.snapshot.paramMap.get('id'));
    this.cargarDatos();

    // Polling cada 5 segundos para actualización en tiempo real (RF-19).
    // El catchError va dentro del switchMap: fuera, un error puntual completaba
    // el stream y el seguimiento dejaba de refrescarse el resto de la sesión.
    this.pollingSubscription = interval(5000).pipe(
      switchMap(() => this.pedidoService.obtenerSeguimiento(this.pedidoId).pipe(catchError(() => of(null))))
    ).subscribe(seg => {
      if (seg) this.seguimiento = seg;
      // La app corre con detección de cambios zoneless: varias peticiones HTTP
      // concurrentes en ngOnInit más este polling en segundo plano dejaban la
      // vista pintada con datos viejos (o con el spinner) aunque el estado ya
      // se hubiera actualizado, porque ninguna fuente rastreada por Angular
      // disparaba el siguiente tick. markForCheck lo fuerza explícitamente.
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }

  cargarDatos(): void {
    this.loading = true;
    this.pedidoService.obtenerPedido(this.pedidoId).subscribe({
      next: (pedido) => {
        this.pedido = pedido;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al cargar el pedido';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });

    this.pedidoService.obtenerSeguimiento(this.pedidoId).subscribe({
      next: (seg) => {
        this.seguimiento = seg;
        this.cdr.markForCheck();
      },
      // El polling de más abajo lo reintenta cada 5s, pero si la primera carga
      // falla el usuario no debe quedarse sin ninguna pista de por qué no ve nada.
      error: () => this.toast.error('No se pudo cargar el seguimiento del pedido')
    });

    this.ticketService.listarTickets(this.pedidoId).subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.cdr.markForCheck();
      },
      error: () => this.toast.error('No se pudieron cargar los tickets de revisión')
    });

    // Un 404 aquí solo significa que aún no hay entregable para este pedido.
    this.entregableService.obtenerEntregable(this.pedidoId)
      .pipe(catchError(() => of(null)))
      .subscribe(entregable => {
        this.entregaAprobada = entregable?.estaLiberado === true;
        this.cdr.markForCheck();
      });

    // 404 aquí = contrato aún no generado; también es la señal de que la
    // negociación de términos sigue abierta.
    this.contratoService.obtenerContratoPorPedido(this.pedidoId)
      .pipe(catchError(() => of(null)))
      .subscribe(contrato => {
        this.contrato = contrato;
        this.cdr.markForCheck();
      });
  }

  avanzarEtapa(): void {
    if (this.avanzando) return;
    this.avanzando = true;

    const peticion: PeticionAvanzarEtapa = { observacion: this.observacion };
    this.pedidoService.avanzarEtapa(this.pedidoId, peticion).subscribe({
      next: (pedido) => {
        this.pedido = pedido;
        this.observacion = '';
        this.avanzando = false;
        this.cargarDatos();
      },
      error: (err) => {
        // El backend responde ProblemDetail (RFC 7807): el mensaje va en
        // `detail`, no en `message`.
        this.error = err.error?.detail || 'No se pudo avanzar la etapa';
        this.avanzando = false;
      }
    });
  }

  crearTicket(): void {
    if (this.creandoTicket) return;

    // El botón solo comprobaba que la cadena no estuviera vacía, así que
    // un textarea con puros espacios pasaba igual (string no vacía) y el
    // backend quedaba como única línea de defensa real.
    const descripcion = this.nuevoTicket.descripcionCliente.trim();
    if (!descripcion) {
      this.error = 'Describe el motivo de la revisión.';
      return;
    }
    this.nuevoTicket.descripcionCliente = descripcion;

    this.creandoTicket = true;

    this.ticketService.crearTicket(this.pedidoId, this.nuevoTicket).subscribe({
      next: (ticket) => {
        this.tickets = [ticket, ...this.tickets];
        this.nuevoTicket = { idMotivo: 1, descripcionCliente: '' };
        this.showTicketForm = false;
        this.creandoTicket = false;
      },
      error: (err) => {
        this.error = err.error?.detail || 'Error al crear ticket';
        this.creandoTicket = false;
      }
    });
  }

  cambiarEstadoTicket(idTicket: number, estado: string): void {
    this.ticketService.cambiarEstado(idTicket, estado).subscribe({
      next: (updated) => {
        const idx = this.tickets.findIndex(t => t.idTicket === idTicket);
        if (idx > -1) this.tickets[idx] = updated;
      }
    });
  }

  /**
   * Por identidad (idCreador de ESTE pedido), no por rol global: el backend
   * tampoco da paso libre a ADMIN en avanzarEtapa/aprobarEntrega/etc (ver
   * PedidoServicioImpl, EntregableServicioImpl — todo por identidad, sin
   * excepción para ADMIN), así que un rol global aquí solo lograba mostrar
   * botones que el servidor iba a rechazar de todas formas. Además, una
   * cuenta con ambos roles CLIENTE y CREADOR (el admin puede asignar los dos)
   * veía secciones de "creador" en un pedido donde en realidad es el
   * cliente, solo por tener ese rol en otro servicio suyo.
   */
  get esCreador(): boolean {
    return this.pedido?.idCreador === this.authService.getCurrentUserId();
  }

  get esCliente(): boolean {
    return this.pedido?.idCliente === this.authService.getCurrentUserId();
  }

  get progresoRedondeado(): number {
    const pct = this.seguimiento?.porcentajeProgreso ?? 0;
    return Math.max(0, Math.min(100, Math.round(pct)));
  }

  getStepStatus(etapaOrden: number): 'completed' | 'active' | 'pending' {
    if (!this.seguimiento) return 'pending';
    if (etapaOrden < this.seguimiento.etapaActualOrden) return 'completed';
    if (etapaOrden === this.seguimiento.etapaActualOrden) return 'active';
    return 'pending';
  }

  formatDate(date: string): string {
    if (!date) return '—';
    return new Date(date).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }


  getTicketBadge(estado: string): string {
    switch (estado?.toLowerCase()) {
      case 'abierto': return 'bg-amber-50 text-amber-700';
      case 'resuelto': return 'bg-emerald-50 text-emerald-700';
      case 'rechazado': return 'bg-rose-50 text-rose-700';
      default: return 'bg-sky-50 text-sky-700';
    }
  }
}
