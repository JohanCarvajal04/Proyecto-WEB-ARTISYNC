import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../../core/services/toast.service';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { ServicioService } from '../../services/servicio.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import {
  RespuestaServicioResumido,
  RespuestaServicio,
  PeticionActualizarServicio,
  EstadoPublicacion
} from '../../models/creador.model';
import { formatPrice, badgePublicacion, mensajeError } from '../../utils/formato';

type FiltroEstado = 'TODOS' | EstadoPublicacion;

@Component({
  selector: 'app-mis-servicios',
  standalone: true,
  imports: [RouterLink, TitleCasePipe, PerfilRequeridoComponent],
  templateUrl: './mis-servicios.component.html',
  styleUrl: './mis-servicios.component.css'
})
export class MisServiciosComponent implements OnInit {

  private servicioService = inject(ServicioService);
  private contexto = inject(CreadorContextoService);
  private toast = inject(ToastService);

  readonly servicios = signal<RespuestaServicioResumido[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');
  readonly filtro = signal<FiltroEstado>('TODOS');
  readonly busqueda = signal<string>('');
  /** Id del servicio cuya publicación se está cambiando (para deshabilitar su fila). */
  readonly procesando = signal<number | null>(null);
  /** Servicio pendiente de confirmación de borrado. */
  readonly aEliminar = signal<RespuestaServicioResumido | null>(null);

  readonly perfilFaltante = this.contexto.perfilFaltante;

  readonly estados: FiltroEstado[] = ['TODOS', 'ACTIVO', 'PAUSADO', 'BORRADOR'];

  totalPorEstado = computed(() => {
    const conteo: Record<string, number> = { TODOS: this.servicios().length, ACTIVO: 0, PAUSADO: 0, BORRADOR: 0 };
    for (const s of this.servicios()) {
      conteo[s.estadoPublicacion] = (conteo[s.estadoPublicacion] || 0) + 1;
    }
    return conteo;
  });

  serviciosFiltrados = computed(() => {
    const estado = this.filtro();
    const texto = this.busqueda().trim().toLowerCase();
    return this.servicios().filter(s => {
      if (estado !== 'TODOS' && s.estadoPublicacion !== estado) return false;
      if (texto) {
        const blob = `${s.tituloServicio} ${s.nombreCategoria} ${s.nombreSubcategoria}`.toLowerCase();
        if (!blob.includes(texto)) return false;
      }
      return true;
    });
  });

  precioPromedio = computed(() => {
    const lista = this.servicios();
    if (lista.length === 0) return 0;
    return lista.reduce((suma, s) => suma + Number(s.precioBase || 0), 0) / lista.length;
  });

  ngOnInit(): void {
    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        if (!perfil) {
          this.isLoading.set(false);
          return;
        }
        this.cargar();
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar tu perfil de creador'));
        this.isLoading.set(false);
      }
    });
  }

  cargar(): void {
    const perfil = this.contexto.perfil();
    if (!perfil) return;
    this.isLoading.set(true);
    this.error.set('');
    this.servicioService.listarPorCreador(perfil.idPerfil).subscribe({
      next: (data) => {
        this.servicios.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'Error al cargar tus servicios'));
        this.isLoading.set(false);
      }
    });
  }

  setFiltro(estado: FiltroEstado): void {
    this.filtro.set(estado);
  }

  onBusqueda(event: Event): void {
    this.busqueda.set((event.target as HTMLInputElement).value);
  }

  limpiarFiltros(): void {
    this.filtro.set('TODOS');
    this.busqueda.set('');
  }

  /**
   * Publica o pausa un servicio. El endpoint de actualización es un PUT completo,
   * así que primero se recupera el detalle y se reenvía con el estado cambiado.
   */
  cambiarEstado(servicio: RespuestaServicioResumido, nuevoEstado: EstadoPublicacion): void {
    this.procesando.set(servicio.idServicio);
    this.servicioService.obtenerPorId(servicio.idServicio).subscribe({
      next: (detalle: RespuestaServicio) => {
        const peticion: PeticionActualizarServicio = {
          tituloServicio: detalle.tituloServicio,
          descripcionDetallada: detalle.descripcionDetallada,
          precioBase: detalle.precioBase,
          idSubcategoria: detalle.idSubcategoria,
          tipoItem: detalle.tipoItem,
          estadoPublicacion: nuevoEstado,
          urlMiniatura: detalle.urlMiniatura,
          cargoRevisionAdicional: detalle.cargoRevisionAdicional,
          limiteRevisionesBase: detalle.limiteRevisionesBase,
          etiquetaIds: (detalle.etiquetas || []).map(e => e.idEtiqueta)
        };
        this.servicioService.actualizar(servicio.idServicio, peticion).subscribe({
          next: () => {
            this.servicios.update(lista => lista.map(s =>
              s.idServicio === servicio.idServicio ? { ...s, estadoPublicacion: nuevoEstado } : s
            ));
            this.procesando.set(null);
            this.toast.success(nuevoEstado === 'ACTIVO' ? 'Servicio publicado' : 'Servicio pausado');
          },
          error: (err) => {
            this.procesando.set(null);
            this.toast.error(mensajeError(err, 'No se pudo cambiar el estado del servicio'));
          }
        });
      },
      error: (err) => {
        this.procesando.set(null);
        this.toast.error(mensajeError(err, 'No se pudo leer el servicio'));
      }
    });
  }

  pedirConfirmacion(servicio: RespuestaServicioResumido): void {
    this.aEliminar.set(servicio);
  }

  cancelarEliminacion(): void {
    this.aEliminar.set(null);
  }

  confirmarEliminacion(): void {
    const servicio = this.aEliminar();
    if (!servicio) return;
    this.procesando.set(servicio.idServicio);
    this.servicioService.eliminar(servicio.idServicio).subscribe({
      next: () => {
        this.servicios.update(lista => lista.filter(s => s.idServicio !== servicio.idServicio));
        this.aEliminar.set(null);
        this.procesando.set(null);
        this.toast.success('Servicio eliminado');
      },
      error: (err) => {
        this.procesando.set(null);
        this.aEliminar.set(null);
        this.toast.error(mensajeError(err, 'No se pudo eliminar el servicio'));
      }
    });
  }

  formatPrice = formatPrice;
  badgePublicacion = badgePublicacion;
}
