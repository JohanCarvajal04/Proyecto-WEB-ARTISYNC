import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ReporteContratoService } from '../../services/reporte-contrato.service';
import { FilaReporteContrato, FiltroReporteContrato } from '../../models/reporte-contrato.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';
import { BotonExportarComponent } from '../../../../shared/components/boton-exportar/boton-exportar.component';
import { FormatoReporte, OpcionesExportacion } from '../../../../shared/models/formato-reporte.model';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';
import { rangoFechasInvertido } from '../../../../shared/utils/rango-fechas';

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
    if (rangoFechasInvertido(this.filtro())) {
      this.toastService.error('La fecha "Desde" no puede ser posterior a "Hasta".');
      return;
    }
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

  exportar(opcion: FormatoReporte | OpcionesExportacion): void {
    if (rangoFechasInvertido(this.filtro())) {
      this.toastService.error('La fecha "Desde" no puede ser posterior a "Hasta".');
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
        descargarRespuesta(respuesta, `contratos${sufijo}_${new Date().toISOString().slice(0, 10)}.${formato.toLowerCase()}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        const mensaje = await mensajeErrorBlob(err, 'No se pudo exportar el reporte de contratos');
        this.toastService.error(mensaje);
      }
    });
  }

  private descargarTodasLasPartes(formato: FormatoReporte, tamanoLote?: number): void {
    const total = this.pagina().totalElementos;
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
          descargarRespuesta(respuesta, `contratos_parte_${p + 1}_${new Date().toISOString().slice(0, 10)}.${formato.toLowerCase()}`);
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
