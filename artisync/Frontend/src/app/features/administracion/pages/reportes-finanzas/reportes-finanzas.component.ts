import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ReporteFinancieroService } from '../../services/reporte-financiero.service';
import { FiltroReporteFinanciero, RespuestaReporteComisiones } from '../../models/reporte-financiero.model';
import { ToastService } from '../../../../core/services/toast.service';
import { BotonExportarComponent } from '../../../../shared/components/boton-exportar/boton-exportar.component';
import { FormatoReporte, OpcionesExportacion } from '../../../../shared/models/formato-reporte.model';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';
import { rangoFechasInvertido } from '../../../../shared/utils/rango-fechas';

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
    if (rangoFechasInvertido(this.filtro())) {
      this.error.set('La fecha "Desde" no puede ser posterior a "Hasta".');
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

  exportar(opcion: FormatoReporte | OpcionesExportacion): void {
    if (!this.filtro().idPerfil) {
      this.error.set('Indica el id de perfil del creador antes de exportar.');
      return;
    }
    if (rangoFechasInvertido(this.filtro())) {
      this.error.set('La fecha "Desde" no puede ser posterior a "Hasta".');
      return;
    }

    const formato: FormatoReporte = typeof opcion === 'string' ? opcion : opcion.formato;
    const page: number | undefined = typeof opcion === 'object' ? opcion.page : undefined;
    const size: number | undefined = typeof opcion === 'object' ? opcion.size : undefined;
    const todasLasPartes: boolean = typeof opcion === 'object' ? !!opcion.todasLasPartes : false;

    if (todasLasPartes) {
      this.descargarTodasLasPartes(formato, size);
      return;
    }

    this.exportando.set(true);
    this.reporteService.exportar(this.filtro(), formato, page, size).subscribe({
      next: (respuesta) => {
        this.exportando.set(false);
        const sufijo = page !== undefined ? `_parte_${page + 1}` : '';
        descargarRespuesta(respuesta, `comisiones_${this.filtro().idPerfil}${sufijo}.${formato.toLowerCase()}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        const mensaje = await mensajeErrorBlob(err, 'No se pudo exportar el reporte financiero');
        this.toastService.error(mensaje);
      }
    });
  }

  private descargarTodasLasPartes(formato: FormatoReporte, tamanoLote?: number): void {
    const total = this.reporte()?.detalle?.length ?? 0;
    const tope = tamanoLote ?? 5000;
    const totalPartes = Math.max(1, Math.ceil(total / tope));
    let parteActual = 0;

    this.exportando.set(true);
    this.toastService.info(`Iniciando descarga de ${totalPartes} partes (${formato})...`);

    const descargarSiguiente = () => {
      if (parteActual >= totalPartes) {
        this.exportando.set(false);
        this.toastService.success(`Descarga completada: ${totalPartes} partes descargadas con éxito.`);
        return;
      }

      const p = parteActual;
      this.reporteService.exportar(this.filtro(), formato, p, tope).subscribe({
        next: (respuesta) => {
          descargarRespuesta(respuesta, `comisiones_${this.filtro().idPerfil}_parte_${p + 1}.${formato.toLowerCase()}`);
          parteActual++;
          setTimeout(descargarSiguiente, 600);
        },
        error: async (err) => {
          this.exportando.set(false);
          const mensaje = await mensajeErrorBlob(err, `Error al descargar parte ${p + 1}`);
          this.toastService.error(mensaje);
        }
      });
    };

    descargarSiguiente();
  }
}
