import { Component, inject, signal, OnInit } from '@angular/core';
import { ModeracionService } from '../../services/moderacion.service';
import { Comentario } from '../../models/moderacion.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';

const ESTADO_ACTIVO = 'Activo';
const ESTADO_ELIMINADO = 'Eliminado';

@Component({
  selector: 'app-mod-comentarios',
  standalone: true,
  imports: [],
  templateUrl: './mod-comentarios.component.html'
})
export class ModComentariosComponent implements OnInit {

  private modService = inject(ModeracionService);
  private toastService = inject(ToastService);
  private authService = inject(AuthService);

  /** El borrado definitivo es ADMIN-only; el moderador solo oculta/reactiva. */
  readonly esAdmin = this.authService.hasAnyRole('ADMIN', 'ADMINISTRADOR');
  readonly estadoActivo = ESTADO_ACTIVO;
  readonly estadoEliminado = ESTADO_ELIMINADO;

  readonly pagina = signal<Pagina<Comentario>>(paginaVacia());
  readonly isLoading = signal<boolean>(true);
  readonly procesandoId = signal<number | null>(null);
  readonly aEliminar = signal<Comentario | null>(null);
  readonly eliminando = signal<boolean>(false);

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.modService.listarComentarios(page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudieron cargar los comentarios');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.cargar(numero);
  }

  toggleEstado(comentario: Comentario): void {
    if (this.procesandoId() !== null) return;
    this.procesandoId.set(comentario.idComentario);

    const accion$ = comentario.estadoModeracion === ESTADO_ACTIVO
      ? this.modService.ocultarComentario(comentario.idComentario)
      : this.modService.reactivarComentario(comentario.idComentario);

    accion$.subscribe({
      next: (actualizado) => {
        this.procesandoId.set(null);
        this.pagina.update(p => ({
          ...p,
          contenido: p.contenido.map(c => c.idComentario === actualizado.idComentario ? actualizado : c)
        }));
        this.toastService.success(actualizado.estadoModeracion === ESTADO_ACTIVO ? 'Comentario reactivado' : 'Comentario ocultado');
      },
      error: (err) => {
        this.procesandoId.set(null);
        this.toastService.error(err.error?.detail || err.error?.message || 'No se pudo actualizar el comentario');
      }
    });
  }

  confirmarEliminacion(comentario: Comentario): void {
    this.aEliminar.set(comentario);
  }

  cancelarEliminacion(): void {
    this.aEliminar.set(null);
  }

  eliminarComentario(): void {
    const comentario = this.aEliminar();
    if (!comentario) return;

    this.eliminando.set(true);
    this.modService.eliminarComentario(comentario.idComentario).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.aEliminar.set(null);
        this.toastService.success('Comentario eliminado');
        this.cargar(this.pagina().numero);
      },
      error: (err) => {
        this.eliminando.set(false);
        this.toastService.error(err.error?.detail || err.error?.message || 'No se pudo eliminar el comentario');
      }
    });
  }

  formatFecha(fecha: string): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  resumen(texto: string): string {
    if (!texto) return '—';
    return texto.length > 100 ? `${texto.slice(0, 100)}…` : texto;
  }
}
