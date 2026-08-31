import { Component, Input, OnInit, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { ComentarioService } from '../../services/comentario.service';
import { RespuestaComentario } from '../../models/social.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

/** Tope declarado por @Size en PeticionCrearComentario. */
const MAX_TEXTO = 2000;

@Component({
  selector: 'app-comentarios-obra',
  standalone: true,
  imports: [],
  templateUrl: './comentarios-obra.component.html'
})
export class ComentariosObraComponent implements OnInit, OnChanges {

  @Input({ required: true }) idItemPortafolio!: number;

  private comentarioService = inject(ComentarioService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);

  readonly pagina = signal<Pagina<RespuestaComentario>>(paginaVacia());
  readonly cargando = signal<boolean>(false);
  readonly texto = signal<string>('');
  readonly enviando = signal<boolean>(false);
  readonly error = signal<string>('');
  readonly eliminandoId = signal<number | null>(null);

  readonly maxTexto = MAX_TEXTO;
  readonly puedeComentar = this.authService.isAuthenticated();
  readonly esAdmin = this.authService.hasAnyRole('ADMIN', 'ADMINISTRADOR');

  ngOnInit(): void {
    this.cargar(0);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['idItemPortafolio'] && !changes['idItemPortafolio'].firstChange) {
      this.cargar(0);
    }
  }

  cargar(page: number): void {
    if (!this.idItemPortafolio) return;
    this.cargando.set(true);
    this.comentarioService.listarComentarios(this.idItemPortafolio, page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.cargando.set(false);
      },
      error: () => {
        this.pagina.set(paginaVacia());
        this.cargando.set(false);
      }
    });
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.cargar(numero);
  }

  onTexto(evento: Event): void {
    this.texto.set((evento.target as HTMLTextAreaElement).value);
  }

  publicar(): void {
    const texto = this.texto().trim();
    if (!texto || this.enviando()) return;

    this.enviando.set(true);
    this.error.set('');

    this.comentarioService.crearComentario(this.idItemPortafolio, { textoComentario: texto }).subscribe({
      next: () => {
        this.texto.set('');
        this.enviando.set(false);
        this.toast.success('Comentario publicado');
        this.cargar(0);
      },
      error: (err) => {
        this.error.set(err.error?.detail || err.error?.message || 'No se pudo publicar el comentario');
        this.enviando.set(false);
      }
    });
  }

  esAutor(comentario: RespuestaComentario): boolean {
    return this.authService.getCurrentUserId() === comentario.idUsuarioAutor;
  }

  puedeEliminar(comentario: RespuestaComentario): boolean {
    return this.esAdmin || this.esAutor(comentario);
  }

  eliminar(comentario: RespuestaComentario): void {
    if (this.eliminandoId() !== null) return;
    if (!confirm('¿Eliminar este comentario?')) return;

    this.eliminandoId.set(comentario.idComentario);
    this.comentarioService.eliminarComentario(comentario.idComentario).subscribe({
      next: () => {
        this.eliminandoId.set(null);
        this.toast.success('Comentario eliminado');
        this.cargar(this.pagina().numero);
      },
      error: (err) => {
        this.eliminandoId.set(null);
        this.toast.error(err.error?.detail || err.error?.message || 'No se pudo eliminar el comentario');
      }
    });
  }

  formatFecha(fecha: string): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
