import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ReporteContratoService } from '../../services/reporte-contrato.service';
import { FilaReporteContrato, FiltroReporteContrato } from '../../models/reporte-contrato.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';
import { BotonExportarComponent } from '../../../../shared/components/boton-exportar/boton-exportar.component';
import { FormatoReporte } from '../../../../shared/models/formato-reporte.model';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';

/**
 * Reporte de contratos formalizados (Backend: ReporteContratoControlador).
 * Cierra el permiso huérfano REPORTE_CONTRATO_EXPORTAR (V19__permisos_reportes.sql).
 */
@Component({
  selector: 'app-reportes-contratos',
  standalone: true,
  imports: [FormsModule, BotonExportarComponent, DatePipe, DecimalPipe],
  templateUrl: './reportes-contratos.component.html'
})
export class ReportesContratosComponent implements OnInit {

  private reporteService = inject(ReporteContratoService);
  private toastService = inject(ToastService);

  readonly filtro = signal<FiltroReporteContrato>({});
  readonly pagina = signal<Pagina<FilaReporteContrato>>(paginaVacia());
  readonly isLoading = signal(true);
  readonly exportando = signal(false);

  ngOnInit(): void {
    this.cargar(0);
  }

  actualizarFiltro<K extends keyof FiltroReporteContrato>(campo: K, valor: FiltroReporteContrato[K]): void {
    this.filtro.update(f => ({ ...f, [campo]: valor }));
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.reporteService.listar(this.filtro(), page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudo cargar el reporte de contratos');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
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

  exportar(formato: FormatoReporte): void {
    this.exportando.set(true);
    this.reporteService.exportar(this.filtro(), formato).subscribe({
      next: (respuesta) => {
        this.exportando.set(false);
        descargarRespuesta(respuesta, `contratos_${new Date().toISOString().slice(0, 10)}.${formato.toLowerCase()}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        const mensaje = await mensajeErrorBlob(err, 'No se pudo exportar el reporte de contratos');
        this.toastService.error(mensaje);
      }
    });
  }
}
