import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SorteoPublicoService } from '../../services/sorteo-publico.service';
import { RespuestaSorteo } from '../../models/social.model';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-sorteos-cliente',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './sorteos-cliente.component.html'
})
export class SorteosClienteComponent implements OnInit {

  private sorteoService = inject(SorteoPublicoService);
  private toast = inject(ToastService);

  readonly sorteos = signal<RespuestaSorteo[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  /** id del sorteo cuya inscripción se está resolviendo. */
  readonly enCurso = signal<number | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.isLoading.set(true);
    this.error.set('');

    this.sorteoService.listarActivos().subscribe({
      next: (sorteos) => {
        this.sorteos.set(sorteos);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudieron cargar los sorteos');
        this.isLoading.set(false);
      }
    });
  }

  alternarParticipacion(sorteo: RespuestaSorteo): void {
    if (this.enCurso() !== null) return;
    this.enCurso.set(sorteo.idSorteo);

    const accion$ = sorteo.yoParticipo
      ? this.sorteoService.cancelarParticipacion(sorteo.idSorteo)
      : this.sorteoService.participar(sorteo.idSorteo);

    accion$.subscribe({
      next: () => {
        // Se refresca el sorteo concreto en vez de recargar la lista entera:
        // `totalParticipantes` y `yoParticipo` los recalcula el backend.
        this.sorteoService.obtenerSorteo(sorteo.idSorteo).subscribe({
          next: (actualizado) => {
            this.sorteos.update(lista =>
              lista.map(s => s.idSorteo === actualizado.idSorteo ? actualizado : s));
            this.enCurso.set(null);
          },
          error: () => {
            this.enCurso.set(null);
            this.cargar();
          }
        });
        this.toast.success(sorteo.yoParticipo ? 'Inscripción cancelada' : '¡Ya estás participando!');
      },
      error: (err) => {
        this.enCurso.set(null);
        this.toast.error(err.error?.message || 'No se pudo completar la operación');
      }
    });
  }

  diasRestantes(fechaCierre: string): number | null {
    if (!fechaCierre) return null;
    const ms = new Date(fechaCierre).getTime() - Date.now();
    if (Number.isNaN(ms)) return null;
    return Math.max(0, Math.ceil(ms / 86_400_000));
  }

  formatFecha(fecha: string): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
