import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PedidoService } from '../../../pedido/services/pedido.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { RespuestaPedidoResumido } from '../../../pedido/models/pedido.model';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit {
  private pedidoService = inject(PedidoService);
  private authService = inject(AuthService);

  readonly pedidos = signal<RespuestaPedidoResumido[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  userName = computed(() => {
    const user = this.authService.currentUser();
    const email = user?.email || user?.sub || 'Usuario';
    const prefix = email.split('@')[0];
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });

  pedidosActivos = computed(() => {
    return this.pedidos().filter(p => {
      const etapa = (p.etapaActual || '').toLowerCase();
      return !etapa.includes('completado') && !etapa.includes('entregado') && !etapa.includes('cancelado') && !etapa.includes('final');
    }).length;
  });

  pedidosCompletados = computed(() => {
    return this.pedidos().filter(p => {
      const etapa = (p.etapaActual || '').toLowerCase();
      return etapa.includes('completado') || etapa.includes('entregado') || etapa.includes('final');
    }).length;
  });

  totalGastado = computed(() => {
    return this.pedidos().reduce((sum, p) => sum + (p.precioPactado || 0), 0);
  });

  pedidosRecientes = computed(() => {
    return this.pedidos()
      .slice()
      .sort((a, b) => new Date(b.fechaInicio).getTime() - new Date(a.fechaInicio).getTime())
      .slice(0, 5);
  });

  ngOnInit(): void {
    this.loadPedidos();
  }

  loadPedidos(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.pedidoService.listarMisPedidos().subscribe({
      next: (data) => {
        this.pedidos.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Error al cargar datos');
        this.isLoading.set(false);
      }
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price);
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
