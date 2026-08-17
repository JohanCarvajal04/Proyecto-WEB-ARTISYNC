import { Component, OnInit } from '@angular/core';
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

  private idPedido = 0;
  private idContrato = 0;
  private modo: 'contrato' | 'pedido' = 'pedido';

  constructor(
    private contratoService: ContratoService,
    public authService: AuthService,
    private route: ActivatedRoute
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
        this.loading = false;
        this.cargarEstadoFirma();
      },
      error: (err) => {
        if (this.modo === 'pedido' && err.status === 404) {
          this.generarContrato();
        } else {
          this.error = err.error?.message || 'Error al cargar el contrato';
          this.loading = false;
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
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al generar el contrato';
        this.loading = false;
      }
    });
  }

  cargarEstadoFirma(): void {
    if (!this.idContrato) return;
    this.contratoService.obtenerEstadoFirma(this.idContrato).subscribe({
      next: (estado) => this.estadoFirma = estado
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
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al firmar el contrato';
        this.firmando = false;
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
      }
    });
  }

  get yaFirme(): boolean {
    if (!this.contrato) return false;
    if (this.authService.hasRole('CREADOR') && this.contrato.hashFirmaCreador) return true;
    if (this.authService.hasRole('CLIENTE') && this.contrato.hashFirmaCliente) return true;
    return false;
  }

  get puedeIniciarProduccion(): boolean {
    return this.contrato?.ambasFirmasCompletas ?? false;
  }
}
