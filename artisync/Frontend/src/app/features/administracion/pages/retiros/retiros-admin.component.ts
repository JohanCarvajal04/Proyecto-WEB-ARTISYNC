import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SolicitudRetiroService } from '../../services/solicitud-retiro.service';
import { FiltroSolicitudRetiro, SolicitudRetiro } from '../../models/solicitud-retiro.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';
import { MonedaPipe } from '../../../../shared/pipes/moneda.pipe';
import { rangoFechasInvertido } from '../../../../shared/utils/rango-fechas';

/**
 * Cola de revisión de retiros del Auditor Financiero (RETIROS_GESTIONAR),
 * espejo de SolicitudRetiroAdminControlador.java. Aprobar dispara el payout
 * real a PayPal en el backend; el estado que vuelve ya refleja el resultado
 * (Pagado/Aprobado/Fallido), no hace falta que esta pantalla lo infiera.
 */
@Component({
  selector: 'app-retiros-admin',
  standalone: true,
  imports: [FormsModule, MonedaPipe],
  templateUrl: './retiros-admin.component.html'
})
export class RetirosAdminComponent implements OnInit {

  private solicitudRetiroService = inject(SolicitudRetiroService);
  private toastService = inject(ToastService);

  readonly pagina = signal<Pagina<SolicitudRetiro>>(paginaVacia());
  readonly isLoading = signal<boolean>(true);
  readonly filtro = signal<FiltroSolicitudRetiro>({ estado: 'Pendiente' });

  readonly detalle = signal<SolicitudRetiro | null>(null);
  readonly isSubmitting = signal<boolean>(false);
  notaAdmin = '';

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.solicitudRetiroService.listar(this.filtro(), page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudieron cargar las solicitudes de retiro');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  actualizarFiltro<K extends keyof FiltroSolicitudRetiro>(campo: K, valor: FiltroSolicitudRetiro[K]): void {
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

  verDetalle(solicitud: SolicitudRetiro): void {
    this.notaAdmin = '';
    this.detalle.set(solicitud);
  }

  cerrarDetalle(): void {
    this.detalle.set(null);
  }

  aprobar(): void {
    const solicitud = this.detalle();
    if (!solicitud) return;

    this.isSubmitting.set(true);
    this.solicitudRetiroService.aprobar(solicitud.idSolicitud).subscribe({
      next: (actualizada) => {
        this.toastService.success(
          actualizada.estado === 'Pagado' ? 'Retiro aprobado y pagado exitosamente' : 'Retiro aprobado: el pago quedó en proceso');
        this.isSubmitting.set(false);
        this.cerrarDetalle();
        this.cargar(this.pagina().numero);
      },
      // El error.interceptor global ya muestra el detalle específico del
      // backend (p. ej. "La solicitud no está en estado Pendiente"): un toast
      // propio aquí solo lo eclipsaría con un mensaje genérico.
      error: () => this.isSubmitting.set(false)
    });
  }

  rechazar(): void {
    const solicitud = this.detalle();
    if (!solicitud) return;

    if (!this.notaAdmin.trim()) {
      this.toastService.error('Debes indicar un motivo para rechazar la solicitud');
      return;
    }

    this.isSubmitting.set(true);
    this.solicitudRetiroService.rechazar(solicitud.idSolicitud, this.notaAdmin.trim()).subscribe({
      next: () => {
        this.toastService.success('Solicitud rechazada');
        this.isSubmitting.set(false);
        this.cerrarDetalle();
        this.cargar(this.pagina().numero);
      },
      error: () => this.isSubmitting.set(false)
    });
  }

  reintentar(): void {
    const solicitud = this.detalle();
    if (!solicitud) return;

    this.isSubmitting.set(true);
    this.solicitudRetiroService.reintentar(solicitud.idSolicitud).subscribe({
      next: (actualizada) => {
        this.toastService.success(`Reintento ejecutado: el retiro quedó ${actualizada.estado.toLowerCase()}`);
        this.isSubmitting.set(false);
        this.cerrarDetalle();
        this.cargar(this.pagina().numero);
      },
      error: () => this.isSubmitting.set(false)
    });
  }

  formatFechaHora(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  badgeEstado(estado: string): string {
    switch (estado) {
      case 'Pagado': return 'bg-emerald-50 text-emerald-700';
      case 'Aprobado': return 'bg-sky-50 text-sky-700';
      case 'Pendiente': return 'bg-amber-50 text-amber-700';
      case 'Rechazado': return 'bg-slate-100 text-slate-600';
      case 'Fallido': return 'bg-rose-50 text-rose-700';
      default: return 'bg-slate-100 text-slate-600';
    }
  }
}
