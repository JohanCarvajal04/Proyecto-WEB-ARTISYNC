import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ModeracionService } from '../../services/moderacion.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Categoria, CrearCategoria, ActualizarCategoria } from '../../models/moderacion.model';

@Component({
  selector: 'app-mod-categorias',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './mod-categorias.component.html'
})
export class ModCategoriasComponent implements OnInit {
  private modService = inject(ModeracionService);
  private toastService = inject(ToastService);

  readonly categorias = signal<Categoria[]>([]);
  readonly isLoading = signal<boolean>(true);

  // Form State
  isFormOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  editingId = signal<number | null>(null);
  
  formData = {
    nombreCategoria: '',
    estadoActiva: true
  };

  ngOnInit(): void {
    this.loadCategorias();
  }

  loadCategorias(): void {
    this.isLoading.set(true);
    this.modService.listarCategorias().subscribe({
      next: (data) => {
        this.categorias.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('Error al cargar categorías');
        this.isLoading.set(false);
      }
    });
  }

  openNewForm(): void {
    this.editingId.set(null);
    this.formData = { nombreCategoria: '', estadoActiva: true };
    this.isFormOpen.set(true);
  }

  openEditForm(categoria: Categoria): void {
    this.editingId.set(categoria.idCategoria);
    this.formData = {
      nombreCategoria: categoria.nombreCategoria,
      estadoActiva: categoria.estadoActiva
    };
    this.isFormOpen.set(true);
  }

  closeForm(): void {
    this.isFormOpen.set(false);
  }

  submitForm(): void {
    if (!this.formData.nombreCategoria.trim()) {
      this.toastService.error('El nombre es obligatorio');
      return;
    }

    this.isSubmitting.set(true);
    const id = this.editingId();

    if (id) {
      const payload: ActualizarCategoria = { ...this.formData };
      this.modService.actualizarCategoria(id, payload).subscribe({
        next: () => {
          this.toastService.success('Categoría actualizada');
          this.isSubmitting.set(false);
          this.closeForm();
          this.loadCategorias();
        },
        error: () => {
          this.toastService.error('Error al actualizar');
          this.isSubmitting.set(false);
        }
      });
    } else {
      const payload: CrearCategoria = { nombreCategoria: this.formData.nombreCategoria };
      this.modService.crearCategoria(payload).subscribe({
        next: () => {
          this.toastService.success('Categoría creada');
          this.isSubmitting.set(false);
          this.closeForm();
          this.loadCategorias();
        },
        error: () => {
          this.toastService.error('Error al crear');
          this.isSubmitting.set(false);
        }
      });
    }
  }

  toggleEstado(categoria: Categoria): void {
    const payload: ActualizarCategoria = { estadoActiva: !categoria.estadoActiva };
    this.modService.actualizarCategoria(categoria.idCategoria, payload).subscribe({
      next: () => {
        this.toastService.success(`Categoría ${payload.estadoActiva ? 'activada' : 'desactivada'}`);
        this.loadCategorias();
      },
      error: () => this.toastService.error('Error al cambiar estado')
    });
  }
}
