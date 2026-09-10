import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RespaldoService } from '../../services/respaldo.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { ProgramacionRespaldoFormModalComponent } from '../../../../shared/components/programacion-respaldo-form-modal/programacion-respaldo-form-modal.component';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';
import {
  CrearProgramacionRequest,
  EstadoRespaldo,
  FiltroRespaldo,
  ProgramacionRespaldoResponse,
  RespaldoResponse,
  TipoRespaldo
} from '../../models/respaldo.model';

@Component({
  selector: 'app-respaldos',
  standalone: true,
  imports: [FormsModule, DatePipe, ConfirmDialogComponent, ProgramacionRespaldoFormModalComponent, HasPermissionDirective],
  templateUrl: './respaldos.component.html'
})
export class RespaldosComponent implements OnInit {
  private respaldoService = inject(RespaldoService);
  private toastService = inject(ToastService);

  readonly vistaActiva = signal<'respaldos' | 'programaciones'>('respaldos');

  // ─── Respaldos ───
  readonly respaldos = signal<RespaldoResponse[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly isLoading = signal<boolean>(false);
  readonly isCreandoRespaldo = signal<boolean>(false);
  readonly tipoRespaldoNuevo = signal<TipoRespaldo>('FULL');

  selectedTipoFiltro = 'ALL';
  selectedEstadoFiltro = 'ALL';
  readonly filtroAplicado = signal<FiltroRespaldo>({});

  readonly isConfirmEliminarRespaldoOpen = signal<boolean>(false);
  readonly respaldoSeleccionado = signal<RespaldoResponse | null>(null);

  // ─── Programaciones ───
  readonly programaciones = signal<ProgramacionRespaldoResponse[]>([]);
  readonly isLoadingProgramaciones = signal<boolean>(false);
  readonly isFormModalOpen = signal<boolean>(false);
  readonly formModalMode = signal<'create' | 'edit'>('create');
  readonly programacionSeleccionada = signal<ProgramacionRespaldoResponse | null>(null);
  readonly isActionLoading = signal<boolean>(false);

  readonly isConfirmEliminarProgramacionOpen = signal<boolean>(false);

  ngOnInit(): void {
    this.cargarRespaldos();
    this.cargarProgramaciones();
  }

  cambiarVista(vista: 'respaldos' | 'programaciones'): void {
    this.vistaActiva.set(vista);
  }

  // ─── Respaldos: listado y filtros ───

  private filtroDesdeBorrador(): FiltroRespaldo {
    return {
      tipoRespaldo: this.selectedTipoFiltro !== 'ALL' ? (this.selectedTipoFiltro as TipoRespaldo) : undefined,
      estadoRespaldo: this.selectedEstadoFiltro !== 'ALL' ? (this.selectedEstadoFiltro as EstadoRespaldo) : undefined
    };
  }

  aplicarFiltros(): void {
    this.filtroAplicado.set(this.filtroDesdeBorrador());
    this.currentPage.set(0);
    this.cargarRespaldos();
  }

  limpiarFiltros(): void {
    this.selectedTipoFiltro = 'ALL';
    this.selectedEstadoFiltro = 'ALL';
    this.aplicarFiltros();
  }

  cargarRespaldos(): void {
    this.isLoading.set(true);
    this.respaldoService.listar(this.filtroAplicado(), this.currentPage(), this.pageSize()).subscribe({
      next: (res) => {
        this.respaldos.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo cargar el listado de respaldos');
      }
    });
  }

  changePage(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages()) {
      this.currentPage.set(newPage);
      this.cargarRespaldos();
    }
  }

  // ─── Respaldos: crear / descargar / eliminar ───

  generarRespaldo(): void {
    this.isCreandoRespaldo.set(true);
    this.respaldoService.crear({ tipoRespaldo: this.tipoRespaldoNuevo() }).subscribe({
      next: () => {
        this.isCreandoRespaldo.set(false);
        this.toastService.success('Respaldo iniciado. Actualiza el listado en unos segundos para ver su estado.');
        this.cargarRespaldos();
      },
      error: (err) => {
        this.isCreandoRespaldo.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo iniciar el respaldo');
      }
    });
  }

