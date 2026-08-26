import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ReporteFinancieroService } from '../../services/reporte-financiero.service';
import { FiltroReporteFinanciero, RespuestaReporteComisiones } from '../../models/reporte-financiero.model';
import { ToastService } from '../../../../core/services/toast.service';
import { BotonExportarComponent } from '../../../../shared/components/boton-exportar/boton-exportar.component';
import { FormatoReporte } from '../../../../shared/models/formato-reporte.model';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';

/**
 * Reporte de comisiones por creador (Backend: ReporteFinancieroControlador).
 * Activa fn_reporte_comisiones_creador, que estaba escrita y verificada por
 * CI sin ningún consumidor Angular hasta esta pantalla.
 */
@Component({
  selector: 'app-reportes-finanzas',
  standalone: true,
  imports: [FormsModule, BotonExportarComponent, DatePipe, DecimalPipe],
  templateUrl: './reportes-finanzas.component.html'
})
export class ReportesFinanzasComponent {

  private reporteService = inject(ReporteFinancieroService);
  private toastService = inject(ToastService);

  readonly filtro = signal<FiltroReporteFinanciero>({});
  readonly reporte = signal<RespuestaReporteComisiones | null>(null);
  readonly isLoading = signal(false);
  readonly exportando = signal(false);
  readonly error = signal('');

  actualizarFiltro<K extends keyof FiltroReporteFinanciero>(campo: K, valor: FiltroReporteFinanciero[K]): void {
    this.filtro.update(f => ({ ...f, [campo]: valor }));
  }

  consultar(): void {
    if (!this.filtro().idPerfil) {
      this.error.set('Indica el id de perfil del creador para generar el reporte.');
      return;
    }

    this.error.set('');
    this.isLoading.set(true);
    this.reporteService.obtener(this.filtro()).subscribe({
      next: (reporte) => {
        this.reporte.set(reporte);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.reporte.set(null);
        this.isLoading.set(false);
        this.error.set(err?.error?.detail || err?.error?.message || 'No se pudo generar el reporte financiero.');
      }
    });
  }

  limpiar(): void {
    this.filtro.set({});
    this.reporte.set(null);
    this.error.set('');
  }

  exportar(formato: FormatoReporte): void {
    if (!this.filtro().idPerfil) {
      this.error.set('Indica el id de perfil del creador antes de exportar.');
      return;
    }

    this.exportando.set(true);
    this.reporteService.exportar(this.filtro(), formato).subscribe({
      next: (respuesta) => {
        this.exportando.set(false);
        descargarRespuesta(respuesta, `comisiones_${this.filtro().idPerfil}.${formato.toLowerCase()}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        const mensaje = await mensajeErrorBlob(err, 'No se pudo exportar el reporte financiero');
        this.toastService.error(mensaje);
      }
    });
  }
}
