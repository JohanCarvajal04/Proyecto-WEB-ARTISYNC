import { Component, Input, OnInit, inject, signal, computed } from '@angular/core';
import { BriefingService } from '../../services/briefing.service';
import { RespuestaBriefing, RespuestaItemBriefing } from '../../models/comunicacion.model';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-briefing-pedido',
  standalone: true,
  imports: [],
  templateUrl: './briefing-pedido.component.html'
})
export class BriefingPedidoComponent implements OnInit {

  @Input({ required: true }) idPedido!: number;
  /** Solo el cliente responde; el creador ve el briefing en modo lectura. */
  @Input() puedeResponder = false;

  private briefingService = inject(BriefingService);
  private toast = inject(ToastService);

  readonly briefing = signal<RespuestaBriefing | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly enviando = signal<boolean>(false);
  readonly error = signal<string>('');

  /** Borradores por idPregunta, mientras el briefing no se haya enviado. */
  readonly respuestas = signal<Record<number, string>>({});

  readonly preguntasOrdenadas = computed(() =>
    [...(this.briefing()?.preguntas ?? [])].sort((a, b) => a.numeroOrden - b.numeroOrden));

  readonly todasRespondidas = computed(() => {
    const borradores = this.respuestas();
    const preguntas = this.preguntasOrdenadas();
    return preguntas.length > 0 && preguntas.every(p => (borradores[p.idPregunta] ?? '').trim().length > 0);
  });

  readonly formularioAbierto = computed(() =>
    this.puedeResponder && this.briefing() !== null && !this.briefing()!.completado);

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
      // Un 404 significa que el creador todavía no envió briefing a este pedido.
      error: () => {
        this.briefing.set(null);
        this.isLoading.set(false);
      }
    });
  }

  onRespuesta(idPregunta: number, evento: Event): void {
    const texto = (evento.target as HTMLTextAreaElement).value;
    this.respuestas.update(r => ({ ...r, [idPregunta]: texto }));
  }

  enviar(): void {
    if (!this.todasRespondidas() || this.enviando()) return;

    const borradores = this.respuestas();
    const payload: RespuestaItemBriefing[] = this.preguntasOrdenadas().map(p => ({
      idPregunta: p.idPregunta,
      textoRespuesta: (borradores[p.idPregunta] ?? '').trim()
    }));

    this.enviando.set(true);
    this.error.set('');

    this.briefingService.responderBriefing(this.idPedido, payload).subscribe({
      next: (briefing) => {
        this.briefing.set(briefing);
        this.enviando.set(false);
        this.toast.success('Briefing enviado. Las respuestas ya no se pueden modificar.');
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo enviar el briefing');
        this.enviando.set(false);
      }
    });
  }

  formatFecha(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
