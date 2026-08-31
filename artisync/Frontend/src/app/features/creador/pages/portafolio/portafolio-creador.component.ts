import { Component, ElementRef, inject, signal, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { ToastService } from '../../../../core/services/toast.service';
import { PortafolioService } from '../../../perfil/services/portafolio.service';
import {
  Portafolio,
  OpcionesPersonalizacion,
  PortafolioItem,
  TIPOS_OBRA_ACEPTADOS,
  MAX_BYTES_OBRA
} from '../../../perfil/models/portafolio.model';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import { formatDate, mensajeError } from '../../utils/formato';

const COLORES_POR_DEFECTO: OpcionesPersonalizacion = {
  primary: '#0F9B8E',
  secondary: '#203A43',
  bg: '#EFF2F7',
  text: '#1E293B',
  surface: '#FFFFFF'
};

@Component({
  selector: 'app-portafolio-creador',
  standalone: true,
  imports: [FormsModule, PerfilRequeridoComponent],
  templateUrl: './portafolio-creador.component.html',
  styleUrl: './portafolio-creador.component.css'
})
export class PortafolioCreadorComponent implements OnInit {

  private portafolioService = inject(PortafolioService);
  private contexto = inject(CreadorContextoService);
  private toast = inject(ToastService);

  readonly portafolio = signal<Portafolio | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly guardando = signal<boolean>(false);
  readonly creando = signal<boolean>(false);
  readonly error = signal<string>('');

  readonly perfilFaltante = this.contexto.perfilFaltante;

  // Obras del portafolio
  readonly items = signal<PortafolioItem[]>([]);
  readonly cargandoItems = signal<boolean>(false);
  readonly subiendo = signal<boolean>(false);
  readonly eliminando = signal<number | null>(null);
  readonly archivoSeleccionado = signal<File | null>(null);
  readonly errorArchivo = signal<string>('');
  readonly idItemEditando = signal<number | null>(null);
  readonly guardandoEdicion = signal<boolean>(false);

  @ViewChild('inputArchivo') inputArchivo?: ElementRef<HTMLInputElement>;

  tituloObra = '';
  descripcionObra = '';

  esPublico = false;
  colores: OpcionesPersonalizacion = { ...COLORES_POR_DEFECTO };

  readonly camposColor: { clave: keyof OpcionesPersonalizacion; etiqueta: string; ayuda: string }[] = [
    { clave: 'primary', etiqueta: 'Color primario', ayuda: 'Botones y acentos' },
    { clave: 'secondary', etiqueta: 'Color secundario', ayuda: 'Cabeceras y detalles' },
    { clave: 'bg', etiqueta: 'Fondo', ayuda: 'Lienzo de la página' },
    { clave: 'surface', etiqueta: 'Superficie', ayuda: 'Tarjetas y bloques' },
    { clave: 'text', etiqueta: 'Texto', ayuda: 'Color principal de lectura' }
  ];

  ngOnInit(): void {
    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        if (!perfil) {
          this.isLoading.set(false);
          return;
        }
        this.portafolioService.obtenerPorPerfil(perfil.idPerfil).pipe(
          catchError((err) => {
            // 404 = el creador todavía no ha abierto su portafolio.
            if (err?.status === 404) return of(null);
            throw err;
          })
        ).subscribe({
          next: (portafolio) => {
            this.aplicar(portafolio);
            this.isLoading.set(false);
          },
          error: (err) => {
            this.error.set(mensajeError(err, 'No se pudo cargar tu portafolio'));
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

  private aplicar(portafolio: Portafolio | null): void {
    const anterior = this.portafolio();
    this.portafolio.set(portafolio);
    if (portafolio) {
      this.esPublico = portafolio.esPublico;
      this.colores = { ...COLORES_POR_DEFECTO, ...(portafolio.opcionesPersonalizacion || {}) };
      // Solo al aparecer el portafolio o al cambiar de uno a otro; guardar los
      // colores no debe provocar una recarga de la galería.
      if (anterior?.idPortafolio !== portafolio.idPortafolio) {
        this.cargarItems(portafolio.idPortafolio);
      }
    }
  }

  crear(): void {
    const perfil = this.contexto.perfil();
    if (!perfil) return;

    this.creando.set(true);
    this.portafolioService.crear({
      idPerfil: perfil.idPerfil,
      esPublico: true,
      opcionesPersonalizacion: this.colores
    }).subscribe({
      next: (portafolio) => {
        this.aplicar(portafolio);
        this.creando.set(false);
        this.toast.success('Portafolio creado');
      },
      error: (err) => {
        this.creando.set(false);
        this.toast.error(mensajeError(err, 'No se pudo crear el portafolio'));
      }
    });
  }

  guardar(): void {
    const portafolio = this.portafolio();
    if (!portafolio) return;

    this.guardando.set(true);
    this.portafolioService.actualizar(portafolio.idPortafolio, {
      esPublico: this.esPublico,
      opcionesPersonalizacion: this.colores
    }).subscribe({
      next: (actualizado) => {
        this.aplicar(actualizado);
        this.guardando.set(false);
        this.toast.success('Portafolio actualizado');
      },
      error: (err) => {
        this.guardando.set(false);
        this.toast.error(mensajeError(err, 'No se pudo guardar el portafolio'));
      }
    });
  }

  restaurarColores(): void {
    this.colores = { ...COLORES_POR_DEFECTO };
  }

  // ── Obras del portafolio ───────────────────────────────────────────────────

  private cargarItems(idPortafolio: number): void {
    this.cargandoItems.set(true);
    this.portafolioService.listarItems(idPortafolio).subscribe({
      next: (items) => {
        this.items.set(items);
        this.cargandoItems.set(false);
      },
      error: (err) => {
        this.cargandoItems.set(false);
        this.toast.error(mensajeError(err, 'No se pudieron cargar tus obras'));
      }
    });
  }

  alSeleccionarArchivo(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    this.errorArchivo.set('');

    if (!archivo) {
      this.archivoSeleccionado.set(null);
      return;
    }

    // Se valida aquí lo mismo que PoliticaArchivo.PORTAFOLIO en el backend, para
    // no hacer viajar 100MB solo para que el servidor los rechace. El control
    // real sigue siendo el del backend.
    if (!TIPOS_OBRA_ACEPTADOS.includes(archivo.type)) {
      this.errorArchivo.set('Formato no admitido. Sube una imagen o un video.');
      this.limpiarSeleccion(input);
      return;
    }
    if (archivo.size > MAX_BYTES_OBRA) {
      this.errorArchivo.set('El archivo supera los 100 MB permitidos.');
      this.limpiarSeleccion(input);
      return;
    }

    this.archivoSeleccionado.set(archivo);
  }

  private limpiarSeleccion(input?: HTMLInputElement): void {
    this.archivoSeleccionado.set(null);
    if (input) input.value = '';
  }

  subirObra(): void {
    const portafolio = this.portafolio();
    const archivo = this.archivoSeleccionado();
    if (!portafolio || !archivo || !this.tituloObra.trim()) return;

    this.subiendo.set(true);
    this.portafolioService.subirItem(
      portafolio.idPortafolio,
      { tituloObra: this.tituloObra.trim(), descripcionObra: this.descripcionObra.trim() || undefined },
      archivo
    ).subscribe({
      next: (item) => {
        // El backend devuelve las obras más recientes primero.
        this.items.update((actuales) => [item, ...actuales]);
        this.subiendo.set(false);
        this.resetFormularioObra();
        this.toast.success('Obra publicada');
      },
      error: (err) => {
        this.subiendo.set(false);
        this.toast.error(mensajeError(err, 'No se pudo subir la obra'));
      }
    });
  }

  private resetFormularioObra(): void {
    this.tituloObra = '';
    this.descripcionObra = '';
    this.errorArchivo.set('');
    this.archivoSeleccionado.set(null);
    if (this.inputArchivo?.nativeElement) {
      this.inputArchivo.nativeElement.value = '';
    }
  }

  // ── Edición de metadatos de una obra ──────────────────────────────────────

  tituloEdicion = '';
  descripcionEdicion = '';

  editarObra(item: PortafolioItem): void {
    this.idItemEditando.set(item.idItemPortafolio);
    this.tituloEdicion = item.tituloObra;
    this.descripcionEdicion = item.descripcionObra ?? '';
  }

  cancelarEdicionObra(): void {
    this.idItemEditando.set(null);
  }

  guardarEdicionObra(item: PortafolioItem): void {
    if (!this.tituloEdicion.trim()) return;

    this.guardandoEdicion.set(true);
    this.portafolioService.actualizarItem(item.idItemPortafolio, {
      tituloObra: this.tituloEdicion.trim(),
      descripcionObra: this.descripcionEdicion.trim() || undefined
    }).subscribe({
      next: (actualizado) => {
        this.items.update((actuales) =>
          actuales.map((i) => i.idItemPortafolio === actualizado.idItemPortafolio ? actualizado : i));
        this.guardandoEdicion.set(false);
        this.idItemEditando.set(null);
        this.toast.success('Obra actualizada');
      },
      error: (err) => {
        this.guardandoEdicion.set(false);
        this.toast.error(mensajeError(err, 'No se pudo actualizar la obra'));
      }
    });
  }

  eliminarObra(item: PortafolioItem): void {
    if (!confirm(`¿Eliminar "${item.tituloObra}"? Esta acción no se puede deshacer.`)) return;

    this.eliminando.set(item.idItemPortafolio);
    this.portafolioService.eliminarItem(item.idItemPortafolio).subscribe({
      next: () => {
        this.items.update((actuales) =>
          actuales.filter((i) => i.idItemPortafolio !== item.idItemPortafolio));
        this.eliminando.set(null);
        this.toast.success('Obra eliminada');
      },
      error: (err) => {
        this.eliminando.set(null);
        this.toast.error(mensajeError(err, 'No se pudo eliminar la obra'));
      }
    });
  }

  /** Decide si la miniatura se pinta con <img> o con <video>. */
  esVideo(item: PortafolioItem): boolean {
    return /\.(mp4|webm|mov)(\?|$)/i.test(item.urlArchivo);
  }

  formatDate = formatDate;
}
