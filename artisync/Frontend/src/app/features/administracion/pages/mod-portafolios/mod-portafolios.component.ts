import { Component, inject, signal, OnInit } from '@angular/core';
import { ModeracionService } from '../../services/moderacion.service';
import { Portafolio } from '../../models/moderacion.model';
import { PortafolioItem } from '../../../perfil/models/portafolio.model';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';

@Component({
  selector: 'app-mod-portafolios',
  standalone: true,
  templateUrl: './mod-portafolios.component.html'
})
export class ModPortafoliosComponent implements OnInit {
  private modService = inject(ModeracionService);
  private toastService = inject(ToastService);
  private authService = inject(AuthService);

  /** El borrado del portafolio es ADMIN-only; el moderador solo mira. */
  readonly esAdmin = this.authService.hasAnyRole('ADMIN', 'ADMINISTRADOR');
  readonly eliminando = signal<boolean>(false);

  readonly portafolios = signal<Portafolio[]>([]);
  readonly isLoading = signal<boolean>(true);

  /** Portafolio cuyo contenido está desplegado. */
  readonly seleccionado = signal<Portafolio | null>(null);
  readonly obras = signal<PortafolioItem[]>([]);
  readonly cargandoObras = signal<boolean>(false);

  ngOnInit(): void {
    this.loadPortafolios();
  }

  loadPortafolios(): void {
    this.isLoading.set(true);
    this.modService.listarPortafolios().subscribe({
      next: (data) => {
        this.portafolios.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  verObras(portafolio: Portafolio): void {
    this.seleccionado.set(portafolio);
    this.obras.set([]);
    this.cargandoObras.set(true);

    this.modService.listarObrasPortafolio(portafolio.idPortafolio).subscribe({
      next: (items) => {
        this.obras.set(items);
        this.cargandoObras.set(false);
      },
      error: () => {
        this.cargandoObras.set(false);
        this.toastService.error('No se pudieron cargar las obras del portafolio');
      }
    });
  }

  cerrarDetalle(): void {
    this.seleccionado.set(null);
    this.obras.set([]);
  }

  eliminarPortafolio(): void {
    const portafolio = this.seleccionado();
    if (!portafolio || this.eliminando()) return;

    if (!confirm(`¿Eliminar el portafolio #${portafolio.idPortafolio} y todas sus obras? Esta acción no se puede deshacer.`)) return;

    this.eliminando.set(true);
    this.modService.eliminarPortafolio(portafolio.idPortafolio).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.cerrarDetalle();
        this.toastService.success('Portafolio eliminado');
        this.loadPortafolios();
      },
      error: (err) => {
        this.eliminando.set(false);
        this.toastService.error(err.error?.message || 'No se pudo eliminar el portafolio');
      }
    });
  }

  formatDate(date: string | null): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }
}
