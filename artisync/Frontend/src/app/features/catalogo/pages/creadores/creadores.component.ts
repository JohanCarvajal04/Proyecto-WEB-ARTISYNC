import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogoPublicoService } from '../../services/catalogo-publico.service';
import { RespuestaPerfil } from '../../models/catalogo.model';

/**
 * Directorio público de creadores activos (M3). Complementa a "Explorar
 * servicios": ahí el cliente busca por servicio/precio, aquí busca
 * directamente por creador.
 */
@Component({
  selector: 'app-creadores',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './creadores.component.html'
})
export class CreadoresComponent implements OnInit {

  private catalogoService = inject(CatalogoPublicoService);

  readonly creadores = signal<RespuestaPerfil[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');
  readonly busqueda = signal<string>('');

  readonly creadoresFiltrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    if (!termino) return this.creadores();
    return this.creadores().filter(c =>
      this.nombreCompleto(c).toLowerCase().includes(termino)
      || (c.biografia || '').toLowerCase().includes(termino)
    );
  });

  ngOnInit(): void {
    this.catalogoService.listarCreadoresActivos().subscribe({
      next: (creadores) => {
        this.creadores.set(creadores);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo cargar el directorio de creadores');
        this.isLoading.set(false);
      }
    });
  }

  onBusqueda(evento: Event): void {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }

  nombreCompleto(c: RespuestaPerfil): string {
    return `${c.nombresUsuario ?? ''} ${c.apellidosUsuario ?? ''}`.trim();
  }

  iniciales(c: RespuestaPerfil): string {
    const nombre = this.nombreCompleto(c);
    if (!nombre) return '?';
    return nombre.split(/\s+/).slice(0, 2).map(p => p.charAt(0).toUpperCase()).join('');
  }
}
