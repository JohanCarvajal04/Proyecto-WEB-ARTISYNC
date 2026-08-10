import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PedidoService } from '../../services/pedido.service';
import { PeticionCrearPedido } from '../../models/pedido.model';

@Component({
  selector: 'app-pedido-crear',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './pedido-crear.component.html'
})
export class PedidoCrearComponent {
  pedido: PeticionCrearPedido = {
    idServicio: 0,
    precioOfrecido: null,
    fechaEntregaEstimada: null
  };

  loading = false;
  error = '';
  success = '';

  constructor(private pedidoService: PedidoService, private router: Router) {}

  onSubmit(): void {
    if (!this.pedido.idServicio) {
      this.error = 'Debes indicar el ID del servicio';
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
}
