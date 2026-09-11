import { Component, inject, input, signal, computed, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { CatalogoPublicoService } from '../../services/catalogo-publico.service';
import { SeguidorService, RespuestaEstadoSeguimiento } from '../../../social/services/seguidor.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { exigirSesion } from '../../../../core/utils/exigir-sesion';
import { RespuestaPerfil, RespuestaServicioResumido } from '../../models/catalogo.model';

interface CacheCreador {
  perfil: RespuestaPerfil;
  estadoSeguimiento: RespuestaEstadoSeguimiento;
  totalServicios: number;
}

/** Cache en memoria compartida para evitar peticiones repetidas al pasar el cursor */
const cacheCreadores = new Map<number, CacheCreador>();

@Component({
  selector: 'app-creador-popover',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="relative inline-block"
      (mouseenter)="mostrar()"
      (mouseleave)="programarCierre()">

      <!-- Trigger: Fila de autor con inicial/foto y nombre -->
      <a [routerLink]="[base(), 'creador', idPerfil()]"
        class="inline-flex items-center gap-2 text-xs font-semibold text-slate-600 hover:text-purple-700 transition-colors py-0.5 group/trigger">
        <span class="w-5 h-5 rounded-full bg-purple-700 text-white text-[10px] font-bold flex items-center justify-center shrink-0 overflow-hidden shadow-xs ring-1 ring-purple-200">
          @if (fotoPerfil()) {
            <img [src]="fotoPerfil()" [alt]="nombreMostrar()" class="w-full h-full object-cover" />
          } @else {
            {{ inicial() }}
          }
        </span>
        <span class="truncate group-hover/trigger:underline">{{ nombreMostrar() }}</span>
      </a>

      <!-- Popover de perfil de creador estilo Flowbite / Artisync -->
      @if (abierto()) {
        <div
          (mouseenter)="cancelarCierre()"
          (mouseleave)="programarCierre()"
          role="tooltip"
          class="absolute bottom-full left-0 mb-3 z-50 w-72 bg-white rounded-2xl shadow-2xl border border-purple-100/90 p-4 animate-[fadeIn_0.18s_ease-out] text-left text-slate-800 pointer-events-auto after:absolute after:top-full after:left-0 after:right-0 after:h-4 after:content-['']">

          <!-- Header con Avatar y Botón Seguir -->
          <div class="flex items-center justify-between gap-2 mb-3">
            <a [routerLink]="[base(), 'creador', idPerfil()]" class="relative group/avatar shrink-0">
              <div class="w-12 h-12 rounded-full bg-gradient-to-br from-purple-600 to-indigo-700 text-white font-bold text-base flex items-center justify-center overflow-hidden ring-2 ring-purple-100 shadow-sm">
                @if (fotoPerfil()) {
                  <img [src]="fotoPerfil()" [alt]="nombreMostrar()" class="w-full h-full object-cover group-hover/avatar:scale-105 transition-transform" />
                } @else {
                  {{ inicial() }}
                }
              </div>
              @if (perfil()?.identidadVerificada) {
                <span class="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-purple-600 text-white rounded-full flex items-center justify-center text-[9px] shadow" title="Identidad verificada">
                  ✓
                </span>
              }
            </a>

            <!-- Acción Seguir / Siguiendo -->
            @if (!esPropioPerfil()) {
              <div>
                @if (esSeguidor()) {
                  <button type="button" (click)="toggleSeguir($event)" [disabled]="procesandoSeguir()"
                    class="group/btn text-xs font-semibold px-3 py-1.5 rounded-xl bg-purple-50 hover:bg-rose-50 text-purple-700 hover:text-rose-600 border border-purple-200 hover:border-rose-200 shadow-2xs transition-all flex items-center gap-1.5 cursor-pointer">
                    @if (procesandoSeguir()) {
                      <span class="inline-block w-3 h-3 border-2 border-purple-700 border-t-transparent rounded-full animate-spin"></span>
                    } @else {
                      <svg class="w-3.5 h-3.5 group-hover/btn:hidden text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                      <span class="group-hover/btn:hidden">Siguiendo</span>
                      <span class="hidden group-hover/btn:inline text-rose-600">Dejar de seguir</span>
                    }
                  </button>
                } @else {
                  <button type="button" (click)="toggleSeguir($event)" [disabled]="procesandoSeguir()"
                    class="text-xs font-semibold px-3.5 py-1.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white shadow-xs hover:shadow transition-all flex items-center gap-1 cursor-pointer">
                    @if (procesandoSeguir()) {
                      <span class="inline-block w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                    } @else {
                      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
                      </svg>
                      <span>Seguir</span>
                    }
                  </button>
                }
              </div>
            } @else {
              <span class="text-[11px] font-semibold px-2.5 py-1 rounded-lg bg-slate-100 text-slate-600 border border-slate-200">Tu perfil</span>
            }
          </div>

          <!-- Nombre y Handle / Título -->
          <div class="mb-1">
            <a [routerLink]="[base(), 'creador', idPerfil()]"
              class="font-headline font-bold text-sm text-slate-900 hover:text-purple-700 transition-colors flex items-center gap-1.5">
              <span class="truncate">{{ nombreMostrar() }}</span>
              @if (perfil()?.identidadVerificada) {
                <svg class="w-3.5 h-3.5 text-purple-600 shrink-0" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                </svg>
              }
            </a>
            <p class="text-xs text-purple-700 font-medium truncate">
              @if (perfil()?.tituloProfesional) {
                {{ perfil()!.tituloProfesional }}
              } @else {
                &#64;{{ handle() }}
              }
            </p>
          </div>

          <!-- Biografía -->
          @if (cargando()) {
            <div class="space-y-1.5 my-2.5 animate-pulse">
              <div class="h-2.5 bg-slate-100 rounded w-full"></div>
              <div class="h-2.5 bg-slate-100 rounded w-4/5"></div>
            </div>
          } @else {
            <p class="text-xs text-slate-600 line-clamp-2 my-2 leading-relaxed">
              {{ perfil()?.biografia || 'Creador digital en Artisync.' }}
            </p>
          }

          <!-- Estadísticas (Seguidores y Servicios) -->
          <div class="flex items-center gap-4 text-xs pt-2.5 mt-2 border-t border-slate-100">
            <div class="flex items-center gap-1.5">
              <span class="font-bold text-slate-900 font-mono">{{ totalSeguidores() }}</span>
              <span class="text-slate-500 text-[11px]">Seguidores</span>
            </div>
            <div class="flex items-center gap-1.5">
              <span class="font-bold text-slate-900 font-mono">{{ totalServicios() }}</span>
              <span class="text-slate-500 text-[11px]">Servicios</span>
            </div>
          </div>

          <!-- Pie de tarjeta: Enlace rápido a perfil -->
          <div class="pt-2 mt-2 border-t border-slate-50">
            <a [routerLink]="[base(), 'creador', idPerfil()]"
              class="text-[11px] font-bold text-purple-700 hover:text-purple-900 flex items-center justify-between group/link">
              <span>Ver perfil y portafolio</span>
              <span class="group-hover/link:translate-x-0.5 transition-transform">→</span>
            </a>
          </div>

          <!-- Flecha indicadora (Popper Arrow) -->
          <div class="absolute -bottom-1.5 left-5 w-3 h-3 bg-white border-b border-r border-purple-100/90 rotate-45"></div>
        </div>
      }
    </div>
  `
})
export class CreadorPopoverComponent implements OnDestroy {

  private catalogoService = inject(CatalogoPublicoService);
  private seguidorService = inject(SeguidorService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  private router = inject(Router);

  // Parámetros de entrada
  readonly idPerfil = input.required<number>();
  readonly nombreCreador = input<string>('');
  readonly base = input<string>('/explorar');

  // Estado del popover
  readonly abierto = signal<boolean>(false);
  readonly perfil = signal<RespuestaPerfil | null>(null);
  readonly cargando = signal<boolean>(false);
  readonly esSeguidor = signal<boolean>(false);
  readonly totalSeguidores = signal<number>(0);
  readonly esPropioPerfil = signal<boolean>(false);
  readonly procesandoSeguir = signal<boolean>(false);
  readonly totalServicios = signal<number>(0);

  private timerCierre: any = null;

  readonly nombreMostrar = computed(() => {
    const p = this.perfil();
    if (p) {
      const nombreCompleto = `${p.nombresUsuario ?? ''} ${p.apellidosUsuario ?? ''}`.trim();
      if (nombreCompleto) return nombreCompleto;
    }
    return this.nombreCreador() || 'Creador';
  });

  readonly inicial = computed(() => {
    return (this.nombreMostrar() || '?').charAt(0).toUpperCase();
  });

  readonly fotoPerfil = computed(() => {
    return this.perfil()?.urlFotoPerfil || null;
  });

  readonly handle = computed(() => {
    const p = this.perfil();
    if (p?.urlRedSocial) {
      const limpio = p.urlRedSocial.replace(/^https?:\/\/(www\.)?[^/]+\/?/i, '');
      if (limpio) return limpio;
    }
    return (this.nombreMostrar() || 'creador')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, '');
  });

  ngOnDestroy(): void {
    if (this.timerCierre) {
      clearTimeout(this.timerCierre);
    }
  }

  mostrar(): void {
    if (this.timerCierre) {
      clearTimeout(this.timerCierre);
      this.timerCierre = null;
    }
    this.abierto.set(true);
    this.cargarDatos();
  }

  programarCierre(): void {
    if (this.timerCierre) {
      clearTimeout(this.timerCierre);
    }
    this.timerCierre = setTimeout(() => {
      this.abierto.set(false);
      this.timerCierre = null;
    }, 220);
  }

  cancelarCierre(): void {
    if (this.timerCierre) {
      clearTimeout(this.timerCierre);
      this.timerCierre = null;
    }
  }

  private cargarDatos(): void {
    const id = this.idPerfil();
    if (!id) return;

    // Verificar si ya está en cache
    const cache = cacheCreadores.get(id);
    if (cache) {
      this.perfil.set(cache.perfil);
      this.esSeguidor.set(cache.estadoSeguimiento.esSeguidor);
      this.totalSeguidores.set(cache.estadoSeguimiento.totalSeguidores || 0);
      this.esPropioPerfil.set(cache.estadoSeguimiento.esPropioPerfil);
      this.totalServicios.set(cache.totalServicios);
      return;
    }

    this.cargando.set(true);

    forkJoin({
      perfil: this.catalogoService.obtenerPerfilCreador(id).pipe(catchError(() => of(null))),
      estadoSeguimiento: this.seguidorService.obtenerEstado(id).pipe(
        catchError(() => of({ esSeguidor: false, totalSeguidores: 0, esPropioPerfil: false } as RespuestaEstadoSeguimiento))
      ),
      servicios: this.catalogoService.listarServiciosPorCreador(id, 'ACTIVO').pipe(
        catchError(() => of([] as RespuestaServicioResumido[]))
      )
    }).subscribe({
      next: ({ perfil, estadoSeguimiento, servicios }) => {
        if (perfil) {
          this.perfil.set(perfil);
        }
        this.esSeguidor.set(estadoSeguimiento.esSeguidor);
        this.totalSeguidores.set(estadoSeguimiento.totalSeguidores || 0);
        this.esPropioPerfil.set(estadoSeguimiento.esPropioPerfil);
        this.totalServicios.set(servicios.length);
        this.cargando.set(false);

        if (perfil) {
          cacheCreadores.set(id, {
            perfil,
            estadoSeguimiento,
            totalServicios: servicios.length
          });
        }
      },
      error: () => {
        this.cargando.set(false);
      }
    });
  }

  private actualizarCache(): void {
    const id = this.idPerfil();
    const p = this.perfil();
    if (id && p) {
      cacheCreadores.set(id, {
        perfil: p,
        estadoSeguimiento: {
          esSeguidor: this.esSeguidor(),
          totalSeguidores: this.totalSeguidores(),
          esPropioPerfil: this.esPropioPerfil()
        },
        totalServicios: this.totalServicios()
      });
    }
  }

  toggleSeguir(evento: Event): void {
    evento.preventDefault();
    evento.stopPropagation();

    const id = this.idPerfil();
    if (!id || this.esPropioPerfil() || this.procesandoSeguir()) return;

    if (!this.authService.isLoggedIn()) {
      this.toast.info('Debes iniciar sesión para interactuar con el creador.');
      const returnUrl = `${this.base()}/creador/${id}`;
      exigirSesion(this.authService, this.router, returnUrl, 'seguir');
      return;
    }

    this.procesandoSeguir.set(true);

    if (this.esSeguidor()) {
      this.seguidorService.dejarDeSeguir(id).subscribe({
        next: (resp) => {
          this.esSeguidor.set(false);
          this.totalSeguidores.set(resp.totalSeguidores);
          this.procesandoSeguir.set(false);
          this.actualizarCache();
          this.toast.success('Has dejado de seguir a este creador');
        },
        error: (err) => {
          this.procesandoSeguir.set(false);
          this.toast.error(err.error?.detail || err.error?.message || 'No se pudo dejar de seguir al creador');
        }
      });
    } else {
      this.seguidorService.seguir(id).subscribe({
        next: (resp) => {
          this.esSeguidor.set(true);
          this.totalSeguidores.set(resp.totalSeguidores);
          this.procesandoSeguir.set(false);
          this.actualizarCache();
          this.toast.success('¡Ahora sigues a este creador!');
        },
        error: (err) => {
          this.procesandoSeguir.set(false);
          this.toast.error(err.error?.detail || err.error?.message || 'No se pudo seguir al creador');
        }
      });
    }
  }
}
