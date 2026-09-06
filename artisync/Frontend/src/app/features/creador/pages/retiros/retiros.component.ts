import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RetiroService } from '../../services/retiro.service';
import { DatosPagoService } from '../../services/datos-pago.service';
import { SaldoCreador, SolicitudRetiro, DatosPagoCreador } from '../../models/retiro.model';
import { ToastService } from '../../../../core/services/toast.service';
import { MonedaPipe } from '../../../../shared/pipes/moneda.pipe';

/**
 * Saldo disponible, correo de PayPal y solicitud de retiro del creador,
 * espejo de SolicitudRetiroControlador.java y DatosPagoControlador.java.
 */
@Component({
  selector: 'app-retiros',
  standalone: true,
  imports: [FormsModule, MonedaPipe],
  templateUrl: './retiros.component.html'
})
export class RetirosComponent implements OnInit {

  private retiroService = inject(RetiroService);
  private datosPagoService = inject(DatosPagoService);
  private toastService = inject(ToastService);

  readonly saldo = signal<SaldoCreador | null>(null);
  readonly datosPago = signal<DatosPagoCreador | null>(null);
  readonly historial = signal<SolicitudRetiro[]>([]);
  readonly isLoading = signal(true);

  readonly editandoCorreo = signal(false);
  readonly guardandoCorreo = signal(false);
  correoPaypalForm = '';

  readonly solicitandoRetiro = signal(false);
  montoASolicitar: number | null = null;

  /**
   * `SaldoCreador | null` con optional chaining produce `boolean | undefined`
   * en la plantilla, que no es asignable a `[disabled]` (espera `boolean`
   * estricto). Centralizarlo aquí como computed evita repetir el `!!` dos
   * veces en el HTML (el input del monto y el botón de solicitar).
   */
  readonly noPuedeSolicitar = computed(() => {
    const saldo = this.saldo();
    return !saldo || !saldo.tieneCorreoPaypalConfigurado || saldo.tieneSolicitudPendiente;
  });

  ngOnInit(): void {
    this.cargarTodo();
  }

  cargarTodo(): void {
    this.isLoading.set(true);
    this.retiroService.obtenerSaldo().subscribe({
      next: (saldo) => {
        this.saldo.set(saldo);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudo cargar tu saldo disponible');
        this.isLoading.set(false);
      }
    });

    this.datosPagoService.obtener().subscribe({
      next: (datos) => {
        this.datosPago.set(datos);
        this.correoPaypalForm = datos.correoPaypal ?? '';
        // Sin correo configurado todavía, se abre el formulario directo: no
        // tiene sentido mostrar "editar" para un dato que aún no existe.
        this.editandoCorreo.set(!datos.correoPaypal);
      },
      error: () => {}
    });

    this.cargarHistorial();
  }

  cargarHistorial(): void {
    this.retiroService.misSolicitudes().subscribe({
      next: (historial) => this.historial.set(historial),
      error: () => this.historial.set([])
    });
  }

  guardarCorreo(): void {
    const correo = this.correoPaypalForm.trim();
    if (!correo) {
      this.toastService.error('Ingresa un correo de PayPal válido');
      return;
    }

    this.guardandoCorreo.set(true);
    this.datosPagoService.actualizarCorreoPaypal(correo).subscribe({
      next: (datos) => {
        this.datosPago.set(datos);
        this.editandoCorreo.set(false);
        this.guardandoCorreo.set(false);
        this.toastService.success('Correo de PayPal actualizado');
        // El saldo trae `tieneCorreoPaypalConfigurado`, que acaba de cambiar.
        this.retiroService.obtenerSaldo().subscribe({
          next: (saldo) => this.saldo.set(saldo),
          error: () => {}
        });
      },
      // El error.interceptor global ya muestra el detalle del backend
      // (formato de correo inválido, etc.).
      error: () => this.guardandoCorreo.set(false)
    });
  }

  cancelarEdicionCorreo(): void {
    this.correoPaypalForm = this.datosPago()?.correoPaypal ?? '';
    this.editandoCorreo.set(false);
  }

  solicitarRetiro(): void {
    const monto = this.montoASolicitar;
    if (!monto || monto <= 0) {
      this.toastService.error('Ingresa un monto válido');
      return;
    }

    this.solicitandoRetiro.set(true);
    this.retiroService.solicitar(monto).subscribe({
      next: () => {
        this.toastService.success('Solicitud de retiro creada');
        this.montoASolicitar = null;
        this.solicitandoRetiro.set(false);
        this.cargarTodo();
      },
      // El error.interceptor global ya muestra el motivo real (monto bajo el
      // mínimo, por encima del saldo, solicitud ya en curso, etc.).
      error: () => this.solicitandoRetiro.set(false)
    });
  }

  formatFechaHora(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  badgeEstado(estado: string): string {
    switch (estado) {
      case 'Pagado': return 'bg-emerald-50 text-emerald-700';
      case 'Aprobado': return 'bg-sky-50 text-sky-700';
      case 'Pendiente': return 'bg-amber-50 text-amber-700';
      case 'Rechazado': return 'bg-slate-100 text-slate-600';
      case 'Fallido': return 'bg-rose-50 text-rose-700';
      default: return 'bg-slate-100 text-slate-600';
    }
  }
}
