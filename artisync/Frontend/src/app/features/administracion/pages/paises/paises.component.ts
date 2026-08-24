import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PaisService } from '../../../../shared/services/pais.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { PaisResponse } from '../../../../shared/models/user.model';
import { PaisRequest } from '../../models/admin.model';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { PaisFormModalComponent } from '../../../../shared/components/pais-form-modal/pais-form-modal.component';

@Component({
  selector: 'app-paises',
  standalone: true,
  imports: [FormsModule, ConfirmDialogComponent, PaisFormModalComponent],
  templateUrl: './paises.component.html'
})
export class PaisesComponent implements OnInit {
  private paisService = inject(PaisService);
  private toastService = inject(ToastService);
  authService = inject(AuthService);

  readonly paises = signal<PaisResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isActionLoading = signal<boolean>(false);

  // Filtros
  searchTerm = '';

  // Modales
  readonly isFormModalOpen = signal<boolean>(false);
  readonly formModalMode = signal<'create' | 'edit'>('create');
  readonly selectedPais = signal<PaisResponse | null>(null);

  readonly isConfirmOpen = signal<boolean>(false);

  ngOnInit(): void {
    this.loadPaises();
  }

  loadPaises(): void {
    this.isLoading.set(true);
    this.paisService.getPaises().subscribe({
      next: (res) => {
        this.paises.set(res);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  filteredPaises = computed(() => {
    let list = this.paises();
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      list = list.filter(p => 
        p.nombrePais.toLowerCase().includes(term)
      );
    }
    return list;
  });

  openCreateModal(): void {
    this.formModalMode.set('create');
    this.selectedPais.set(null);
    this.isFormModalOpen.set(true);
  }

  openEditModal(pais: PaisResponse): void {
    this.formModalMode.set('edit');
    this.selectedPais.set(pais);
    this.isFormModalOpen.set(true);
  }

  handleCreate(request: PaisRequest): void {
    this.isActionLoading.set(true);
    this.paisService.createPais(request).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.isFormModalOpen.set(false);
        this.toastService.success('País creado exitosamente');
        this.loadPaises();
      },
      error: () => {
        this.isActionLoading.set(false);
      }
    });
  }

  handleEdit(request: PaisRequest): void {
    const pais = this.selectedPais();
    if (!pais) return;

    this.isActionLoading.set(true);
    this.paisService.updatePais(pais.idPais, request).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.isFormModalOpen.set(false);
        this.toastService.success('País actualizado exitosamente');
        this.loadPaises();
      },
      error: () => {
        this.isActionLoading.set(false);
      }
    });
  }

  confirmDelete(pais: PaisResponse): void {
    this.selectedPais.set(pais);
    this.isConfirmOpen.set(true);
  }

  executeConfirmAction(): void {
    const pais = this.selectedPais();
    if (!pais) return;

    this.isActionLoading.set(true);
    this.paisService.deletePais(pais.idPais).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.isConfirmOpen.set(false);
        const accionStr = pais.estado ? 'desactivado' : 'reactivado';
        this.toastService.success(`País ${accionStr} exitosamente`);
        this.loadPaises();
      },
      error: () => this.isActionLoading.set(false)
    });
  }
}
