import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { InfraccionService } from '../../services/infraccion.service';
import { Infraccion } from '../../models/moderacion.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-infracciones',
  standalone: true,
  imports: [],
  templateUrl: './infracciones.component.html'
})
export class InfraccionesComponent implements OnInit {

  private infraccionService = inject(InfraccionService);
  private toastService = inject(ToastService);

  readonly pagina = signal<Pagina<Infraccion>>(paginaVacia());
  readonly isLoading = signal<boolean>(true);

  /** Usuario cuyo historial se está filtrando; `null` = todo el sistema. */
  readonly usuarioFiltrado = signal<Infraccion | null>(null);

  readonly detalle = signal<Infraccion | null>(null);
  readonly aRevertir = signal<Infraccion | null>(null);
  readonly revirtiendo = signal<boolean>(false);

  /** Reincidentes: agrupa el listado actual por usuario. */
  readonly reincidentes = computed(() => {
    const conteo = new Map<number, { nombre: string; total: number }>();
    for (const i of this.pagina().contenido) {
      const previo = conteo.get(i.idUsuario);
      conteo.set(i.idUsuario, {
        nombre: i.nombreUsuario,
        total: (previo?.total ?? 0) + 1
      });
    }
    return [...conteo.entries()]
      .map(([idUsuario, v]) => ({ idUsuario, ...v }))
      .filter(u => u.total > 1)
      .sort((a, b) => b.total - a.total);
  });

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(page: number): void {
    this.isLoading.set(true);

    const usuario = this.usuarioFiltrado();
    const peticion$ = usuario
      ? this.infraccionService.historialPorUsuario(usuario.idUsuario, page)
      : this.infraccionService.listarInfracciones(page);

    peticion$.subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudieron cargar las infracciones');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  filtrarPorUsuario(infraccion: Infraccion): void {
    this.usuarioFiltrado.set(infraccion);
    this.cargar(0);
  }

  quitarFiltro(): void {
    this.usuarioFiltrado.set(null);
    this.cargar(0);
  }

  verDetalle(infraccion: Infraccion): void {
    this.detalle.set(infraccion);
  }

  cerrarDetalle(): void {
    this.detalle.set(null);
  }

  confirmarReversion(infraccion: Infraccion): void {
    this.aRevertir.set(infraccion);
  }

  cancelarReversion(): void {
    this.aRevertir.set(null);
  }

  revertirSuspension(): void {
    const infraccion = this.aRevertir();
    if (!infraccion) return;

    this.revirtiendo.set(true);
    this.infraccionService.revertirSuspension(infraccion.idUsuario).subscribe({
      next: (res) => {
        this.revirtiendo.set(false);
        this.aRevertir.set(null);
        this.toastService.success(res.message || res.mensaje || 'Suspensión revertida');
      },
      error: (err) => {
        this.revirtiendo.set(false);
        this.toastService.error(err.error?.message || 'No se pudo revertir la suspensión');
      }
    });
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.cargar(numero);
  }

  formatFecha(fecha: string): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  /** Recorta el mensaje para la tabla; el completo va en el detalle. */
  resumen(texto: string): string {
    if (!texto) return '—';
    return texto.length > 80 ? `${texto.slice(0, 80)}…` : texto;
  }
}
