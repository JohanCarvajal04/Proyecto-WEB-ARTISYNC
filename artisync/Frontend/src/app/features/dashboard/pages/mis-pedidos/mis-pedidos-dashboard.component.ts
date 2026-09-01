import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PedidoService } from '../../../pedido/services/pedido.service';
import { RespuestaPedidoResumido } from '../../../pedido/models/pedido.model';

import { AuthService } from '../../../seguridad/services/auth.service';
import { BotonExportarComponent } from '../../../../shared/components/boton-exportar/boton-exportar.component';
import { FormatoReporte } from '../../../../shared/models/formato-reporte.model';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';
import { MonedaPipe } from '../../../../shared/pipes/moneda.pipe';

type FiltroEstado = 'todos' | 'activos' | 'completados';

@Component({
  selector: 'app-mis-pedidos-dashboard',
  standalone: true,
  imports: [RouterLink, BotonExportarComponent, MonedaPipe],
  templateUrl: './mis-pedidos-dashboard.component.html'
})
export class MisPedidosDashboardComponent implements OnInit {
  private pedidoService = inject(PedidoService);
  private authService = inject(AuthService);

  readonly pedidos = signal<RespuestaPedidoResumido[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');
  readonly exportando = signal<boolean>(false);

  readonly filtroEstado = signal<FiltroEstado>('todos');
  readonly filtroEtapa = signal<string>('');
  readonly busqueda = signal<string>('');

  etapasDisponibles = computed(() => {
    const etapas = this.pedidos()
      .map(p => p.etapaActual)
      .filter((e): e is string => !!e);
    return Array.from(new Set(etapas)).sort();
  });

  pedidosFiltrados = computed(() => {
    const estado = this.filtroEstado();
    const etapa = this.filtroEtapa();
    const texto = this.busqueda().trim().toLowerCase();

    return this.pedidos().filter(p => {
      if (estado === 'activos' && !this.esActivo(p.etapaActual)) return false;
      if (estado === 'completados' && this.esActivo(p.etapaActual)) return false;
      if (etapa && p.etapaActual !== etapa) return false;
      if (texto) {
        const blob = `${p.tituloServicio || ''} ${p.nombreCreador || ''}`.toLowerCase();
        if (!blob.includes(texto)) return false;
      }
      return true;
    }).sort((a, b) => new Date(b.fechaInicio).getTime() - new Date(a.fechaInicio).getTime());
  });

  hayFiltrosActivos = computed(() =>
    this.filtroEstado() !== 'todos' || this.filtroEtapa() !== '' || this.busqueda() !== ''
  );

  ngOnInit(): void {
    this.loadPedidos();
  }

  loadPedidos(): void {
    this.isLoading.set(true);
    this.error.set('');
    const role = this.authService.primaryRole();
    const req$ = role === 'CREADOR'
      ? this.pedidoService.listarMisComisiones()
      : this.pedidoService.listarMisPedidos();

    req$.subscribe({
      next: (data) => {
        this.pedidos.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Error al cargar tus pedidos');
        this.isLoading.set(false);
      }
    });
  }

  /** Mismo criterio de rol que loadPedidos(): exporta lo que la pantalla está mostrando. */
  exportar(formato: FormatoReporte): void {
    this.exportando.set(true);
    const role = this.authService.primaryRole();
    const req$ = role === 'CREADOR'
      ? this.pedidoService.exportarMisComisiones(formato)
      : this.pedidoService.exportarMisPedidos(formato);

    req$.subscribe({
      next: (respuesta) => {
        this.exportando.set(false);
        descargarRespuesta(respuesta, `${role === 'CREADOR' ? 'comisiones' : 'pedidos'}.${formato.toLowerCase()}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        this.error.set(await mensajeErrorBlob(err, 'No se pudo exportar el listado'));
      }
    });
  }

  setFiltroEstado(estado: FiltroEstado): void {
    this.filtroEstado.set(estado);
  }

  onEtapaChange(event: Event): void {
    this.filtroEtapa.set((event.target as HTMLSelectElement).value);
  }

  onBusquedaChange(event: Event): void {
    this.busqueda.set((event.target as HTMLInputElement).value);
  }

  limpiarFiltros(): void {
    this.filtroEstado.set('todos');
    this.filtroEtapa.set('');
    this.busqueda.set('');
  }

  esActivo(etapa: string): boolean {
    const lower = (etapa || '').toLowerCase();
    return !lower.includes('completado') && !lower.includes('entregado')
      && !lower.includes('cancelado') && !lower.includes('final');
  }


  formatDate(date: string): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  getBadgeClasses(etapa: string): string {
    if (!etapa) return 'bg-slate-100 text-slate-600';
    const lower = etapa.toLowerCase();
    if (lower.includes('completado') || lower.includes('entregado') || lower.includes('final')) return 'bg-emerald-50 text-emerald-700';
    if (lower.includes('revision') || lower.includes('pendiente')) return 'bg-amber-50 text-amber-700';
    if (lower.includes('cancelado') || lower.includes('rechazado')) return 'bg-rose-50 text-rose-700';
    return 'bg-sky-50 text-sky-700';
  }
}
