import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { CatalogoPublicoService } from '../../services/catalogo-publico.service';
import { RespuestaPerfil, RespuestaServicioResumido } from '../../models/catalogo.model';
import { PortafolioService } from '../../../perfil/services/portafolio.service';
import { Portafolio, PortafolioItem } from '../../../perfil/models/portafolio.model';
import { ResenaService } from '../../../creador/services/resena.service';
import { SorteoPublicoService } from '../../../social/services/sorteo-publico.service';
import { RespuestaResena, RespuestaSorteo } from '../../../social/models/social.model';
import { SeguidorService, RespuestaEstadoSeguimiento } from '../../../social/services/seguidor.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ComentariosObraComponent } from '../../../social/components/comentarios-obra/comentarios-obra.component';

type Pestana = 'servicios' | 'portafolio' | 'resenas' | 'sorteos';

@Component({
  selector: 'app-creador-publico',
  standalone: true,
  imports: [RouterLink, ComentariosObraComponent],
  templateUrl: './creador-publico.component.html'
})
export class CreadorPublicoComponent implements OnInit {

  private catalogoService = inject(CatalogoPublicoService);
  private portafolioService = inject(PortafolioService);
  private resenaService = inject(ResenaService);
  private sorteoService = inject(SorteoPublicoService);
  private seguidorService = inject(SeguidorService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);

  readonly perfil = signal<RespuestaPerfil | null>(null);
  readonly servicios = signal<RespuestaServicioResumido[]>([]);
  readonly portafolio = signal<Portafolio | null>(null);
  readonly obras = signal<PortafolioItem[]>([]);
  readonly resenas = signal<RespuestaResena[]>([]);
  readonly promedio = signal<number | null>(null);
  readonly sorteos = signal<RespuestaSorteo[]>([]);
  readonly estadoSeguimiento = signal<RespuestaEstadoSeguimiento | null>(null);
  readonly isLogueado = signal<boolean>(false);

  readonly pestana = signal<Pestana>('servicios');
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  ngOnInit(): void {
    const idPerfil = Number(this.route.snapshot.paramMap.get('idPerfil'));
    this.isLogueado.set(this.authService.isLoggedIn());
    this.cargar(idPerfil);
  }

  private cargar(idPerfil: number): void {
    this.isLoading.set(true);
    this.error.set('');

    // Solo el perfil es obligatorio: un creador puede no tener portafolio,
    // reseñas ni sorteos, y un 404 en esos casos no es un fallo de la vista.
    forkJoin({
      perfil: this.catalogoService.obtenerPerfilCreador(idPerfil),
      servicios: this.catalogoService.listarServiciosPorCreador(idPerfil, 'ACTIVO')
        .pipe(catchError(() => of([] as RespuestaServicioResumido[]))),
      portafolio: this.portafolioService.obtenerPorPerfil(idPerfil).pipe(catchError(() => of(null))),
      resenas: this.resenaService.listarPorCreador(idPerfil).pipe(catchError(() => of([] as RespuestaResena[]))),
      promedio: this.resenaService.obtenerPromedio(idPerfil).pipe(catchError(() => of({} as Record<string, unknown>))),
      sorteos: this.sorteoService.listarPorCreador(idPerfil).pipe(catchError(() => of([] as RespuestaSorteo[]))),
      estadoSeguimiento: this.seguidorService.obtenerEstado(idPerfil).pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ perfil, servicios, portafolio, resenas, promedio, sorteos, estadoSeguimiento }) => {
        this.perfil.set(perfil);
        this.servicios.set(servicios);
        this.portafolio.set(portafolio);
        this.resenas.set(resenas);
        const valor = Number(promedio['promedio'] ?? 0);
        this.promedio.set(valor > 0 ? valor : null);
        this.sorteos.set(sorteos);
        if (estadoSeguimiento) this.estadoSeguimiento.set(estadoSeguimiento);
        this.isLoading.set(false);

        if (portafolio) this.cargarPortafolio(portafolio);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo cargar el perfil del creador');
        this.isLoading.set(false);
      }
    });
  }

  private cargarPortafolio(portafolio: Portafolio): void {
    this.portafolioService.listarItems(portafolio.idPortafolio).subscribe({
      next: (items) => this.obras.set(items),
      error: () => this.obras.set([])
    });

    // Contador de visitas del portafolio; que falle no afecta a la vista.
    this.catalogoService.registrarVisitaPortafolio(portafolio.idPortafolio).subscribe({
      next: () => {},
      error: () => {}
    });
  }

  cambiarPestana(pestana: Pestana): void {
    this.pestana.set(pestana);
  }

  toggleSeguir(): void {
    const p = this.perfil();
    if (!p) return;
    
    const estado = this.estadoSeguimiento();
    const isSiguiendo = estado?.estaSiguiendo || false;
    
    if (isSiguiendo) {
      this.seguidorService.dejarDeSeguir(p.idPerfil).subscribe({
        next: () => {
          this.estadoSeguimiento.update(e => e ? { estaSiguiendo: false, cantidadSeguidores: Math.max(0, e.cantidadSeguidores - 1) } : null);
        }
      });
    } else {
      this.seguidorService.seguir(p.idPerfil).subscribe({
        next: () => {
          this.estadoSeguimiento.update(e => e ? { estaSiguiendo: true, cantidadSeguidores: e.cantidadSeguidores + 1 } : { estaSiguiendo: true, cantidadSeguidores: 1 });
        }
      });
    }
  }

  nombreCompleto(): string {
    const p = this.perfil();
    if (!p) return '';
    return `${p.nombresUsuario ?? ''} ${p.apellidosUsuario ?? ''}`.trim();
  }

  iniciales(): string {
    const nombre = this.nombreCompleto();
    if (!nombre) return '?';
    return nombre.split(/\s+/).slice(0, 2).map(p => p.charAt(0).toUpperCase()).join('');
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
