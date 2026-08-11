import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ModeracionService } from '../../services/moderacion.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { VerificacionCola, Portafolio, Categoria } from '../../models/moderacion.model';

@Component({
  selector: 'app-mod-overview',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './mod-overview.component.html'
})
export class ModOverviewComponent implements OnInit {
  private modService = inject(ModeracionService);
  private authService = inject(AuthService);

  readonly verificaciones = signal<VerificacionCola[]>([]);
  readonly portafolios = signal<Portafolio[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly isLoading = signal<boolean>(true);

  userName = computed(() => {
    const user = this.authService.currentUser();
    const email = user?.email || user?.sub || 'Moderador';
    const prefix = email.split('@')[0];
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });

  pendientes = computed(() =>
    this.verificaciones().filter(v => v.nombreEstado?.toUpperCase().includes('PENDIENTE')).length
  );

  aprobadas = computed(() =>
    this.verificaciones().filter(v => v.nombreEstado?.toUpperCase().includes('APROBAD')).length
  );

  rechazadas = computed(() =>
    this.verificaciones().filter(v => v.nombreEstado?.toUpperCase().includes('RECHAZAD')).length
  );

  totalPortafolios = computed(() => this.portafolios().length);

  totalCategorias = computed(() => this.categorias().length);

  verificacionesRecientes = computed(() =>
    this.verificaciones().slice(0, 5)
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    this.modService.listarColaVerificaciones(undefined, 50, 0).subscribe({
      next: (data) => this.verificaciones.set(data),
      error: () => {}
    });

    this.modService.listarPortafolios().subscribe({
      next: (data) => this.portafolios.set(data),
      error: () => {}
    });

    this.modService.listarCategorias().subscribe({
      next: (data) => {
        this.categorias.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  formatDate(date: string | null): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  getConfianzaColor(score: number | null): string {
    if (score === null) return 'text-slate-400';
    if (score >= 0.8) return 'text-emerald-600';
    if (score >= 0.5) return 'text-amber-600';
    return 'text-rose-600';
  }

  getEstadoBadge(estado: string): string {
    if (!estado) return 'bg-slate-100 text-slate-600';
    const lower = estado.toLowerCase();
    if (lower.includes('aprobad')) return 'bg-emerald-50 text-emerald-700';
    if (lower.includes('pendiente')) return 'bg-amber-50 text-amber-700';
    if (lower.includes('rechazad')) return 'bg-rose-50 text-rose-700';
    return 'bg-sky-50 text-sky-700';
  }
}
