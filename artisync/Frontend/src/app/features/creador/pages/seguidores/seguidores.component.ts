import { Component, inject, signal, OnInit } from '@angular/core';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { SeguidorService, RespuestaSeguidorInfo } from '../../../social/services/seguidor.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import { formatDate, mensajeError } from '../../utils/formato';

@Component({
  selector: 'app-seguidores',
  standalone: true,
  imports: [PerfilRequeridoComponent],
  templateUrl: './seguidores.component.html',
  styleUrl: './seguidores.component.css'
})
export class SeguidoresComponent implements OnInit {

  private seguidorService = inject(SeguidorService);
  private contexto = inject(CreadorContextoService);

  readonly seguidores = signal<RespuestaSeguidorInfo[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  readonly perfilFaltante = this.contexto.perfilFaltante;

  ngOnInit(): void {
    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        if (!perfil) {
          this.isLoading.set(false);
          return;
        }
        this.seguidorService.listarSeguidores(perfil.idPerfil).subscribe({
          next: (seguidores) => {
            // Más recientes primero.
            const ordenados = seguidores.slice().sort((a, b) =>
              new Date(b.fechaSeguimiento || 0).getTime() - new Date(a.fechaSeguimiento || 0).getTime());
            this.seguidores.set(ordenados);
            this.isLoading.set(false);
          },
          error: (err) => {
            this.error.set(mensajeError(err, 'Error al cargar tus seguidores'));
            this.isLoading.set(false);
          }
        });
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar tu perfil de creador'));
        this.isLoading.set(false);
      }
    });
  }

  inicial(nombre: string): string {
    return (nombre || '?').charAt(0).toUpperCase();
  }

  formatDate = formatDate;
}
