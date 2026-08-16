import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ModeracionService } from '../../services/moderacion.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Categoria, CrearCategoria, ActualizarCategoria, Subcategoria, Etiqueta } from '../../models/moderacion.model';
import { AuthService } from '../../../seguridad/services/auth.service';
import { FlujoTrabajoService } from '../../../pedido/services/flujo-trabajo.service';
import { RespuestaFlujoTrabajo } from '../../../pedido/models/pedido.model';

@Component({
  selector: 'app-mod-categorias',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './mod-categorias.component.html'
})
export class ModCategoriasComponent implements OnInit {
  private modService = inject(ModeracionService);
  private toastService = inject(ToastService);
  private authService = inject(AuthService);
  private flujoService = inject(FlujoTrabajoService);

  readonly categorias = signal<Categoria[]>([]);
  readonly isLoading = signal<boolean>(true);

  /**
   * El alta y baja de subcategorías y el borrado de etiquetas son
   * `hasRole('ADMIN')` en el backend; el MODERADOR llega aquí con
   * CATEGORIA_GESTIONAR pero recibiría 403, así que no se le ofrecen.
   */
  readonly esAdmin = this.authService.hasAnyRole('ADMIN', 'ADMINISTRADOR');

  // Subcategorías
  readonly subcategorias = signal<Subcategoria[]>([]);
  nuevaSubcategoria = { idCategoria: 0, nombreSubcategoria: '' };
  readonly guardandoSub = signal<boolean>(false);

  // Etiquetas
  readonly etiquetas = signal<Etiqueta[]>([]);
  readonly borrandoEtiqueta = signal<number | null>(null);

  // Form State
  isFormOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  editingId = signal<number | null>(null);
  
  formData = {
    nombreCategoria: '',
    estadoActiva: true,
    idFlujo: null as number | null
  };

  /** Flujos disponibles para asignar a una categoría (RF-19). */
  readonly flujos = signal<RespuestaFlujoTrabajo[]>([]);

  ngOnInit(): void {
    this.loadCategorias();
    this.loadFlujos();
    if (this.esAdmin) {
      this.loadSubcategorias();
      this.loadEtiquetas();
    }
  }

  loadFlujos(): void {
    this.flujoService.listarFlujos().subscribe({
      next: (data) => this.flujos.set(data),
      error: () => this.flujos.set([])
    });
  }

  // ── Subcategorías ────────────────────────────────────────────────────────

  loadSubcategorias(): void {
    this.modService.listarSubcategorias().subscribe({
      next: (data) => this.subcategorias.set(data),
      error: () => this.subcategorias.set([])
    });
  }

  crearSubcategoria(): void {
    const nombre = this.nuevaSubcategoria.nombreSubcategoria.trim();
    if (!this.nuevaSubcategoria.idCategoria || !nombre) {
      this.toastService.error('Elige una categoría e indica el nombre');
      return;
    }

    this.guardandoSub.set(true);
    this.modService.crearSubcategoria({
      idCategoria: Number(this.nuevaSubcategoria.idCategoria),
      nombreSubcategoria: nombre
    }).subscribe({
      next: () => {
        this.guardandoSub.set(false);
        this.nuevaSubcategoria = { idCategoria: 0, nombreSubcategoria: '' };
        this.toastService.success('Subcategoría creada');
        this.loadSubcategorias();
      },
      error: (err) => {
        this.guardandoSub.set(false);
        this.toastService.error(err.error?.message || 'No se pudo crear la subcategoría');
      }
    });
  }

  eliminarSubcategoria(sub: Subcategoria): void {
    if (!confirm(`¿Eliminar la subcategoría «${sub.nombreSubcategoria}»?`)) return;

    this.modService.eliminarSubcategoria(sub.idSubcategoria).subscribe({
      next: () => {
        this.toastService.success('Subcategoría eliminada');
        this.loadSubcategorias();
      },
      error: (err) => this.toastService.error(err.error?.message || 'No se pudo eliminar la subcategoría')
    });
  }

  // ── Etiquetas ────────────────────────────────────────────────────────────

  loadEtiquetas(): void {
    this.modService.listarEtiquetas().subscribe({
      next: (data) => this.etiquetas.set(data),
      error: () => this.etiquetas.set([])
    });
  }

  eliminarEtiqueta(etiqueta: Etiqueta): void {
    if (!confirm(`¿Eliminar la etiqueta «${etiqueta.nombreEtiqueta}»? Dejará de estar disponible para los creadores.`)) return;

    this.borrandoEtiqueta.set(etiqueta.idEtiqueta);
    this.modService.eliminarEtiqueta(etiqueta.idEtiqueta).subscribe({
      next: () => {
        this.borrandoEtiqueta.set(null);
        this.toastService.success('Etiqueta eliminada');
        this.loadEtiquetas();
      },
      error: (err) => {
        this.borrandoEtiqueta.set(null);
        this.toastService.error(err.error?.message || 'No se pudo eliminar la etiqueta');
      }
    });
  }

  loadCategorias(): void {
    this.isLoading.set(true);
    // `todas` y no `listarCategorias`: el panel debe seguir viendo las
    // desactivadas para poder reactivarlas.
    this.modService.listarTodasLasCategorias().subscribe({
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
    this.formData = { nombreCategoria: '', estadoActiva: true, idFlujo: null };
    this.isFormOpen.set(true);
  }

  openEditForm(categoria: Categoria): void {
    this.editingId.set(categoria.idCategoria);
    this.formData = {
      nombreCategoria: categoria.nombreCategoria,
      estadoActiva: categoria.estadoActiva,
      idFlujo: categoria.idFlujo
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
      const payload: CrearCategoria = {
        nombreCategoria: this.formData.nombreCategoria.trim(),
        estadoActiva: this.formData.estadoActiva,
        idFlujo: this.formData.idFlujo
      };
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
    // El nombre viaja aunque no cambie: PeticionActualizarCategoria lo valida
    // con @NotBlank, así que mandar solo `estadoActiva` devolvía 400 y el
    // toggle nunca llegó a funcionar.
    const payload: ActualizarCategoria = {
      nombreCategoria: categoria.nombreCategoria,
      estadoActiva: !categoria.estadoActiva
    };
    this.modService.actualizarCategoria(categoria.idCategoria, payload).subscribe({
      next: () => {
        this.toastService.success(`Categoría ${payload.estadoActiva ? 'activada' : 'desactivada'}`);
        this.loadCategorias();
      },
      error: () => this.toastService.error('Error al cambiar estado')
    });
  }
}
