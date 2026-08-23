import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { PedidoService } from '../../services/pedido.service';
import { RespuestaPedidoResumido } from '../../models/pedido.model';

@Component({
  selector: 'app-pedidos-lista',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pedidos-lista.component.html'
})
export class PedidosListaComponent implements OnInit {
  pedidos: RespuestaPedidoResumido[] = [];
  loading = true;
  error = '';
  modo: 'cliente' | 'creador' = 'cliente';

  constructor(
    private pedidoService: PedidoService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.modo = data['modo'] || 'cliente';
      this.cargarPedidos();
    });
  }

  cargarPedidos(): void {
    this.loading = true;
    this.error = '';

    const obs$ = this.modo === 'creador'
      ? this.pedidoService.listarMisComisiones()
      : this.pedidoService.listarMisPedidos();

    obs$.subscribe({
      next: (data) => {
        this.pedidos = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al cargar pedidos';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get titulo(): string {
    return this.modo === 'creador' ? 'Mis Comisiones' : 'Mis Pedidos';
  }

  get subtitulo(): string {
    return this.modo === 'creador'
      ? 'Pedidos asignados a ti como creador'
      : 'Servicios que has solicitado';
  }

  getBadgeClass(etapa: string): string {
    if (!etapa) return 'bg-slate-100 text-slate-600';
    const lower = etapa.toLowerCase();
    if (lower.includes('completado') || lower.includes('entregado') || lower.includes('final')) return 'bg-emerald-50 text-emerald-700';
    if (lower.includes('revision') || lower.includes('pendiente')) return 'bg-amber-50 text-amber-700';
    if (lower.includes('cancelado') || lower.includes('rechazado')) return 'bg-rose-50 text-rose-700';
    return 'bg-sky-50 text-sky-700';
  }

  formatDate(date: string): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price);
  }
}
