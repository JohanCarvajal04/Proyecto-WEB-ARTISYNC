import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { forkJoin, of, catchError } from 'rxjs';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { ResenaService } from '../../services/resena.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import { RespuestaResena } from '../../models/creador.model';
import { formatDate, mensajeError } from '../../utils/formato';

@Component({
  selector: 'app-resenas',
  standalone: true,
  imports: [DecimalPipe, PerfilRequeridoComponent],
  templateUrl: './resenas.component.html',
  styleUrl: './resenas.component.css'
})
export class ResenasComponent implements OnInit {

  private resenaService = inject(ResenaService);
  private contexto = inject(CreadorContextoService);

  readonly resenas = signal<RespuestaResena[]>([]);
  readonly promedio = signal<number>(0);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');
  readonly filtroEstrellas = signal<number>(0);

  readonly perfilFaltante = this.contexto.perfilFaltante;

  /** Cuántas reseñas hay por cada nota, de 5 a 1 estrellas. */
  distribucion = computed(() => {
    const total = this.resenas().length;
    return [5, 4, 3, 2, 1].map(estrellas => {
      const cantidad = this.resenas().filter(r => r.calificacionEstrellas === estrellas).length;
      return {
        estrellas,
        cantidad,
        porcentaje: total > 0 ? Math.round((cantidad / total) * 100) : 0
      };
    });
  });

  resenasFiltradas = computed(() => {
    const filtro = this.filtroEstrellas();
    const lista = filtro > 0
      ? this.resenas().filter(r => r.calificacionEstrellas === filtro)
      : this.resenas();
    return lista.slice().sort((a, b) => new Date(b.fechaResena).getTime() - new Date(a.fechaResena).getTime());
  });

  /** Promedio calculado en cliente como respaldo si el endpoint no lo devuelve. */
  promedioEfectivo = computed(() => {
    if (this.promedio() > 0) return this.promedio();
    const lista = this.resenas();
    if (lista.length === 0) return 0;
    return lista.reduce((suma, r) => suma + (r.calificacionEstrellas || 0), 0) / lista.length;
  });

  estrellasPromedio = computed(() => {
    const redondeado = Math.round(this.promedioEfectivo());
    return [1, 2, 3, 4, 5].map(n => n <= redondeado);
  });

  ngOnInit(): void {
    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        if (!perfil) {
          this.isLoading.set(false);
          return;
        }
        forkJoin({
          resenas: this.resenaService.listarPorCreador(perfil.idPerfil),
          promedio: this.resenaService.obtenerPromedio(perfil.idPerfil).pipe(catchError(() => of({} as Record<string, unknown>)))
        }).subscribe({
          next: ({ resenas, promedio }) => {
            this.resenas.set(resenas);
            this.promedio.set(Number(promedio['promedio'] ?? promedio['promedioCalificacion'] ?? 0));
            this.isLoading.set(false);
          },
          error: (err) => {
            this.error.set(mensajeError(err, 'Error al cargar tus reseñas'));
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

  setFiltro(estrellas: number): void {
    this.filtroEstrellas.update(actual => actual === estrellas ? 0 : estrellas);
  }

  estrellasDe(cantidad: number): boolean[] {
    return [1, 2, 3, 4, 5].map(n => n <= cantidad);
  }

  formatDate = formatDate;
}
