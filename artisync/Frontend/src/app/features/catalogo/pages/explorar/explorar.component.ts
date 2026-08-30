import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { CatalogoPublicoService } from '../../services/catalogo-publico.service';
import {
  FiltrosCatalogo,
  ORDEN_CATALOGO,
  RespuestaCategoria,
  RespuestaEtiqueta,
  RespuestaServicioResumido,
  RespuestaSubcategoria
} from '../../models/catalogo.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { CATALOGO_BASE_PATH } from '../../catalogo.config';

const TAMANO_PAGINA = 12;

@Component({
  selector: 'app-explorar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './explorar.component.html'
})
export class ExplorarComponent implements OnInit {

  private catalogoService = inject(CatalogoPublicoService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  /** Prefijo de los routerLink internos: '/explorar' o '/dashboard/explorar' según el montaje. */
  readonly base = inject(CATALOGO_BASE_PATH);

  readonly pagina = signal<Pagina<RespuestaServicioResumido>>(paginaVacia());
  readonly categorias = signal<RespuestaCategoria[]>([]);
  readonly subcategorias = signal<RespuestaSubcategoria[]>([]);
  readonly etiquetas = signal<RespuestaEtiqueta[]>([]);

  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  readonly ordenes = ORDEN_CATALOGO;

  // Estado de los filtros
  readonly filtros = signal<FiltrosCatalogo>({
    categoria: null,
    subcategoria: null,
    precioMin: null,
    precioMax: null,
    etiquetas: [],
    q: '',
    sort: ORDEN_CATALOGO[0].valor,
    page: 0,
    size: TAMANO_PAGINA
  });

  /** El texto libre no debe disparar una petición por cada tecla. */
  private busqueda$ = new Subject<string>();

  readonly hayFiltrosActivos = computed(() => {
    const f = this.filtros();
    return f.categoria != null || f.subcategoria != null || f.precioMin != null
      || f.precioMax != null || !!f.q || (f.etiquetas?.length ?? 0) > 0;
  });

  readonly paginasVisibles = computed(() => {
    const total = this.pagina().totalPaginas;
    const actual = this.pagina().numero;
    const desde = Math.max(0, Math.min(actual - 2, total - 5));
    const hasta = Math.min(total, desde + 5);
    return Array.from({ length: Math.max(0, hasta - desde) }, (_, i) => desde + i);
  });

  ngOnInit(): void {
    this.catalogoService.listarCategorias().subscribe({
      next: (cats) => this.categorias.set(cats.filter(c => c.estadoActiva)),
      error: () => {}
    });
    this.catalogoService.listarEtiquetas().subscribe({
      next: (etqs) => this.etiquetas.set(etqs),
      error: () => {}
    });

    this.busqueda$.pipe(debounceTime(350), distinctUntilChanged()).subscribe(texto => {
      this.actualizarFiltro({ q: texto, page: 0 });
    });

    // Permite llegar con ?categoria=3 desde otra pantalla.
    const categoriaParam = this.route.snapshot.queryParamMap.get('categoria');
    if (categoriaParam) {
      const idCategoria = Number(categoriaParam);
      this.filtros.update(f => ({ ...f, categoria: idCategoria }));
      this.cargarSubcategorias(idCategoria);
    }

    this.buscar();
  }

  buscar(): void {
    this.isLoading.set(true);
    this.error.set('');

    this.catalogoService.buscarCatalogo(this.filtros()).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo cargar el catálogo');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  private actualizarFiltro(parcial: Partial<FiltrosCatalogo>): void {
    this.filtros.update(f => ({ ...f, ...parcial }));
    this.buscar();
  }

  onBusqueda(evento: Event): void {
    this.busqueda$.next((evento.target as HTMLInputElement).value.trim());
  }

  onCategoria(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    const idCategoria = valor ? Number(valor) : null;

    this.subcategorias.set([]);
    if (idCategoria != null) this.cargarSubcategorias(idCategoria);

    this.actualizarFiltro({ categoria: idCategoria, subcategoria: null, page: 0 });
  }

  private cargarSubcategorias(idCategoria: number): void {
    this.catalogoService.listarSubcategoriasDeCategoria(idCategoria).subscribe({
      next: (subs) => this.subcategorias.set(subs),
      error: () => this.subcategorias.set([])
    });
  }

  onSubcategoria(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    this.actualizarFiltro({ subcategoria: valor ? Number(valor) : null, page: 0 });
  }

  onOrden(evento: Event): void {
    this.actualizarFiltro({ sort: (evento.target as HTMLSelectElement).value, page: 0 });
  }

  onPrecioMin(evento: Event): void {
    const valor = (evento.target as HTMLInputElement).value;
    this.actualizarFiltro({ precioMin: valor ? Number(valor) : null, page: 0 });
  }

  onPrecioMax(evento: Event): void {
    const valor = (evento.target as HTMLInputElement).value;
    this.actualizarFiltro({ precioMax: valor ? Number(valor) : null, page: 0 });
  }

  alternarEtiqueta(idEtiqueta: number): void {
    const actuales = this.filtros().etiquetas ?? [];
    const siguientes = actuales.includes(idEtiqueta)
      ? actuales.filter(id => id !== idEtiqueta)
      : [...actuales, idEtiqueta];
    this.actualizarFiltro({ etiquetas: siguientes, page: 0 });
  }

  etiquetaActiva(idEtiqueta: number): boolean {
    return (this.filtros().etiquetas ?? []).includes(idEtiqueta);
  }

  limpiarFiltros(): void {
    this.subcategorias.set([]);
    this.filtros.set({
      categoria: null, subcategoria: null, precioMin: null, precioMax: null,
      etiquetas: [], q: '', sort: ORDEN_CATALOGO[0].valor, page: 0, size: TAMANO_PAGINA
    });
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
    this.buscar();
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.actualizarFiltro({ page: numero });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  formatPrice(precio: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(precio);
  }
}
