import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComentarioService, RespuestaComentario } from '../../services/comentario.service';
import { AuthService } from '../../../seguridad/services/auth.service';

@Component({
  selector: 'app-comentarios-obra',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './comentarios-obra.component.html'
})
export class ComentariosObraComponent implements OnInit {
  @Input({ required: true }) idItemPortafolio!: number;
  @Input() idCreador!: number;

  private comentarioService = inject(ComentarioService);
  private authService = inject(AuthService);

  readonly comentarios = signal<RespuestaComentario[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isLoadingMas = signal<boolean>(false);
  readonly error = signal<string>('');
  
  readonly nuevoComentario = signal<string>('');
  readonly isEnviando = signal<boolean>(false);
  
  readonly isLogueado = signal<boolean>(false);
  readonly idUsuarioLogueado = signal<number | null>(null);

  private currentPage = 0;
  readonly hasMore = signal<boolean>(false);

  ngOnInit(): void {
    this.isLogueado.set(this.authService.isLoggedIn());
    const currentUserId = this.authService.getCurrentUserId();
    if (currentUserId) {
      this.idUsuarioLogueado.set(currentUserId);
    }
    this.cargarComentarios();
  }

  cargarComentarios(): void {
    this.isLoading.set(true);
    this.comentarioService.listarComentarios(this.idItemPortafolio, 0, 5).subscribe({
      next: (page) => {
        this.comentarios.set(page.content);
        this.currentPage = 0;
        this.hasMore.set(page.number < page.totalPages - 1);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los comentarios');
        this.isLoading.set(false);
      }
    });
  }

  cargarMas(): void {
    if (!this.hasMore() || this.isLoadingMas()) return;
    
    this.isLoadingMas.set(true);
    this.comentarioService.listarComentarios(this.idItemPortafolio, this.currentPage + 1, 5).subscribe({
      next: (page) => {
        this.comentarios.update(c => [...c, ...page.content]);
        this.currentPage = page.number;
        this.hasMore.set(page.number < page.totalPages - 1);
        this.isLoadingMas.set(false);
      },
      error: () => this.isLoadingMas.set(false)
    });
  }

  enviarComentario(): void {
    if (!this.nuevoComentario().trim() || !this.isLogueado()) return;
    
    this.isEnviando.set(true);
    this.comentarioService.agregarComentario(this.idItemPortafolio, { textoComentario: this.nuevoComentario().trim() }).subscribe({
      next: (res) => {
        this.comentarios.update(c => [res, ...c]);
        this.nuevoComentario.set('');
        this.isEnviando.set(false);
      },
      error: (err) => {
        console.error(err);
        this.isEnviando.set(false);
      }
    });
  }

  eliminarComentario(idComentario: number): void {
    if (!confirm('¿Estás seguro de que quieres eliminar este comentario?')) return;
    
    this.comentarioService.eliminarComentario(idComentario).subscribe({
      next: () => {
        this.comentarios.update(c => c.filter(x => x.idComentario !== idComentario));
      },
      error: (err) => console.error(err)
    });
  }

  puedeEliminar(c: RespuestaComentario): boolean {
    const id = this.idUsuarioLogueado();
    if (!id) return false;
    return id === c.idUsuarioAutor || id === this.idCreador;
  }

  formatDate(fecha: string): string {
    if (!fecha) return '';
    return new Date(fecha).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
