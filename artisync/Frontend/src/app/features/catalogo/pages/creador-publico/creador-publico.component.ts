import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { CatalogoPublicoService } from '../../services/catalogo-publico.service';
import { RespuestaPerfil, RespuestaServicioResumido } from '../../models/catalogo.model';
import { PortafolioService } from '../../../perfil/services/portafolio.service';
import { Portafolio, PortafolioItem } from '../../../perfil/models/portafolio.model';
import { ResenaService } from '../../../creador/services/resena.service';
import { SorteoPublicoService } from '../../../social/services/sorteo-publico.service';
import { RespuestaResena, RespuestaSorteo } from '../../../social/models/social.model';
import { SeguidorService, RespuestaCreadorSeguidoNovedad, RespuestaSeguidorInfo } from '../../../social/services/seguidor.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CATALOGO_BASE_PATH } from '../../catalogo.config';
import { AuthService } from '../../../seguridad/services/auth.service';
import { exigirSesion } from '../../../../core/utils/exigir-sesion';

export type Pestana = 'portafolio' | 'servicios' | 'comisiones' | 'sorteos' | 'comunidad' | 'creadores_seguidos';

@Component({
  selector: 'app-creador-publico',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './creador-publico.component.html'
})
export class CreadorPublicoComponent implements OnInit {

