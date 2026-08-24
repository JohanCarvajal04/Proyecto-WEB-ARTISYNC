import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditoriaService } from '../../services/auditoria.service';
import {
  EventoAuditoria, EventoAuditoriaResumen, FiltroAuditoria,
  MODULOS_AUDITORIA, RESULTADOS_AUDITORIA
} from '../../models/auditoria.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [FormsModule, HasPermissionDirective],
  templateUrl: './auditoria.component.html'
})
export class AuditoriaComponent implements OnInit {

  private auditoriaService = inject(AuditoriaService);
  private toastService = inject(ToastService);

  readonly modulos = MODULOS_AUDITORIA;
  readonly resultados = RESULTADOS_AUDITORIA;

  readonly pagina = signal<Pagina<EventoAuditoriaResumen>>(paginaVacia());
  readonly isLoading = signal<boolean>(true);
  readonly acciones = signal<string[]>([]);

  readonly filtro = signal<FiltroAuditoria>({});

  readonly detalle = signal<EventoAuditoria | null>(null);
  readonly cargandoDetalle = signal<boolean>(false);

  readonly exportando = signal<boolean>(false);

  ngOnInit(): void {
    this.auditoriaService.listarAcciones().subscribe({
      next: (acciones) => this.acciones.set(acciones),
      error: () => this.acciones.set([])
    });
    this.cargar(0);
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.auditoriaService.listar(this.filtro(), page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudo cargar la bitácora de auditoría');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  actualizarFiltro<K extends keyof FiltroAuditoria>(campo: K, valor: FiltroAuditoria[K]): void {
    this.filtro.update(f => ({ ...f, [campo]: valor }));
  }

  aplicarFiltros(): void {
    this.cargar(0);
  }

  limpiarFiltros(): void {
    this.filtro.set({});
    this.cargar(0);
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.cargar(numero);
  }

  verDetalle(evento: EventoAuditoriaResumen): void {
    this.cargandoDetalle.set(true);
    this.auditoriaService.obtener(evento.idEventoAuditoria).subscribe({
      next: (completo) => {
        this.detalle.set(completo);
        this.cargandoDetalle.set(false);
      },
      error: () => {
        this.toastService.error('No se pudo cargar el detalle del evento');
        this.cargandoDetalle.set(false);
      }
    });
  }

  cerrarDetalle(): void {
    this.detalle.set(null);
  }

  exportar(): void {
    this.exportando.set(true);
    this.auditoriaService.exportarCsv(this.filtro()).subscribe({
      next: (blob) => this.descargar(blob),
      error: async (err) => {
        this.exportando.set(false);
        const detalle = err?.error instanceof Blob
          ? JSON.parse(await err.error.text())
          : err?.error;
        this.toastService.error(detalle?.detail || 'No se pudo exportar la bitácora');
      }
    });
  }

  private descargar(blob: Blob): void {
    this.exportando.set(false);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `auditoria_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  formatFecha(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  json(valor: unknown): string {
    return JSON.stringify(valor, null, 2);
  }

  claseBadge(resultado: string): string {
    switch (resultado) {
      case 'EXITO': return 'bg-emerald-50 text-emerald-700';
      case 'FALLIDO': return 'bg-rose-50 text-rose-700';
      case 'DENEGADO': return 'bg-amber-50 text-amber-700';
      default: return 'bg-slate-50 text-slate-600';
    }
  }
}
