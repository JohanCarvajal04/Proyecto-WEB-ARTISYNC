import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ModeracionService } from '../../services/moderacion.service';
import { RespuestaServicioResumido } from '../../models/moderacion.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';

/**
 * Moderación de servicios (SERVICIO_MODERAR): la única acción disponible hoy
 * es quitarle a un servicio una subcategoría mal asignada. No reutiliza el
 * formulario del creador porque este permiso no tiene por qué acotarse a los
 * servicios de un solo creador ni pasar por su flujo de edición completo.
 */
@Component({
  selector: 'app-mod-servicios',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './mod-servicios.component.html'
})
export class ModServiciosComponent implements OnInit {

  private modService = inject(ModeracionService);
  private toastService = inject(ToastService);

  readonly pagina = signal<Pagina<RespuestaServicioResumido>>(paginaVacia());
  readonly isLoading = signal<boolean>(true);
  /** `"<idServicio>-<idSubcategoria>"` de la subcategoría que se está quitando, para deshabilitar solo ese botón. */
  readonly quitandoId = signal<string | null>(null);

  busqueda = '';
  private busqueda$ = new Subject<string>();

  ngOnInit(): void {
    this.busqueda$.pipe(debounceTime(350), distinctUntilChanged()).subscribe(() => this.cargar(0));
    this.cargar(0);
  }

  onBusqueda(): void {
    this.busqueda$.next(this.busqueda);
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.modService.listarServiciosParaModeracion(this.busqueda.trim(), page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudieron cargar los servicios');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.cargar(numero);
  }

  quitarSubcategoria(servicio: RespuestaServicioResumido, idSubcategoria: number, nombreSubcategoria: string): void {
    if (servicio.subcategorias.length <= 1) {
      this.toastService.error('Un servicio necesita al menos una subcategoría');
      return;
    }
    if (!confirm(`¿Quitar «${nombreSubcategoria}» del servicio «${servicio.tituloServicio}»?`)) return;

    const clave = `${servicio.idServicio}-${idSubcategoria}`;
    this.quitandoId.set(clave);
    this.modService.quitarSubcategoriaDeServicio(servicio.idServicio, idSubcategoria).subscribe({
      next: () => {
        this.quitandoId.set(null);
        this.pagina.update(p => ({
          ...p,
          contenido: p.contenido.map(s => s.idServicio === servicio.idServicio
            ? { ...s, subcategorias: s.subcategorias.filter(sub => sub.idSubcategoria !== idSubcategoria) }
            : s)
        }));
        this.toastService.success(`«${nombreSubcategoria}» quitada de «${servicio.tituloServicio}»`);
      },
      error: (err) => {
        this.quitandoId.set(null);
        this.toastService.error(err.error?.message || 'No se pudo quitar la subcategoría');
      }
    });
  }
}