  private catalogoService = inject(CatalogoPublicoService);
  private portafolioService = inject(PortafolioService);
  private resenaService = inject(ResenaService);
  private sorteoService = inject(SorteoPublicoService);
  private seguidorService = inject(SeguidorService);
  private toast = inject(ToastService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  /** Prefijo de los routerLink internos: '/explorar' o '/dashboard/explorar' según el montaje. */
  readonly base = inject(CATALOGO_BASE_PATH);

  readonly perfil = signal<RespuestaPerfil | null>(null);
  readonly servicios = signal<RespuestaServicioResumido[]>([]);
  readonly portafolio = signal<Portafolio | null>(null);
  readonly obras = signal<PortafolioItem[]>([]);
  readonly resenas = signal<RespuestaResena[]>([]);
  readonly promedio = signal<number | null>(null);
  readonly sorteos = signal<RespuestaSorteo[]>([]);
  readonly seguidoresList = signal<RespuestaSeguidorInfo[]>([]);
  readonly creadoresSeguidosNovedades = signal<RespuestaCreadorSeguidoNovedad[]>([]);

  // Estado de seguimiento y propio perfil
  readonly esSeguidor = signal<boolean>(false);
  readonly totalSeguidores = signal<number>(0);
  readonly esPropioPerfil = signal<boolean>(false);
  readonly procesandoSeguir = signal<boolean>(false);

  readonly pestana = signal<Pestana>('portafolio');
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  ngOnInit(): void {
    const idPerfilParam = this.route.snapshot.paramMap.get('idPerfil');
    if (idPerfilParam) {
      this.cargar(Number(idPerfilParam));
    }
  }

  private cargar(idPerfil: number): void {
    this.isLoading.set(true);
    this.error.set('');

    forkJoin({
      perfil: this.catalogoService.obtenerPerfilCreador(idPerfil),
      servicios: this.catalogoService.listarServiciosPorCreador(idPerfil, 'ACTIVO')
        .pipe(catchError(() => of([] as RespuestaServicioResumido[]))),
      portafolio: this.portafolioService.obtenerPorPerfil(idPerfil).pipe(catchError(() => of(null))),
      resenas: this.resenaService.listarPorCreador(idPerfil).pipe(catchError(() => of([] as RespuestaResena[]))),
      promedio: this.resenaService.obtenerPromedio(idPerfil).pipe(catchError(() => of({} as Record<string, unknown>))),
      sorteos: this.sorteoService.listarPorCreador(idPerfil).pipe(catchError(() => of([] as RespuestaSorteo[]))),
      estadoSeguimiento: this.seguidorService.obtenerEstado(idPerfil).pipe(catchError(() => of({ esSeguidor: false, totalSeguidores: 0, esPropioPerfil: false }))),
      seguidores: this.seguidorService.listarSeguidores(idPerfil).pipe(catchError(() => of([] as RespuestaSeguidorInfo[]))),
      // Endpoint autenticado (@PreAuthorize("isAuthenticated()")): un visitante
      // sin sesión no tiene "creadores seguidos" que consultar, y llamarlo de
      // todas formas solo generaría un 401 sin sentido en cada visita.
      novedadesSiguiendo: this.authService.isLoggedIn()
        ? this.seguidorService.listarNovedadesSiguiendo().pipe(catchError(() => of([] as RespuestaCreadorSeguidoNovedad[])))
        : of([] as RespuestaCreadorSeguidoNovedad[])
    }).subscribe({
      next: ({ perfil, servicios, portafolio, resenas, promedio, sorteos, estadoSeguimiento, seguidores, novedadesSiguiendo }) => {
        this.perfil.set(perfil);
        this.servicios.set(servicios);
        this.portafolio.set(portafolio);
        this.resenas.set(resenas);

        // Sin reseñas propias no hay calificación que mostrar. Antes caía a un
        // 4.9 fijo — en una página pública eso es una calificación falsa a la
        // vista de cualquiera, no un dato de maqueta inofensivo.
        const valor = Number(promedio['promedio'] ?? 0);
        this.promedio.set(valor > 0 ? valor : null);
        this.sorteos.set(sorteos);

        this.esSeguidor.set(estadoSeguimiento.esSeguidor);
        this.totalSeguidores.set(estadoSeguimiento.totalSeguidores || 0);
        this.esPropioPerfil.set(estadoSeguimiento.esPropioPerfil);

        this.seguidoresList.set(seguidores);
        this.creadoresSeguidosNovedades.set(novedadesSiguiendo);

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

    this.catalogoService.registrarVisitaPortafolio(portafolio.idPortafolio).subscribe({
      next: () => {},
      error: () => {}
    });
  }

  toggleSeguir(): void {
    const p = this.perfil();
    if (!p || this.esPropioPerfil() || this.procesandoSeguir()) return;

    const returnUrl = `${this.base}/creador/${p.idPerfil}`;
    if (!exigirSesion(this.authService, this.router, returnUrl, 'seguir')) return;

    this.procesandoSeguir.set(true);

    if (this.esSeguidor()) {
      this.seguidorService.dejarDeSeguir(p.idPerfil).subscribe({
        next: (resp) => {
          this.esSeguidor.set(false);
          this.totalSeguidores.set(resp.totalSeguidores);
          this.procesandoSeguir.set(false);
          this.toast.success('Has dejado de seguir a este creador');
        },
        error: () => this.procesandoSeguir.set(false)
      });
    } else {
      this.seguidorService.seguir(p.idPerfil).subscribe({
        next: (resp) => {
          this.esSeguidor.set(true);
          this.totalSeguidores.set(resp.totalSeguidores);
          this.procesandoSeguir.set(false);
          this.toast.success('¡Ahora sigues a este creador!');
        },
        error: () => this.procesandoSeguir.set(false)
      });
    }
  }

  cambiarPestana(pestana: Pestana): void {
    this.pestana.set(pestana);
  }

  nombreCompleto = computed(() => {
    const p = this.perfil();
    if (!p) return '';
    return `${p.nombresUsuario ?? ''} ${p.apellidosUsuario ?? ''}`.trim();
  });

  handleText = computed(() => {
    const nombre = this.nombreCompleto().toLowerCase().replace(/\s+/g, '');
    return `@${nombre || 'creador'}`;
  });

  tituloProfesional = computed(() => {
    return 'Ilustradora & Directora de Arte';
  });

  biografiaText = computed(() => {
    const p = this.perfil();
    return p?.biografia || 'Creo mundos coloridos entre lo editorial y lo onírico. Especializada en ilustración de personaje, identidad visual y dirección creativa para marcas que quieren destacar.';
  });

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

  formatNumber(num: number): string {
    if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'k';
    }
    return num.toString();
  }
}
