import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, interval, of, switchMap, take, catchError } from 'rxjs';
import { PagoService } from '../../services/pago.service';
import { RespuestaPago } from '../../models/legal.model';
import { MonedaPipe } from '../../../../shared/pipes/moneda.pipe';

/**
 * El pago se aprueba en PayPal, en otra pestaña, y quien actualiza el estado de
 * los fondos es el webhook. Esta pantalla no se entera de nada salvo que
 * pregunte, así que tras abrir PayPal sondea hasta que el webhook haya
 * aterrizado (RNF: fondos actualizados en ≤ 10 s post-pago).
 */
const INTERVALO_SONDEO_MS = 5_000;

/** ~5 minutos. Pasado ese punto lo normal es que el pago no se completara. */
const MAX_INTENTOS_SONDEO = 60;

/** Estados en los que ya no hay nada que esperar. */
const ESTADOS_TERMINALES = ['retenido', 'liberado', 'reembolsado'];

/** Trazos SVG (heroicons outline) usados por el badge de estado de fondos. */
const ICONO_CANDADO_CERRADO = 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z';
const ICONO_CANDADO_ABIERTO = 'M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z';
const ICONO_REEMBOLSO = 'M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15';
const ICONO_RELOJ = 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z';

@Component({
  selector: 'app-pago-checkout',
  standalone: true,
  imports: [RouterLink, MonedaPipe],
  templateUrl: './pago-checkout.component.html'
})
export class PagoCheckoutComponent implements OnInit, OnDestroy {
  // NG-03 (auditoria Angular): estado como signals en vez de propiedades
  // planas + ChangeDetectorRef.markForCheck() manual. En zoneless, olvidar
  // markForCheck() tras un camino asincrono nuevo deja la pantalla de pago
  // congelada en silencio, sin ningun error visible; con signal.set() el
  // fallo es imposible por construccion -- es lo que ya usa el resto de
  // componentes de la app (ver docs de la auditoria).
  readonly pago = signal<RespuestaPago | null>(null);
  readonly loading = signal(true);
  readonly creandoPago = signal(false);
  readonly error = signal('');
  /** `true` mientras se espera la confirmación del webhook tras abrir PayPal. */
  readonly esperandoConfirmacion = signal(false);

  idPedido = 0;

  private sondeoSub?: Subscription;

  /**
   * Al volver a la pestaña se comprueba de inmediato en vez de aguardar al
   * siguiente tick: es justo cuando el usuario acaba de pagar y vuelve.
   */
  private readonly alRecuperarFoco = () => {
    if (this.esperandoConfirmacion()) {
      this.refrescarEstado();
    }
  };

  constructor(
    private pagoService: PagoService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.idPedido = Number(this.route.snapshot.paramMap.get('idPedido'));
    this.cargarEstado();
    window.addEventListener('focus', this.alRecuperarFoco);
  }

  ngOnDestroy(): void {
    this.detenerSondeo();
    window.removeEventListener('focus', this.alRecuperarFoco);
  }

  cargarEstado(): void {
    this.loading.set(true);
    this.pagoService.obtenerEstadoPago(this.idPedido).subscribe({
      next: (pago) => {
        this.pago.set(pago);
        this.loading.set(false);
      },
      error: () => {
        this.pago.set(null);
        this.loading.set(false);
      }
    });
  }

  crearOrdenPago(): void {
    this.creandoPago.set(true);
    this.error.set('');

    this.pagoService.crearOrdenPago(this.idPedido).subscribe({
      next: (pago) => {
        this.pago.set(pago);
        this.creandoPago.set(false);
        if (pago.approvalUrl) {
          window.open(pago.approvalUrl, '_blank', 'noopener');
          this.iniciarSondeo();
        }
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Error al crear la orden de pago');
        this.creandoPago.set(false);
      }
    });
  }

  /**
   * El enlace "Pagar con PayPal" reabre una orden ya creada, así que también
   * debe dejar la pantalla a la espera de la confirmación.
   */
  onAbrirPayPal(): void {
    this.iniciarSondeo();
  }

  /** Consulta puntual, sin tocar el spinner de carga inicial. */
  private refrescarEstado(): void {
    this.pagoService.obtenerEstadoPago(this.idPedido).subscribe({
      next: (pago) => this.aplicarEstado(pago),
      error: () => {}
    });
  }

  private iniciarSondeo(): void {
    this.detenerSondeo();
    this.esperandoConfirmacion.set(true);

    this.sondeoSub = interval(INTERVALO_SONDEO_MS).pipe(
      take(MAX_INTENTOS_SONDEO),
      // catchError dentro del switchMap: fuera, un fallo puntual de red
      // completaría el stream y el sondeo moriría antes de tiempo.
      switchMap(() => this.pagoService.obtenerEstadoPago(this.idPedido).pipe(catchError(() => of(null))))
    ).subscribe({
      next: (pago) => {
        if (pago) {
          this.aplicarEstado(pago);
        }
      },
      // Se agotaron los intentos sin confirmación: se deja de esperar, pero el
      // estado que muestre el backend sigue siendo el bueno.
      complete: () => {
        this.esperandoConfirmacion.set(false);
      }
    });
  }

  private aplicarEstado(pago: RespuestaPago): void {
    this.pago.set(pago);
    if (this.esEstadoTerminal(pago)) {
      this.detenerSondeo();
    }
  }

  private esEstadoTerminal(pago: RespuestaPago): boolean {
    return ESTADOS_TERMINALES.includes((pago.estadoFondos || '').toLowerCase());
  }

  private detenerSondeo(): void {
    this.sondeoSub?.unsubscribe();
    this.sondeoSub = undefined;
    this.esperandoConfirmacion.set(false);
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

}
