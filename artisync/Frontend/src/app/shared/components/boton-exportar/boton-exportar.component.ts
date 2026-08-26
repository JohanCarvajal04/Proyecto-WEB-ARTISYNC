import { Component, input, output, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FORMATOS_REPORTE, FormatoReporte } from '../../models/formato-reporte.model';
import { HasPermissionDirective } from '../../directives/has-permission.directive';

/**
 * Botón "Exportar" con desplegable de formato (CSV / Excel / PDF), para las
 * pantallas que consumen el motor común de reportes (Backend:
 * service/shared/reporte). Reemplaza el botón de un solo formato duplicado en
 * auditoría y evita repetir el markup del desplegable en cada pantalla nueva
 * (finanzas, contratos, usuarios, pedidos).
 *
 * `permiso` es opcional: los reportes "propios" (mis pedidos, mis comisiones)
 * no llevan un permiso de exportación aparte, heredan el guard de la propia
 * pantalla — ver Fase 5.1 del plan de reportes.
 */
@Component({
  selector: 'app-boton-exportar',
  standalone: true,
  imports: [HasPermissionDirective, NgTemplateOutlet],
  templateUrl: './boton-exportar.component.html'
})
export class BotonExportarComponent {
  readonly permiso = input<string | readonly string[] | null>(null);
  readonly cargando = input<boolean>(false);
  readonly deshabilitado = input<boolean>(false);
  readonly etiqueta = input<string>('Exportar');

  readonly exportar = output<FormatoReporte>();

  readonly formatos = FORMATOS_REPORTE;
  readonly abierto = signal(false);

  alternar(): void {
    if (this.cargando() || this.deshabilitado()) return;
    this.abierto.update(v => !v);
  }

  elegir(formato: FormatoReporte): void {
    this.abierto.set(false);
    this.exportar.emit(formato);
  }

  cerrar(): void {
    this.abierto.set(false);
  }
}
