import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PedidoService } from '../../../pedido/services/pedido.service';
import {
  RespuestaSeguimientoPedido,
  RespuestaEtapaConfig,
  RespuestaHistorialEstado
} from '../../../pedido/models/pedido.model';

export type EstadoEtapa = 'completada' | 'actual' | 'pendiente';

export interface EtapaTimeline {
  config: RespuestaEtapaConfig;
  estado: EstadoEtapa;
  registro?: RespuestaHistorialEstado;
}

@Component({
  selector: 'app-seguimiento',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './seguimiento.component.html'
})
export class SeguimientoComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private pedidoService = inject(PedidoService);

  readonly seguimiento = signal<RespuestaSeguimientoPedido | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');
  readonly idPedido = signal<number | null>(null);

  etapas = computed<EtapaTimeline[]>(() => {
    const data = this.seguimiento();
    if (!data) return [];

    const historial = data.historial || [];
    return (data.etapasDelFlujo || [])
      .slice()
      .sort((a, b) => a.numeroOrden - b.numeroOrden)
      .map(config => {
        let estado: EstadoEtapa = 'pendiente';
        if (config.numeroOrden < data.etapaActualOrden) estado = 'completada';
        else if (config.numeroOrden === data.etapaActualOrden) estado = 'actual';

        const registro = historial
          .filter(h => h.nombreEtapa === config.nombreEtapa)
          .sort((a, b) => new Date(b.fechaTransicion).getTime() - new Date(a.fechaTransicion).getTime())[0];

        return { config, estado, registro };
      });
  });

  progreso = computed(() => {
    const data = this.seguimiento();
    if (!data) return 0;
    return Math.max(0, Math.min(100, Math.round(data.porcentajeProgreso || 0)));
  });

  estaFinalizado = computed(() => this.progreso() >= 100);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id || Number.isNaN(id)) {
      this.error.set('Pedido no válido');
      this.isLoading.set(false);
      return;
    }
    this.idPedido.set(id);
    this.loadSeguimiento(id);
  }

  loadSeguimiento(id: number): void {
    this.isLoading.set(true);
    this.error.set('');
    this.pedidoService.obtenerSeguimiento(id).subscribe({
      next: (data) => {
        this.seguimiento.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo cargar el seguimiento de este pedido');
        this.isLoading.set(false);
      }
    });
  }

  reintentar(): void {
    const id = this.idPedido();
    if (id) this.loadSeguimiento(id);
  }

  formatDate(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }
}
