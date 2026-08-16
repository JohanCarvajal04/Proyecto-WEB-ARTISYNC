import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ModeracionService } from '../../services/moderacion.service';
import { ToastService } from '../../../../core/services/toast.service';
import { VerificacionCola, VerificacionDetalle, DecisionVerificacion, CertificadoIa } from '../../models/moderacion.model';

@Component({
  selector: 'app-verificaciones',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './verificaciones.component.html'
})
export class VerificacionesComponent implements OnInit {
  private modService = inject(ModeracionService);
  private toastService = inject(ToastService);

  readonly items = signal<VerificacionCola[]>([]);
  readonly isLoading = signal<boolean>(false);
  
  // Detalle
  readonly selectedItem = signal<VerificacionDetalle | null>(null);
  readonly isDetailLoading = signal<boolean>(false);
  readonly documentUrl = signal<string | null>(null);
  /** Certificados previos del mismo perfil, como contexto para la decisión. */
  readonly historialPerfil = signal<CertificadoIa[]>([]);

  // Filtro
  selectedEstado = 'PENDIENTE';

  // Formulario Decisión
  notaModerador = '';
  isSubmitting = signal<boolean>(false);

  ngOnInit(): void {
    this.loadCola();
  }

  loadCola(): void {
    this.isLoading.set(true);
    const estado = this.selectedEstado === 'ALL' ? undefined : this.selectedEstado;
    this.modService.listarColaVerificaciones(estado, 100, 0).subscribe({
      next: (data) => {
        this.items.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  onFilterChange(): void {
    this.loadCola();
    this.closeDetail();
  }

  openDetail(id: number): void {
    this.isDetailLoading.set(true);
    this.selectedItem.set(null);
    this.documentUrl.set(null);
    this.notaModerador = '';

    this.historialPerfil.set([]);

    this.modService.obtenerVerificacion(id).subscribe({
      next: (detail) => {
        this.selectedItem.set(detail);
        this.loadDocument(id);
        this.loadHistorial(detail.idPerfil);
        this.isDetailLoading.set(false);
      },
      error: () => {
        this.toastService.error('Error al cargar detalle');
        this.isDetailLoading.set(false);
      }
    });
  }

  closeDetail(): void {
    this.selectedItem.set(null);
    if (this.documentUrl()) {
      URL.revokeObjectURL(this.documentUrl()!);
      this.documentUrl.set(null);
    }
  }

  /**
   * Certificados previos del mismo perfil. Es contexto, no bloquea la revisión:
   * un perfil sin historial responde vacío y no debe ensuciar la vista.
   */
  loadHistorial(idPerfil: number | null): void {
    if (idPerfil === null) return;
    this.modService.listarCertificadosDePerfil(idPerfil).subscribe({
      next: (certificados) => this.historialPerfil.set(certificados),
      error: () => this.historialPerfil.set([])
    });
  }

  loadDocument(id: number): void {
    this.modService.obtenerDocumento(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.documentUrl.set(url);
      },
      error: () => this.toastService.error('No se pudo cargar el documento')
    });
  }

  pedirAnalisisIa(): void {
    const item = this.selectedItem();
    if (!item) return;

    this.isDetailLoading.set(true);
    this.modService.analizarConIa(item.idCertificado).subscribe({
      next: (detail) => {
        this.selectedItem.set(detail);
        this.toastService.success('Análisis IA completado');
        this.isDetailLoading.set(false);
        this.loadCola(); // Refrescar lista principal
      },
      error: () => {
        this.toastService.error('Error en el análisis IA');
        this.isDetailLoading.set(false);
      }
    });
  }

  registrarDecision(aprobar: boolean): void {
    const item = this.selectedItem();
    if (!item) return;

    if (!aprobar && !this.notaModerador.trim()) {
      this.toastService.error('Debes incluir una nota si rechazas');
      return;
    }

    this.isSubmitting.set(true);
    const decision: DecisionVerificacion = {
      idEstadoVerificacion: aprobar ? 2 : 3, // 2: Aprobado, 3: Rechazado
      notaModerador: this.notaModerador.trim()
    };

    this.modService.registrarDecision(item.idCertificado, decision).subscribe({
      next: () => {
        this.toastService.success(`Verificación ${aprobar ? 'Aprobada' : 'Rechazada'} exitosamente`);
        this.isSubmitting.set(false);
        this.closeDetail();
        this.loadCola();
      },
      error: () => {
        this.toastService.error('Error al registrar decisión');
        this.isSubmitting.set(false);
      }
    });
  }

  formatDate(date: string | null): string {
    if (!date) return '—';
    return new Date(date).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
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
