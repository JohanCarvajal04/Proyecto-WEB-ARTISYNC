import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { ToastService } from '../../../../core/services/toast.service';
import { PedidoService } from '../../../pedido/services/pedido.service';
import { TicketRevisionService } from '../../../pedido/services/ticket-revision.service';
import { EntregableService } from '../../../legal/services/entregable.service';
import { ContratoService } from '../../../legal/services/contrato.service';
import {
  RespuestaPedido,
  RespuestaSeguimientoPedido,
  RespuestaTicketRevision
} from '../../../pedido/models/pedido.model';
import { RespuestaEntregable, RespuestaContrato } from '../../../legal/models/legal.model';
import { ACEPTA_ENTREGABLE, formatSize, validarEntregable } from '../../../legal/utils/archivo-entregable';
import { BriefingService } from '../../../comunicacion/services/briefing.service';
import { RespuestaBriefing } from '../../../comunicacion/models/comunicacion.model';
import { ChatPedidoComponent } from '../../../comunicacion/components/chat-pedido/chat-pedido.component';
import { BriefingPedidoComponent } from '../../../comunicacion/components/briefing-pedido/briefing-pedido.component';
import { formatPrice, formatDate, formatDateTime, badgeEtapa, mensajeError } from '../../utils/formato';

@Component({
  selector: 'app-comision-detalle',
  standalone: true,
  imports: [RouterLink, ChatPedidoComponent, BriefingPedidoComponent],
  templateUrl: './comision-detalle.component.html',
  styleUrl: './comision-detalle.component.css'
})
export class ComisionDetalleComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private pedidoService = inject(PedidoService);
  private ticketService = inject(TicketRevisionService);
  private entregableService = inject(EntregableService);
  private contratoService = inject(ContratoService);
  private briefingService = inject(BriefingService);
  private toast = inject(ToastService);

  readonly idPedido = signal<number>(0);
  readonly pedido = signal<RespuestaPedido | null>(null);
  readonly seguimiento = signal<RespuestaSeguimientoPedido | null>(null);
  readonly entregable = signal<RespuestaEntregable | null>(null);
  readonly contrato = signal<RespuestaContrato | null>(null);
  readonly tickets = signal<RespuestaTicketRevision[]>([]);

  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  // Avanzar etapa
  readonly observacion = signal<string>('');
  readonly avanzando = signal<boolean>(false);

  // Subida de entregable
  readonly archivoMarcaAgua = signal<File | null>(null);
  readonly archivoLimpia = signal<File | null>(null);
  readonly subiendo = signal<boolean>(false);

  readonly tiposAceptados = ACEPTA_ENTREGABLE;
  readonly formatSize = formatSize;

  // Envío de briefing al cliente
  readonly plantillas = signal<RespuestaBriefing[]>([]);
  readonly briefingEnviado = signal<RespuestaBriefing | null>(null);
  readonly plantillaElegida = signal<number | null>(null);
  readonly enviandoBriefing = signal<boolean>(false);

  readonly firmando = signal<boolean>(false);
  readonly ticketEnCurso = signal<number | null>(null);

  readonly estadosTicket = ['EN_REVISION', 'APROBADO', 'RECHAZADO', 'CERRADO'];

  progreso = computed(() => this.seguimiento()?.porcentajeProgreso ?? 0);

  etapaFinalAlcanzada = computed(() => {
    const s = this.seguimiento();
    if (!s) return false;
    return s.etapaActualOrden >= s.totalEtapas;
  });

  ticketsAbiertos = computed(() =>
    this.tickets().filter(t => (t.estadoTicket || '').toUpperCase() !== 'CERRADO').length
  );

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.idPedido.set(id);
    this.cargar();
  }

  cargar(): void {
    const id = this.idPedido();
    if (!id) return;

    this.isLoading.set(true);
    this.error.set('');

    // Solo el pedido es obligatorio: contrato, entregable y tickets pueden no
    // existir todavía según la etapa, y un 404 ahí no es un fallo de la vista.
    forkJoin({
      pedido: this.pedidoService.obtenerPedido(id),
      seguimiento: this.pedidoService.obtenerSeguimiento(id).pipe(catchError(() => of(null))),
      entregable: this.entregableService.obtenerEntregable(id).pipe(catchError(() => of(null))),
      contrato: this.contratoService.obtenerContratoPorPedido(id).pipe(catchError(() => of(null))),
      tickets: this.ticketService.listarTickets(id).pipe(catchError(() => of([] as RespuestaTicketRevision[]))),
      briefing: this.briefingService.obtenerBriefing(id).pipe(catchError(() => of(null))),
      plantillas: this.briefingService.listarMisPlantillas().pipe(catchError(() => of([] as RespuestaBriefing[])))
    }).subscribe({
      next: ({ pedido, seguimiento, entregable, contrato, tickets, briefing, plantillas }) => {
        this.pedido.set(pedido);
        this.seguimiento.set(seguimiento);
        this.entregable.set(entregable);
        this.contrato.set(contrato);
        this.tickets.set(tickets);
        this.briefingEnviado.set(briefing);
        this.plantillas.set(plantillas);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar la comisión'));
        this.isLoading.set(false);
      }
    });
  }

  onObservacion(event: Event): void {
    this.observacion.set((event.target as HTMLTextAreaElement).value);
  }

  avanzarEtapa(): void {
    const id = this.idPedido();
    const texto = this.observacion().trim();
    if (!texto) {
      this.toast.warning('Describe brevemente el avance antes de cambiar de etapa');
      return;
    }

    this.avanzando.set(true);
    this.pedidoService.avanzarEtapa(id, { observacion: texto }).subscribe({
      next: (pedido) => {
        this.pedido.set(pedido);
        this.observacion.set('');
        this.avanzando.set(false);
        this.toast.success(`Comisión movida a "${pedido.etapaActual}"`);
        this.pedidoService.obtenerSeguimiento(id).subscribe({
          next: (s) => this.seguimiento.set(s),
          error: () => {}
        });
      },
      error: (err) => {
        this.avanzando.set(false);
        this.toast.error(mensajeError(err, 'No se pudo avanzar la etapa'));
      }
    });
  }

  // ── Briefing ──────────────────────────────────────────────────────────────

  onPlantilla(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    this.plantillaElegida.set(valor ? Number(valor) : null);
  }

  enviarBriefing(): void {
    const idPlantilla = this.plantillaElegida();
    if (idPlantilla === null || this.enviandoBriefing()) return;

    this.enviandoBriefing.set(true);
    this.briefingService.enviarBriefing(this.idPedido(), idPlantilla).subscribe({
      next: (briefing) => {
        this.briefingEnviado.set(briefing);
        this.enviandoBriefing.set(false);
        this.toast.success('Briefing enviado. El cliente ya puede responderlo.');
      },
      error: (err) => {
        this.enviandoBriefing.set(false);
        this.toast.error(mensajeError(err, 'No se pudo enviar el briefing'));
      }
    });
  }

  // ── Entregable ────────────────────────────────────────────────────────────

  seleccionarMarcaAgua(evento: Event): void {
    this.archivoMarcaAgua.set(this.leerArchivo(evento));
  }

  seleccionarLimpia(evento: Event): void {
    this.archivoLimpia.set(this.leerArchivo(evento));
  }

  private leerArchivo(evento: Event): File | null {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    if (!archivo) return null;

    const validacion = validarEntregable(archivo);
    if (validacion) {
      this.toast.warning(validacion);
      input.value = '';
      return null;
    }
    return archivo;
  }

  subirEntregable(): void {
    const id = this.idPedido();
    const marcaAgua = this.archivoMarcaAgua();
    const limpia = this.archivoLimpia();
    if (!marcaAgua || !limpia) {
      this.toast.warning('Necesitas la versión con marca de agua y la versión limpia');
      return;
    }

    this.subiendo.set(true);
    this.entregableService.subirEntregable(id, marcaAgua, limpia).subscribe({
      next: (entregable) => {
        this.entregable.set(entregable);
        this.archivoMarcaAgua.set(null);
        this.archivoLimpia.set(null);
        this.subiendo.set(false);
        this.toast.success('Entregable registrado. El cliente ya puede revisarlo.');
      },
      error: (err) => {
        this.subiendo.set(false);
        this.toast.error(mensajeError(err, 'No se pudo registrar el entregable'));
      }
    });
  }

  firmarContrato(): void {
    const contrato = this.contrato();
    if (!contrato) return;

    this.firmando.set(true);
    this.contratoService.firmarContrato(contrato.idContrato).subscribe({
      next: (actualizado) => {
        this.contrato.set(actualizado);
        this.firmando.set(false);
        this.toast.success('Contrato firmado');
      },
      error: (err) => {
        this.firmando.set(false);
        this.toast.error(mensajeError(err, 'No se pudo firmar el contrato'));
      }
    });
  }

  cambiarEstadoTicket(ticket: RespuestaTicketRevision, event: Event): void {
    const nuevoEstado = (event.target as HTMLSelectElement).value;
    if (!nuevoEstado || nuevoEstado === ticket.estadoTicket) return;

    this.ticketEnCurso.set(ticket.idTicket);
    this.ticketService.cambiarEstado(ticket.idTicket, nuevoEstado).subscribe({
      next: (actualizado) => {
        this.tickets.update(lista => lista.map(t => t.idTicket === ticket.idTicket ? actualizado : t));
        this.ticketEnCurso.set(null);
        this.toast.success('Estado del ticket actualizado');
      },
      error: (err) => {
        this.ticketEnCurso.set(null);
        this.toast.error(mensajeError(err, 'No se pudo actualizar el ticket'));
      }
    });
  }

  formatPrice = formatPrice;
  formatDate = formatDate;
  formatDateTime = formatDateTime;
  badgeEtapa = badgeEtapa;
}
