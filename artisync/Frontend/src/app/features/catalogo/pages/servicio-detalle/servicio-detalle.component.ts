import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { of, catchError, forkJoin } from 'rxjs';
import { CatalogoPublicoService } from '../../services/catalogo-publico.service';
import { RespuestaServicio } from '../../models/catalogo.model';
// Las lecturas públicas de reseñas ya viven en el módulo del creador; se
// reutilizan en vez de duplicar los mismos dos endpoints.
import { ResenaService } from '../../../creador/services/resena.service';
import { RespuestaResena } from '../../../social/models/social.model';
import { CATALOGO_BASE_PATH } from '../../catalogo.config';
import { AuthService } from '../../../seguridad/services/auth.service';
import { exigirSesion } from '../../../../core/utils/exigir-sesion';

@Component({
  selector: 'app-servicio-detalle',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './servicio-detalle.component.html'
})
export class ServicioDetalleComponent implements OnInit {

  private catalogoService = inject(CatalogoPublicoService);
  private resenaService = inject(ResenaService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  /** Prefijo de los routerLink internos: '/explorar' o '/dashboard/explorar' según el montaje. */
  readonly base = inject(CATALOGO_BASE_PATH);

  readonly servicio = signal<RespuestaServicio | null>(null);
  readonly resenas = signal<RespuestaResena[]>([]);
  readonly promedio = signal<number | null>(null);

  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar(id);
  }

  private cargar(idServicio: number): void {
    this.isLoading.set(true);
    this.error.set('');

    this.catalogoService.obtenerServicio(idServicio).subscribe({
      next: (servicio) => {
        this.servicio.set(servicio);
        this.isLoading.set(false);
        this.cargarReputacion(servicio.idPerfilCreador);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo cargar el servicio');
        this.isLoading.set(false);
      }
    });
  }

  /** La reputación del creador es contexto, no bloquea la ficha si falla. */
  private cargarReputacion(idPerfil: number): void {
    forkJoin({
      resenas: this.resenaService.listarPorCreador(idPerfil).pipe(catchError(() => of([] as RespuestaResena[]))),
      promedio: this.resenaService.obtenerPromedio(idPerfil).pipe(catchError(() => of({} as Record<string, unknown>)))
    }).subscribe(({ resenas, promedio }) => {
      this.resenas.set(resenas);
      const valor = Number(promedio['promedio'] ?? 0);
      this.promedio.set(valor > 0 ? valor : null);
    });
  }

  solicitarServicio(): void {
    const servicio = this.servicio();
    if (!servicio) return;

    const returnUrl = `/pedido/crear?idServicio=${servicio.idServicio}`;
    if (!exigirSesion(this.authService, this.router, returnUrl, 'contratar')) return;

    this.router.navigate(['/pedido/crear'], { queryParams: { idServicio: servicio.idServicio } });
  }

  estrellas(calificacion: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < calificacion);
  }

  formatPrice(precio: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(precio);
  }

  formatDate(fecha: string): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
