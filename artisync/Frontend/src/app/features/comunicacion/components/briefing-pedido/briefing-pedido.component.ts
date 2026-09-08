import { Component, Input, OnInit, inject, signal, computed } from '@angular/core';
import { BriefingService } from '../../services/briefing.service';
import { RespuestaBriefing } from '../../models/comunicacion.model';

/**
 * Muestra el cuestionario (briefing) ya respondido de un pedido.
 * REQ-F-016 ampliado: las respuestas se dan al crear el pedido
 * (pedido-crear.component), no aquí — este componente es solo de lectura,
 * para que cliente y creador vean lo que se respondió.
 */
@Component({
  selector: 'app-briefing-pedido',
  standalone: true,
  imports: [],
  templateUrl: './briefing-pedido.component.html'
})
export class BriefingPedidoComponent implements OnInit {

  @Input({ required: true }) idPedido!: number;

  private briefingService = inject(BriefingService);

  readonly briefing = signal<RespuestaBriefing | null>(null);
  readonly isLoading = signal<boolean>(true);

  readonly preguntasOrdenadas = computed(() =>
    [...(this.briefing()?.preguntas ?? [])].sort((a, b) => a.numeroOrden - b.numeroOrden));

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.isLoading.set(true);
    this.briefingService.obtenerBriefing(this.idPedido).subscribe({
      next: (briefing) => {
        this.briefing.set(briefing);
        this.isLoading.set(false);
      },
      // Un 404 significa que este servicio no tenía cuestionario asignado.
      error: () => {
        this.briefing.set(null);
        this.isLoading.set(false);
      }
    });
  }

  formatFecha(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
