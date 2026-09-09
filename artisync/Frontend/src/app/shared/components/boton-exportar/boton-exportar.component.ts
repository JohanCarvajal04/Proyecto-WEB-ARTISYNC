import { Component, computed, input, output, signal } from '@angular/core';
import { CommonModule, NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FORMATOS_REPORTE, FormatoReporte, OpcionesExportacion, TOPES_FORMATO } from '../../models/formato-reporte.model';
import { HasPermissionDirective } from '../../directives/has-permission.directive';

export interface ParteExportacion {
  numero: number;
  etiqueta: string;
  desde: number;
  hasta: number;
}

/**
 * Botón "Exportar" con desplegable de formato (CSV / Excel / PDF) y soporte
 * automático de paginación / división en partes para grandes volúmenes de datos.
 */
@Component({
  selector: 'app-boton-exportar',
  standalone: true,
  imports: [CommonModule, FormsModule, HasPermissionDirective, NgTemplateOutlet],
  templateUrl: './boton-exportar.component.html'
})
export class BotonExportarComponent {
  readonly permiso = input<string | readonly string[] | null>(null);
  readonly cargando = input<boolean>(false);
  readonly deshabilitado = input<boolean>(false);
  readonly etiqueta = input<string>('Exportar');

  /** Total de registros del reporte actual */
  readonly totalElementos = input<number | null>(null);

  /** Índice de la página actualmente visible en la tabla (0-indexed) */
  readonly paginaActual = input<number | null>(null);

  /** Cantidad de registros en la página visible de la tabla */
  readonly tamanoPagina = input<number | null>(null);

  readonly exportar = output<FormatoReporte | OpcionesExportacion>();
  readonly exportarPaginado = output<OpcionesExportacion>();

  readonly formatos = FORMATOS_REPORTE;
  readonly topes = TOPES_FORMATO;
  readonly abierto = signal(false);

  // Estado del modal de paginación
  readonly modalAbierto = signal(false);
  readonly formatoSeleccionado = signal<FormatoReporte>('PDF');
  readonly parteSeleccionada = signal<number>(0);

  readonly topeActual = computed(() => this.topes[this.formatoSeleccionado()]);

  readonly partesDisponibles = computed<ParteExportacion[]>(() => {
    const total = this.totalElementos() ?? 0;
    const tope = this.topeActual();
    if (total <= 0) {
      return [{ numero: 0, etiqueta: 'Parte 1 (Registros disponibles)', desde: 1, hasta: 0 }];
    }
    const numPartes = Math.max(1, Math.ceil(total / tope));
    const partes: ParteExportacion[] = [];
    for (let i = 0; i < numPartes; i++) {
      const desde = i * tope + 1;
      const hasta = Math.min((i + 1) * tope, total);
      partes.push({
        numero: i,
        etiqueta: `Parte ${i + 1}: registros ${desde.toLocaleString()} al ${hasta.toLocaleString()} (${(hasta - desde + 1).toLocaleString()} registros)`,
        desde,
        hasta
      });
    }
    return partes;
  });

  alternar(): void {
    if (this.cargando() || this.deshabilitado()) return;
    this.abierto.update(v => !v);
  }

  elegir(formato: FormatoReporte): void {
    this.abierto.set(false);
    const total = this.totalElementos();
    const tope = this.topes[formato];

    if (total != null && total > tope) {
      this.abrirModal(formato);
      return;
    }

    this.exportar.emit(formato);
  }

  abrirModal(formato: FormatoReporte = 'PDF'): void {
    this.abierto.set(false);
    this.formatoSeleccionado.set(formato);
    this.parteSeleccionada.set(0);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  cambiarFormatoModal(formato: FormatoReporte): void {
    this.formatoSeleccionado.set(formato);
    this.parteSeleccionada.set(0);
  }

  descargarParte(): void {
    const opcion: OpcionesExportacion = {
      formato: this.formatoSeleccionado(),
      page: this.parteSeleccionada(),
      size: this.topeActual()
    };
    this.cerrarModal();
    this.exportar.emit(opcion);
    this.exportarPaginado.emit(opcion);
  }

  descargarPaginaActual(): void {
    const pag = this.paginaActual() ?? 0;
    const tam = this.tamanoPagina() ?? 20;
    const opcion: OpcionesExportacion = {
      formato: this.formatoSeleccionado(),
      page: pag,
      size: tam
    };
    this.cerrarModal();
    this.exportar.emit(opcion);
    this.exportarPaginado.emit(opcion);
  }

  descargarTodas(): void {
    const opcion: OpcionesExportacion = {
      formato: this.formatoSeleccionado(),
      page: 0,
      size: this.topeActual(),
      todasLasPartes: true
    };
    this.cerrarModal();
    this.exportar.emit(opcion);
    this.exportarPaginado.emit(opcion);
  }

  cerrar(): void {
    this.abierto.set(false);
  }
}
