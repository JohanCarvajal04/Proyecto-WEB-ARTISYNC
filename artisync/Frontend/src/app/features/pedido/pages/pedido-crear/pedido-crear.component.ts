import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PedidoService } from '../../services/pedido.service';
import { PeticionCrearPedido } from '../../models/pedido.model';
import { CatalogoPublicoService } from '../../../catalogo/services/catalogo-publico.service';
import { RespuestaServicio } from '../../../catalogo/models/catalogo.model';

@Component({
  selector: 'app-pedido-crear',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './pedido-crear.component.html'
})
export class PedidoCrearComponent implements OnInit {

  private pedidoService = inject(PedidoService);
  private catalogoService = inject(CatalogoPublicoService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  pedido: PeticionCrearPedido = {
    idServicio: 0,
    precioOfrecido: null,
    fechaEntregaEstimada: null
  };

  /**
   * El servicio se elige en el catálogo y llega por query param; esta pantalla
   * ya no pide teclear el id a mano.
   */
  readonly servicio = signal<RespuestaServicio | null>(null);
  readonly cargandoServicio = signal<boolean>(false);

  loading = false;
  error = '';

  ngOnInit(): void {
    const idServicio = Number(this.route.snapshot.queryParamMap.get('idServicio'));
    if (!idServicio) return;

    this.pedido.idServicio = idServicio;
    this.cargarServicio(idServicio);
  }

  private cargarServicio(idServicio: number): void {
    this.cargandoServicio.set(true);
    this.catalogoService.obtenerServicio(idServicio).subscribe({
      next: (servicio) => {
        this.servicio.set(servicio);
        // El precio base es el punto de partida de la negociación.
        this.pedido.precioOfrecido = servicio.precioBase;
        this.cargandoServicio.set(false);
      },
      error: () => {
        this.error = 'No se pudo cargar el servicio seleccionado.';
        this.cargandoServicio.set(false);
      }
    });
  }

  onSubmit(): void {
    if (!this.pedido.idServicio) {
      this.error = 'Elige un servicio en el catálogo antes de continuar.';
      return;
    }

    this.loading = true;
    this.error = '';

    this.pedidoService.crearPedido(this.pedido).subscribe({
      next: (res) => {
        this.loading = false;
        this.router.navigate(['/pedido', res.idPedido]);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al crear el pedido';
        this.loading = false;
      }
    });
  }

  formatPrice(precio: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(precio);
  }
}
