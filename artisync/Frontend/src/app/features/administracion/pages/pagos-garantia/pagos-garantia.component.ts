import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PagoGarantiaService } from '../../services/pago-garantia.service';
import { FiltroPagoGarantia, PagoGarantia, PagoGarantiaDetalle, ResumenEscrow } from '../../models/pago-garantia.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';
import { MonedaPipe } from '../../../../shared/pipes/moneda.pipe';
import { rangoFechasInvertido } from '../../../../shared/utils/rango-fechas';

@Component({
  selector: 'app-pagos-garantia',
  standalone: true,
  imports: [FormsModule, MonedaPipe],
  templateUrl: './pagos-garantia.component.html'
})
export class PagosGarantiaComponent implements OnInit {

  private pagoService = inject(PagoGarantiaService);
  private toastService = inject(ToastService);

  readonly pagina = signal<Pagina<PagoGarantia>>(paginaVacia());
  readonly resumen = signal<ResumenEscrow[]>([]);
  readonly isLoading = signal<boolean>(true);

  readonly filtro = signal<FiltroPagoGarantia>({});

  readonly detalle = signal<PagoGarantiaDetalle | null>(null);
  readonly cargandoDetalle = signal<boolean>(false);

  ngOnInit(): void {
    this.cargarResumen();
    this.cargar(0);
  }

  cargarResumen(): void {
    this.pagoService.obtenerResumen().subscribe({
      next: (resumen) => this.resumen.set(resumen),
      error: () => this.resumen.set([])
    });
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.pagoService.listar(this.filtro(), page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudieron cargar los pagos en garantía');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  actualizarFiltro<K extends keyof FiltroPagoGarantia>(campo: K, valor: FiltroPagoGarantia[K]): void {
    this.filtro.update(f => ({ ...f, [campo]: valor || undefined }));
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

  verDetalle(pago: PagoGarantia): void {
    this.cargandoDetalle.set(true);
    this.detalle.set(null);
    this.pagoService.obtenerDetalle(pago.idPago).subscribe({
      next: (detalle) => {
        this.detalle.set(detalle);
        this.cargandoDetalle.set(false);
      },
      error: () => {
        this.toastService.error('No se pudo cargar el detalle del pago');
        this.cargandoDetalle.set(false);
      }
    });
  }

  cerrarDetalle(): void {
    this.detalle.set(null);
  }


  formatFecha(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatFechaHora(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  badgeEstado(estado: string): string {
    switch (estado) {
      case 'Liberado': return 'bg-emerald-50 text-emerald-700';
      case 'Retenido': return 'bg-amber-50 text-amber-700';
      case 'Pendiente': return 'bg-slate-100 text-slate-600';
      default: return 'bg-sky-50 text-sky-700';
    }
  }
}
