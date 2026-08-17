import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { PedidoService } from '../../../pedido/services/pedido.service';
import { RespuestaPedidoResumido } from '../../../pedido/models/pedido.model';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { ServicioService } from '../../services/servicio.service';
import { ResenaService } from '../../services/resena.service';
import { RespuestaServicioResumido } from '../../models/creador.model';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import { formatPrice, esEtapaActiva, badgeEtapa, mensajeError } from '../../utils/formato';

@Component({
  selector: 'app-creador-overview',
  standalone: true,
  imports: [RouterLink, DecimalPipe, PerfilRequeridoComponent],
  templateUrl: './creador-overview.component.html',
  styleUrl: './creador-overview.component.css'
})
export class CreadorOverviewComponent implements OnInit {

  private pedidoService = inject(PedidoService);
  private servicioService = inject(ServicioService);
  private resenaService = inject(ResenaService);
  private contexto = inject(CreadorContextoService);

  readonly comisiones = signal<RespuestaPedidoResumido[]>([]);
  readonly servicios = signal<RespuestaServicioResumido[]>([]);
  readonly promedio = signal<number>(0);
  readonly totalResenas = signal<number>(0);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  readonly perfil = this.contexto.perfil;
  readonly perfilFaltante = this.contexto.perfilFaltante;

  nombreCreador = computed(() => {
    const p = this.perfil();
    return p ? `${p.nombresUsuario} ${p.apellidosUsuario}`.trim() : 'Creador';
  });

  comisionesActivas = computed(() => this.comisiones().filter(c => esEtapaActiva(c.etapaActual)).length);

  comisionesEntregadas = computed(() => this.comisiones().filter(c => !esEtapaActiva(c.etapaActual)).length);

  ingresosTotales = computed(() =>
    this.comisiones()
      .filter(c => !esEtapaActiva(c.etapaActual))
      .reduce((suma, c) => suma + (c.precioPactado || 0), 0)
  );

  ingresosEnCurso = computed(() =>
    this.comisiones()
      .filter(c => esEtapaActiva(c.etapaActual))
      .reduce((suma, c) => suma + (c.precioPactado || 0), 0)
  );

  serviciosActivos = computed(() => this.servicios().filter(s => s.estadoPublicacion === 'ACTIVO').length);

  serviciosBorrador = computed(() => this.servicios().filter(s => s.estadoPublicacion === 'BORRADOR').length);

  comisionesRecientes = computed(() =>
    this.comisiones()
      .slice()
      .sort((a, b) => new Date(b.fechaInicio).getTime() - new Date(a.fechaInicio).getTime())
      .slice(0, 5)
  );

  /** Comisiones cuya fecha estimada de entrega ya pasó y siguen abiertas. */
  entregasVencidas = computed(() => {
    const hoy = Date.now();
    return this.comisiones().filter(c =>
      esEtapaActiva(c.etapaActual) && c.fechaEntregaEstimada && new Date(c.fechaEntregaEstimada).getTime() < hoy
    ).length;
  });

  estrellas = computed(() => [1, 2, 3, 4, 5].map(n => n <= Math.round(this.promedio())));

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.isLoading.set(true);
    this.error.set('');

    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        if (!perfil) {
          this.isLoading.set(false);
          return;
        }
        forkJoin({
          comisiones: this.pedidoService.listarMisComisiones().pipe(catchError(() => of([] as RespuestaPedidoResumido[]))),
          servicios: this.servicioService.listarPorCreador(perfil.idPerfil).pipe(catchError(() => of([] as RespuestaServicioResumido[]))),
          promedio: this.resenaService.obtenerPromedio(perfil.idPerfil).pipe(catchError(() => of({} as Record<string, unknown>)))
        }).subscribe({
          next: ({ comisiones, servicios, promedio }) => {
            this.comisiones.set(comisiones);
            this.servicios.set(servicios);
            this.promedio.set(Number(promedio['promedio'] ?? promedio['promedioCalificacion'] ?? 0));
            this.totalResenas.set(Number(promedio['total'] ?? promedio['totalResenas'] ?? 0));
            this.isLoading.set(false);
          },
          error: (err) => {
            this.error.set(mensajeError(err, 'Error al cargar el resumen de tu actividad'));
            this.isLoading.set(false);
          }
        });
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar tu perfil de creador'));
        this.isLoading.set(false);
      }
    });
  }

  formatPrice = formatPrice;
  badgeEtapa = badgeEtapa;
}