  descargar(respaldo: RespaldoResponse): void {
    this.respaldoService.descargar(respaldo.idRespaldo).subscribe({
      next: (respuesta) => descargarRespuesta(respuesta, respaldo.nombreArchivo || `respaldo_${respaldo.idRespaldo}`),
      error: async (err) => {
        const mensaje = await mensajeErrorBlob(err, 'No se pudo descargar el respaldo');
        this.toastService.error(mensaje);
      }
    });
  }

  confirmarEliminarRespaldo(respaldo: RespaldoResponse): void {
    this.respaldoSeleccionado.set(respaldo);
    this.isConfirmEliminarRespaldoOpen.set(true);
  }

  ejecutarEliminarRespaldo(): void {
    const respaldo = this.respaldoSeleccionado();
    if (!respaldo) return;

    this.isActionLoading.set(true);
    this.respaldoService.eliminar(respaldo.idRespaldo).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.isConfirmEliminarRespaldoOpen.set(false);
        this.toastService.success('Respaldo eliminado exitosamente');
        this.cargarRespaldos();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo eliminar el respaldo');
      }
    });
  }

  // ─── Programaciones ───

  cargarProgramaciones(): void {
    this.isLoadingProgramaciones.set(true);
    this.respaldoService.listarProgramaciones().subscribe({
      next: (res) => {
        this.programaciones.set(res);
        this.isLoadingProgramaciones.set(false);
      },
      error: (err) => {
        this.isLoadingProgramaciones.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo cargar las programaciones de respaldo');
      }
    });
  }

  abrirCrearProgramacion(): void {
    this.formModalMode.set('create');
    this.programacionSeleccionada.set(null);
    this.isFormModalOpen.set(true);
  }

  abrirEditarProgramacion(programacion: ProgramacionRespaldoResponse): void {
    this.formModalMode.set('edit');
    this.programacionSeleccionada.set(programacion);
    this.isFormModalOpen.set(true);
  }

  guardarProgramacion(request: CrearProgramacionRequest): void {
    this.isActionLoading.set(true);
    const seleccionada = this.programacionSeleccionada();

    const obs = this.formModalMode() === 'edit' && seleccionada
      ? this.respaldoService.actualizarProgramacion(seleccionada.idProgramacion, request)
      : this.respaldoService.crearProgramacion(request);

    obs.subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.isFormModalOpen.set(false);
        this.toastService.success(this.formModalMode() === 'edit' ? 'Programación actualizada exitosamente' : 'Programación creada exitosamente');
        this.cargarProgramaciones();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo guardar la programación');
      }
    });
  }

  alternarActivo(programacion: ProgramacionRespaldoResponse): void {
    this.respaldoService.cambiarEstadoProgramacion(programacion.idProgramacion, !programacion.activo).subscribe({
      next: () => {
        this.toastService.success(`Programación ${!programacion.activo ? 'activada' : 'desactivada'} exitosamente`);
        this.cargarProgramaciones();
      },
      error: (err) => this.toastService.error(err.error?.detail || 'No se pudo cambiar el estado de la programación')
    });
  }

  confirmarEliminarProgramacion(programacion: ProgramacionRespaldoResponse): void {
    this.programacionSeleccionada.set(programacion);
    this.isConfirmEliminarProgramacionOpen.set(true);
  }

  ejecutarEliminarProgramacion(): void {
    const programacion = this.programacionSeleccionada();
    if (!programacion) return;

    this.isActionLoading.set(true);
    this.respaldoService.eliminarProgramacion(programacion.idProgramacion).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.isConfirmEliminarProgramacionOpen.set(false);
        this.toastService.success('Programación eliminada exitosamente');
        this.cargarProgramaciones();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo eliminar la programación');
      }
    });
  }

  // ─── Presentación ───

  formatearTamano(bytes: number | null): string {
    if (bytes === null || bytes === undefined) return '—';
    const unidades = ['B', 'KB', 'MB', 'GB'];
    let valor = bytes;
    let i = 0;
    while (valor >= 1024 && i < unidades.length - 1) {
      valor /= 1024;
      i++;
    }
    return `${valor.toFixed(1)} ${unidades[i]}`;
  }

  claseEstado(estado: EstadoRespaldo): string {
    switch (estado) {
      case 'COMPLETADO': return 'bg-secondary-container text-on-secondary-container';
      case 'FALLIDO': return 'bg-error-container text-on-error-container';
      default: return 'bg-surface-container text-on-surface-variant';
    }
  }
}
