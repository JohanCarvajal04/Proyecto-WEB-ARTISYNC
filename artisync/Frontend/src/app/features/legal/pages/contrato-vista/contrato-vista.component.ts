import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ContratoService } from '../../services/contrato.service';
import { RespuestaContrato, RespuestaEstadoFirma } from '../../models/legal.model';
import { AuthService } from '../../../seguridad/services/auth.service';

@Component({
  selector: 'app-contrato-vista',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './contrato-vista.component.html'
})
export class ContratoVistaComponent implements OnInit {
  contrato: RespuestaContrato | null = null;
  estadoFirma: RespuestaEstadoFirma | null = null;
  loading = true;
  firmando = false;
  error = '';
  successMsg = '';

  // Público: el template lo usa para el enlace "Volver al pedido", igual que
  // pago-checkout y entregable-vista (pantallas hermanas del mismo detalle).
  idPedido = 0;
  private idContrato = 0;
  private modo: 'contrato' | 'pedido' = 'pedido';

  constructor(
    private contratoService: ContratoService,
    public authService: AuthService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.paramMap;
    if (params.has('idPedido')) {
      this.idPedido = Number(params.get('idPedido'));
      this.modo = 'pedido';
    } else {
      this.idContrato = Number(params.get('id'));
      this.modo = 'contrato';
    }
    this.cargarContrato();
  }

  cargarContrato(): void {
    this.loading = true;
    this.error = '';

    const obs$ = this.modo === 'pedido'
      ? this.contratoService.obtenerContratoPorPedido(this.idPedido)
      : this.contratoService.obtenerContrato(this.idContrato);

    obs$.subscribe({
      next: (contrato) => {
        this.contrato = contrato;
        this.idContrato = contrato.idContrato;
        // En modo 'contrato' (entrada por /legal/contrato/:id) la ruta no
        // trae el idPedido; lo toma del propio contrato para que el enlace
        // "Volver al pedido" funcione igual en ambos modos de entrada.
        this.idPedido = contrato.idPedido;
        this.loading = false;
        this.cargarEstadoFirma();
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (this.modo === 'pedido' && err.status === 404) {
          this.generarContrato();
        } else {
          this.error = err.error?.message || 'Error al cargar el contrato';
          this.loading = false;
          this.cdr.markForCheck();
        }
      }
    });
  }

  generarContrato(): void {
    this.contratoService.generarContrato(this.idPedido).subscribe({
      next: (contrato) => {
        this.contrato = contrato;
        this.idContrato = contrato.idContrato;
        this.loading = false;
        this.cargarEstadoFirma();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al generar el contrato';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  cargarEstadoFirma(): void {
    if (!this.idContrato) return;
    this.contratoService.obtenerEstadoFirma(this.idContrato).subscribe({
      next: (estado) => {
        this.estadoFirma = estado;
        this.cdr.markForCheck();
      }
    });
  }

  firmarContrato(): void {
    if (this.firmando || !this.idContrato) return;
    this.firmando = true;
    this.error = '';

    this.contratoService.firmarContrato(this.idContrato).subscribe({
      next: (contrato) => {
        this.contrato = contrato;
        this.firmando = false;
        this.successMsg = '¡Contrato firmado exitosamente!';
        this.cargarEstadoFirma();
        this.cdr.markForCheck();
        setTimeout(() => { this.successMsg = ''; this.cdr.markForCheck(); }, 4000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al firmar el contrato';
        this.firmando = false;
        this.cdr.markForCheck();
      }
    });
  }

  descargarPdf(): void {
    if (!this.idContrato) return;
    this.contratoService.descargarPdf(this.idContrato).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contrato_${this.idContrato}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.error = 'Error al descargar el PDF';
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Por identidad, no por rol global: un usuario puede tener a la vez los
   * roles CLIENTE y CREADOR (cuenta híbrida, el admin puede asignar ambos).
   * Con hasRole('CREADOR') esa cuenta veía "Ya has firmado" —y el botón de
   * firmar desaparecía— con solo que EL CREADOR DE ESTE CONTRATO (otra
   * persona) hubiera firmado, aunque ella todavía no lo hiciera como
   * cliente. El backend ya firma por identidad (ContratoServicioImpl); esto
   * solo alinea la UI con esa misma regla.
   */
  get yaFirme(): boolean {
    if (!this.contrato) return false;
    const idActual = this.authService.getCurrentUserId();
    if (idActual === this.contrato.idCreador && this.contrato.hashFirmaCreador) return true;
    if (idActual === this.contrato.idCliente && this.contrato.hashFirmaCliente) return true;
    return false;
  }

  get puedeIniciarProduccion(): boolean {
    return this.contrato?.ambasFirmasCompletas ?? false;
  }
}
