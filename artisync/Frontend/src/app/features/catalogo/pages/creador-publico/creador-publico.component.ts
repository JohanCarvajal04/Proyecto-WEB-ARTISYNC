import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Location, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of, catchError, map } from 'rxjs';
import { CatalogoPublicoService } from '../../services/catalogo-publico.service';
import { RespuestaPerfil, RespuestaServicioResumido } from '../../models/catalogo.model';
import { PortafolioService } from '../../../perfil/services/portafolio.service';
import { Portafolio, PortafolioItem } from '../../../perfil/models/portafolio.model';
import { ResenaService } from '../../../creador/services/resena.service';
import { SorteoPublicoService } from '../../../social/services/sorteo-publico.service';
import { RespuestaResena, RespuestaSorteo, RespuestaComentario, RespuestaEstadoLike } from '../../../social/models/social.model';
import { SeguidorService, RespuestaCreadorSeguidoNovedad, RespuestaSeguidorInfo } from '../../../social/services/seguidor.service';
import { ComentarioService } from '../../../social/services/comentario.service';
import { LikeService } from '../../../social/services/like.service';
import { ComentariosObraComponent } from '../../../social/components/comentarios-obra/comentarios-obra.component';
import { ToastService } from '../../../../core/services/toast.service';
import { CATALOGO_BASE_PATH } from '../../catalogo.config';
import { AuthService } from '../../../seguridad/services/auth.service';
import { exigirSesion } from '../../../../core/utils/exigir-sesion';

export type Pestana = 'portafolio' | 'servicios' | 'comisiones' | 'sorteos' | 'comunidad' | 'creadores_seguidos';

@Component({
  selector: 'app-creador-publico',
  standalone: true,
  imports: [RouterLink, ComentariosObraComponent, DecimalPipe],
  templateUrl: './creador-publico.component.html'
})
export class CreadorPublicoComponent implements OnInit {

  private location = inject(Location);
  private router = inject(Router);
  private catalogoService = inject(CatalogoPublicoService);
  private portafolioService = inject(PortafolioService);
  private resenaService = inject(ResenaService);
  private sorteoService = inject(SorteoPublicoService);
  private seguidorService = inject(SeguidorService);
  private comentarioService = inject(ComentarioService);
  private likeService = inject(LikeService);
  private toast = inject(ToastService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);

  /** Prefijo de los routerLink internos: '/explorar' o '/dashboard/explorar' según el montaje. */
  readonly base = inject(CATALOGO_BASE_PATH);

