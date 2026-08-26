import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PedidoService } from '../../../pedido/services/pedido.service';
import { RespuestaPedidoResumido } from '../../../pedido/models/pedido.model';
import { formatPrice, formatDate, esEtapaActiva, badgeEtapa, mensajeError } from '../../utils/formato';
import { BotonExportarComponent } from '../../../../shared/components/boton-exportar/boton-exportar.component';
import { FormatoReporte } from '../../../../shared/models/formato-reporte.model';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';

type FiltroEstado = 'todos' | 'activos' | 'cerrados' | 'vencidos';

@Component({
  selector: 'app-comisiones',
  standalone: true,
  imports: [RouterLink, BotonExportarComponent],
  templateUrl: './comisiones.component.html',
  styleUrl: './comisiones.component.css'
})
export class ComisionesComponent implements OnInit {

  private pedidoService = inject(PedidoService);

  readonly comisiones = signal<RespuestaPedidoResumido[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  readonly filtro = signal<FiltroEstado>('todos');
  readonly filtroEtapa = signal<string>('');
  readonly busqueda = signal<string>('');

  readonly filtros: { valor: FiltroEstado; etiqueta: string }[] = [
    { valor: 'todos', etiqueta: 'Todas' },
    { valor: 'activos', etiqueta: 'En curso' },
    { valor: 'cerrados', etiqueta: 'Cerradas' },
    { valor: 'vencidos', etiqueta: 'Con retraso' }
  ];

  etapasDisponibles = computed(() =>
    Array.from(new Set(this.comisiones().map(c => c.etapaActual).filter((e): e is string => !!e))).sort()
  );

  comisionesFiltradas = computed(() => {
    const estado = this.filtro();
    const etapa = this.filtroEtapa();
    const texto = this.busqueda().trim().toLowerCase();
    const hoy = Date.now();

    return this.comisiones().filter(c => {
      const activa = esEtapaActiva(c.etapaActual);
      if (estado === 'activos' && !activa) return false;
      if (estado === 'cerrados' && activa) return false;
      if (estado === 'vencidos') {
        const vencida = activa && !!c.fechaEntregaEstimada && new Date(c.fechaEntregaEstimada).getTime() < hoy;
        if (!vencida) return false;
      }
      if (etapa && c.etapaActual !== etapa) return false;
      if (texto) {
        const blob = `${c.tituloServicio || ''} ${c.nombreCliente || ''}`.toLowerCase();
        if (!blob.includes(texto)) return false;
      }
      return true;
    }).sort((a, b) => new Date(b.fechaInicio).getTime() - new Date(a.fechaInicio).getTime());
  });

  totalFiltrado = computed(() =>
    this.comisionesFiltradas().reduce((suma, c) => suma + (c.precioPactado || 0), 0)
  );

  hayFiltros = computed(() => this.filtro() !== 'todos' || this.filtroEtapa() !== '' || this.busqueda() !== '');

  readonly exportando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  exportar(formato: FormatoReporte): void {
    this.exportando.set(true);
    this.pedidoService.exportarMisComisiones(formato).subscribe({
      next: (respuesta) => {
        this.exportando.set(false);
        descargarRespuesta(respuesta, `comisiones.${formato.toLowerCase()}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        this.error.set(await mensajeErrorBlob(err, 'No se pudo exportar tus comisiones'));
      }
    });
  }

  cargar(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.pedidoService.listarMisComisiones().subscribe({
      next: (data) => {
        this.comisiones.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'Error al cargar tus comisiones'));
        this.isLoading.set(false);
      }
    });
  }

  setFiltro(valor: FiltroEstado): void {
    this.filtro.set(valor);
  }

  onEtapa(event: Event): void {
    this.filtroEtapa.set((event.target as HTMLSelectElement).value);
  }

  onBusqueda(event: Event): void {
    this.busqueda.set((event.target as HTMLInputElement).value);
  }

  limpiarFiltros(): void {
    this.filtro.set('todos');
    this.filtroEtapa.set('');
    this.busqueda.set('');
  }

  /** Marca las comisiones abiertas cuya fecha estimada de entrega ya pasó. */
  estaVencida(comision: RespuestaPedidoResumido): boolean {
    return esEtapaActiva(comision.etapaActual)
      && !!comision.fechaEntregaEstimada
      && new Date(comision.fechaEntregaEstimada).getTime() < Date.now();
  }

  formatPrice = formatPrice;
  formatDate = formatDate;
  badgeEtapa = badgeEtapa;
}
