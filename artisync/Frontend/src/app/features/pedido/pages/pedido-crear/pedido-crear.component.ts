import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PedidoService } from '../../services/pedido.service';
import { PeticionCrearPedido } from '../../models/pedido.model';
import { CatalogoPublicoService } from '../../../catalogo/services/catalogo-publico.service';
import { RespuestaServicio } from '../../../catalogo/models/catalogo.model';
import { MonedaPipe } from '../../../../shared/pipes/moneda.pipe';

@Component({
  selector: 'app-pedido-crear',
  standalone: true,
  imports: [FormsModule, RouterLink, MonedaPipe],
  templateUrl: './pedido-crear.component.html'
})
export class PedidoCrearComponent implements OnInit {

  private pedidoService = inject(PedidoService);
  private catalogoService = inject(CatalogoPublicoService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  pedido: PeticionCrearPedido = {
    idServicio: 0,
    precioOfrecido: null,
    fechaEntregaEstimada: null
  };

  /**
   * El servicio se elige en el catálogo y llega por query param; esta pantalla
   * ya no pide teclear el id a mano.
   */
  readonly servicio = signal<RespuestaServicio | null>(null);
  readonly cargandoServicio = signal<boolean>(false);

  /**
   * Antes eran propiedades planas: en una app zoneless
   * (provideZonelessChangeDetection) una respuesta HTTP no notifica al
   * planificador de cambios por sí misma. La rama de éxito navegaba fuera y
   * disimulaba el problema, pero un error de creación de pedido podía no
   * llegar a pintarse nunca en pantalla.
   */
  readonly loading = signal(false);
  readonly error = signal('');

  /**
   * Preguntas del cuestionario del servicio (si tiene uno asignado), en el
   * orden en que deben responderse. REQ-F-016 ampliado: se responden aquí,
   * al crear el pedido, no con un envío manual del creador después.
   */
  readonly preguntasBriefing = computed(() =>
    [...(this.servicio()?.preguntasBriefing ?? [])].sort((a, b) => a.numeroOrden - b.numeroOrden));

  /** Borradores de respuesta por idPregunta. */
  readonly respuestasBriefing = signal<Record<number, string>>({});

  readonly todasLasPreguntasRespondidas = computed(() => {
    const borradores = this.respuestasBriefing();
    const preguntas = this.preguntasBriefing();
    return preguntas.every(p => (borradores[p.idPregunta] ?? '').trim().length > 0);
  });

  ngOnInit(): void {
    const idServicio = Number(this.route.snapshot.queryParamMap.get('idServicio'));
    if (!idServicio) return;

    this.pedido.idServicio = idServicio;
    this.cargarServicio(idServicio);
  }

  private cargarServicio(idServicio: number): void {
    this.cargandoServicio.set(true);
    this.catalogoService.obtenerServicio(idServicio).subscribe({
      next: (servicio) => {
        this.servicio.set(servicio);
        // El precio base es el punto de partida de la negociación.
        this.pedido.precioOfrecido = servicio.precioBase;
        this.cargandoServicio.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el servicio seleccionado.');
        this.cargandoServicio.set(false);
      }
    });
  }

  onRespuestaBriefing(idPregunta: number, evento: Event): void {
    const texto = (evento.target as HTMLTextAreaElement).value;
    this.respuestasBriefing.update(r => ({ ...r, [idPregunta]: texto }));
  }

  onSubmit(): void {
    if (!this.pedido.idServicio) {
      this.error.set('Elige un servicio en el catálogo antes de continuar.');
      return;
    }

    // El backend rechaza precioOfrecido <= 0 (@DecimalMin en
    // PeticionCrearPedido), pero avisar aquí evita el viaje al servidor.
    if (this.pedido.precioOfrecido != null && this.pedido.precioOfrecido <= 0) {
      this.error.set('El precio ofrecido debe ser mayor a 0.');
      return;
    }

    if (this.preguntasBriefing().length > 0 && !this.todasLasPreguntasRespondidas()) {
      this.error.set('Este servicio tiene un cuestionario: responde todas sus preguntas antes de continuar.');
      return;
    }

    if (this.preguntasBriefing().length > 0) {
      const borradores = this.respuestasBriefing();
      this.pedido.respuestasBriefing = this.preguntasBriefing().map(p => ({
        idPregunta: p.idPregunta,
        textoRespuesta: (borradores[p.idPregunta] ?? '').trim()
      }));
    }

    this.loading.set(true);
    this.error.set('');

    this.pedidoService.crearPedido(this.pedido).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.router.navigate(['/pedido', res.idPedido]);
      },
      error: (err) => {
        // ProblemDetail (RFC 7807): el mensaje va en `detail`, y una falla de
        // @Valid (p. ej. @Future en la fecha) además trae `fieldErrors` — no
        // hay un `message` de nivel superior, así que leerlo siempre caía al
        // genérico de abajo aunque el backend explicara el motivo real.
        const erroresPorCampo = err.error?.fieldErrors;
        this.error.set(
          (erroresPorCampo && Object.values(erroresPorCampo).join(', '))
          || err.error?.detail
          || 'Error al crear el pedido'
        );
        this.loading.set(false);
      }
    });
  }


  /**
   * `min` del input datetime-local: evita que el selector nativo ofrezca
   * siquiera una fecha pasada. El backend igual la rechaza con @Future
   * (PeticionCrearPedido) si alguien la fuerza por fuera del UI.
   */
  get minFechaEntrega(): string {
    const ahora = new Date(Date.now() - new Date().getTimezoneOffset() * 60000);
    return ahora.toISOString().slice(0, 16);
  }
}
