import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PagoService } from '../../services/pago.service';
import { RespuestaPago } from '../../models/legal.model';

/** Trazos SVG (heroicons outline) usados por el badge de estado de fondos. */
const ICONO_CANDADO_CERRADO = 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z';
const ICONO_CANDADO_ABIERTO = 'M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z';
const ICONO_REEMBOLSO = 'M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15';
const ICONO_RELOJ = 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z';

@Component({
  selector: 'app-pago-checkout',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pago-checkout.component.html'
})
export class PagoCheckoutComponent implements OnInit {
  pago: RespuestaPago | null = null;
  loading = true;
  creandoPago = false;
  error = '';
  idPedido = 0;

  constructor(
    private pagoService: PagoService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.idPedido = Number(this.route.snapshot.paramMap.get('idPedido'));
    this.cargarEstado();
  }

  cargarEstado(): void {
    this.loading = true;
    this.pagoService.obtenerEstadoPago(this.idPedido).subscribe({
      next: (pago) => {
        this.pago = pago;
        this.loading = false;
      },
      error: () => {
        this.pago = null;
        this.loading = false;
      }
    });
  }

  crearOrdenPago(): void {
    this.creandoPago = true;
    this.error = '';

    this.pagoService.crearOrdenPago(this.idPedido).subscribe({
      next: (pago) => {
        this.pago = pago;
        this.creandoPago = false;
        if (pago.approvalUrl) {
          window.open(pago.approvalUrl, '_blank');
        }
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al crear la orden de pago';
        this.creandoPago = false;
      }
    });
  }

  readonly pasosEscrow = [
    { num: 1, texto: 'El cliente realiza el pago y los fondos quedan retenidos en garantía.' },
    { num: 2, texto: 'El creador completa el trabajo y sube el entregable.' },
    { num: 3, texto: 'El cliente aprueba el trabajo y los fondos se liberan al creador.' }
  ];

  getEstadoInfo(estado: string): { path: string; wrapper: string; label: string } {
    switch (estado?.toLowerCase()) {
      case 'retenido':
        return { path: ICONO_CANDADO_CERRADO, wrapper: 'bg-gradient-to-r from-amber-400 to-amber-500 text-white', label: 'Fondos Retenidos' };
      case 'liberado':
        return { path: ICONO_CANDADO_ABIERTO, wrapper: 'bg-gradient-to-r from-emerald-500 to-emerald-600 text-white', label: 'Fondos Liberados' };
      case 'reembolsado':
        return { path: ICONO_REEMBOLSO, wrapper: 'bg-gradient-to-r from-slate-500 to-slate-600 text-white', label: 'Reembolsado' };
      default:
        return { path: ICONO_RELOJ, wrapper: 'bg-gradient-to-r from-sky-500 to-sky-600 text-white', label: estado || 'Pendiente' };
    }
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price);
  }
}
