import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EntregableService } from '../../services/entregable.service';
import { RespuestaEntregable } from '../../models/legal.model';
import { AuthService } from '../../../seguridad/services/auth.service';

@Component({
  selector: 'app-entregable-vista',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './entregable-vista.component.html'
})
export class EntregableVistaComponent implements OnInit {
  entregable: RespuestaEntregable | null = null;
  loading = true;
  error = '';
  successMsg = '';

  // Upload form (Creador)
  urlMarcaAgua = '';
  urlLimpia = '';
  subiendo = false;

  // Aprobar (Cliente)
  aprobando = false;

  idPedido = 0;

  constructor(
    private entregableService: EntregableService,
    public authService: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.idPedido = Number(this.route.snapshot.paramMap.get('idPedido'));
    this.cargarEntregable();
  }

  cargarEntregable(): void {
    this.loading = true;
    this.entregableService.obtenerEntregable(this.idPedido).subscribe({
      next: (ent) => {
        this.entregable = ent;
        this.loading = false;
      },
      error: () => {
        this.entregable = null;
        this.loading = false;
      }
    });
  }

  subirEntregable(): void {
    if (!this.urlMarcaAgua || !this.urlLimpia) {
      this.error = 'Ambas URLs son requeridas';
      return;
    }

    this.subiendo = true;
    this.error = '';

    this.entregableService.subirEntregable(this.idPedido, this.urlMarcaAgua, this.urlLimpia).subscribe({
      next: (ent) => {
        this.entregable = ent;
        this.subiendo = false;
        this.successMsg = 'Entregable subido exitosamente';
        this.urlMarcaAgua = '';
        this.urlLimpia = '';
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al subir entregable';
        this.subiendo = false;
      }
    });
  }

  aprobarEntrega(): void {
    if (!confirm('¿Estás seguro? Al aprobar se liberarán los fondos al creador.')) return;

    this.aprobando = true;
    this.error = '';

    this.entregableService.aprobarEntrega(this.idPedido).subscribe({
      next: () => {
        this.aprobando = false;
        this.successMsg = '¡Entrega aprobada! Fondos liberados al creador.';
        this.cargarEntregable();
        setTimeout(() => this.successMsg = '', 5000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al aprobar la entrega';
        this.aprobando = false;
      }
    });
  }

  descargarLimpia(): void {
    this.entregableService.descargarVersionLimpia(this.idPedido).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `entregable_${this.idPedido}_limpio`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.error = err.error?.message || 'El entregable no está disponible hasta que el pago sea liberado';
      }
    });
  }

  get esCreador(): boolean {
    return this.authService.hasAnyRole('CREADOR', 'ADMIN');
  }

  get esCliente(): boolean {
    return this.authService.hasAnyRole('CLIENTE', 'ADMIN');
  }
}