  readonly perfil = signal<RespuestaPerfil | null>(null);
  readonly servicios = signal<RespuestaServicioResumido[]>([]);
  readonly portafolio = signal<Portafolio | null>(null);
  readonly obras = signal<PortafolioItem[]>([]);
  readonly conteoComentarios = signal<Record<number, number>>({});
  readonly likesPorObra = signal<Record<number, RespuestaEstadoLike>>({});
  readonly procesandoLike = signal<number | null>(null);
  readonly obraSeleccionada = signal<PortafolioItem | null>(null);
  readonly comentariosRecientes = signal<Array<RespuestaComentario & { tituloObra: string }>>([]);
  readonly cargandoComentariosRecientes = signal<boolean>(false);
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
      next: (items) => {
        this.obras.set(items);
        this.cargarConteoComentarios(items);
        this.cargarComentariosRecientes(items);
        this.cargarLikes(items);
      },
      error: () => this.obras.set([])
    });

    this.catalogoService.registrarVisitaPortafolio(portafolio.idPortafolio).subscribe({
      next: () => {},
      error: () => {}
    });
  }

  /** Conteo de comentarios por obra, para el badge 💬 de cada tarjeta. */
  private cargarConteoComentarios(items: PortafolioItem[]): void {
    if (items.length === 0) return;

    forkJoin(
      items.map(item => this.comentarioService.contarComentarios(item.idItemPortafolio)
        .pipe(catchError(() => of({ idItemPortafolio: item.idItemPortafolio, total: 0 }))))
    ).subscribe(resultados => {
      const conteo: Record<number, number> = {};
      for (const r of resultados) conteo[r.idItemPortafolio] = r.total;
      this.conteoComentarios.set(conteo);
    });
  }

  /**
   * Últimos comentarios de todas las obras del portafolio, para que el
   * creador (y cualquier visitante) los vea sin tener que abrir obra por
   * obra. Se pide la primera página de cada ítem y se mezclan por fecha.
   */
  private cargarComentariosRecientes(items: PortafolioItem[]): void {
    if (items.length === 0) {
      this.comentariosRecientes.set([]);
      return;
    }

    this.cargandoComentariosRecientes.set(true);
    forkJoin(
      items.map(item => this.comentarioService.listarComentarios(item.idItemPortafolio, 0, 5)
        .pipe(
          map(pagina => pagina.contenido.map(c => ({ ...c, tituloObra: item.tituloObra }))),
          catchError(() => of([] as Array<RespuestaComentario & { tituloObra: string }>))
        ))
    ).subscribe(listas => {
      const todos = listas.flat()
        .sort((a, b) => new Date(b.fechaPublicacion).getTime() - new Date(a.fechaPublicacion).getTime())
        .slice(0, 8);
      this.comentariosRecientes.set(todos);
      this.cargandoComentariosRecientes.set(false);
    });
  }

  conteoComentariosDe(obra: PortafolioItem): number {
    return this.conteoComentarios()[obra.idItemPortafolio] ?? 0;
  }

  /** Estado de like por obra, para el corazón de cada tarjeta. */
  private cargarLikes(items: PortafolioItem[]): void {
    if (items.length === 0) return;

    forkJoin(
      items.map(item => this.likeService.obtenerEstado(item.idItemPortafolio)
        .pipe(catchError(() => of({ idItemPortafolio: item.idItemPortafolio, totalLikes: 0, meGusta: false } as RespuestaEstadoLike))))
    ).subscribe(resultados => {
      const likes: Record<number, RespuestaEstadoLike> = {};
      for (const r of resultados) likes[r.idItemPortafolio] = r;
      this.likesPorObra.set(likes);
    });
  }

  likeDe(obra: PortafolioItem): RespuestaEstadoLike {
    return this.likesPorObra()[obra.idItemPortafolio] ?? { idItemPortafolio: obra.idItemPortafolio, totalLikes: 0, meGusta: false };
  }

  toggleLike(obra: PortafolioItem, evento: Event): void {
    evento.stopPropagation();
    if (this.procesandoLike() !== null) return;

    const idItem = obra.idItemPortafolio;
    const actual = this.likeDe(obra);
    this.procesandoLike.set(idItem);

    const accion$ = actual.meGusta ? this.likeService.quitarLike(idItem) : this.likeService.darLike(idItem);
    accion$.subscribe({
      next: (estado) => {
        this.likesPorObra.update(mapa => ({ ...mapa, [idItem]: estado }));
        this.procesandoLike.set(null);
      },
      error: (err) => {
        this.procesandoLike.set(null);
        this.toast.error(err.error?.detail || err.error?.message || 'No se pudo actualizar el like');
      }
    });
  }

  totalComentarios = computed(() =>
    Object.values(this.conteoComentarios()).reduce((acc, n) => acc + n, 0)
  );

  verComentarios(obra: PortafolioItem): void {
    this.obraSeleccionada.set(obra);
  }

  /** Abre el modal de comentarios de la obra a la que pertenece un comentario reciente. */
  verComentariosDeObra(idItemPortafolio: number): void {
    const obra = this.obras().find(o => o.idItemPortafolio === idItemPortafolio);
    if (obra) this.verComentarios(obra);
  }

  cerrarComentarios(): void {
    this.obraSeleccionada.set(null);
    // Refresca conteos y recientes por si se publicó o borró algo en el modal.
    if (this.obras().length > 0) {
      this.cargarConteoComentarios(this.obras());
      this.cargarComentariosRecientes(this.obras());
    }
  }

  /**
   * Vuelve a la pantalla anterior (Explorar, Creadores, un servicio...), en
   * vez de mandar siempre a "Explorar" sin importar de dónde vino el usuario.
   * Si no hay historial dentro de la app (enlace directo, pestaña nueva), cae
   * al directorio de creadores en vez de dejar el botón sin efecto.
   */
  volver(): void {
    if (window.history.length > 1) {
      this.location.back();
    } else {
      this.router.navigate(['/dashboard/explorar/creadores']);
    }
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

  /**
   * El chat de verdad solo existe atado a un pedido (SalaChat.pedido es
   * obligatorio y único) — no hay mensajería directa sin comprar. En vez de
   * prometer un "Mensaje" que no existe, este botón lleva a donde sí se puede
   * abrir una conversación real: pedir uno de sus servicios.
   */
  contactarCreador(): void {
    const lista = this.servicios();
    if (lista.length === 0) {
      this.toast.error('Este creador todavía no tiene servicios publicados para solicitar.');
      return;
    }
    if (lista.length === 1) {
      this.router.navigate(['/pedido/crear'], { queryParams: { idServicio: lista[0].idServicio } });
      return;
    }
    this.cambiarPestana('servicios');
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

  /**
   * Antes devolvía el string fijo 'Ilustradora & Directora de Arte' para
   * cualquier creador: era texto de maqueta (Figma) que nunca se conectó al
   * dato real `tituloProfesional` del perfil, así que todos los creadores
   * mostraban la misma profesión. Ahora viene del campo real en
   * RespuestaPerfil; la plantilla lo trata como opcional para el creador que
   * aún no lo haya definido.
   */
  tituloProfesional = computed(() => this.perfil()?.tituloProfesional || '');

  biografiaText = computed(() => {
    const p = this.perfil();
    // Antes, si el creador no había escrito biografía, se mostraba un texto
    // de maqueta ("Creo mundos coloridos entre lo editorial y lo onírico...")
    // como si fuera suya. Ahora el vacío se comunica como tal.
    return p?.biografia || 'Este creador todavía no ha añadido una biografía.';
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
