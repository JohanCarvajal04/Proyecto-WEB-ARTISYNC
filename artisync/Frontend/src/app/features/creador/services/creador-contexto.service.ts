import { Injectable, inject, signal } from '@angular/core';
import { Observable, of, switchMap, tap, shareReplay, catchError, throwError } from 'rxjs';
import { UserService } from '../../perfil/services/user.service';
import { PerfilCreadorService } from './perfil-creador.service';
import { RespuestaPerfil } from '../models/creador.model';

/**
 * Resuelve —y cachea— el perfil de creador del usuario autenticado.
 *
 * Casi todos los endpoints del vendedor se indexan por `idPerfilCreador`, no por
 * id de usuario, así que cada vista necesitaría encadenar
 * `/usuarios/me` → `/v1/perfiles/usuario/{id}` antes de pedir sus datos. Este
 * servicio hace ese salto una sola vez por sesión y lo comparte.
 *
 * Si el creador todavía no ha creado su perfil el backend responde 404: eso no
 * es un error a mostrar, sino la señal de que hay que ofrecerle el alta
 * (`perfilFaltante`).
 */
@Injectable({ providedIn: 'root' })
export class CreadorContextoService {

  private userService = inject(UserService);
  private perfilService = inject(PerfilCreadorService);

  private perfil$: Observable<RespuestaPerfil | null> | null = null;

  /** Perfil ya resuelto, o `null` mientras no se haya cargado / no exista. */
  readonly perfil = signal<RespuestaPerfil | null>(null);
  /** `true` cuando el usuario está autenticado pero aún no tiene perfil de creador. */
  readonly perfilFaltante = signal<boolean>(false);
  /** Id de usuario del creador, necesario para dar de alta el perfil. */
  readonly idUsuario = signal<number | null>(null);

  /** Devuelve el perfil del creador (cacheado). Emite `null` si aún no existe. */
  obtenerPerfil(): Observable<RespuestaPerfil | null> {
    if (!this.perfil$) {
      this.perfil$ = this.userService.getCurrentUser().pipe(
        tap(usuario => this.idUsuario.set(usuario.idUsuario)),
        switchMap(usuario => this.perfilService.obtenerPorUsuario(usuario.idUsuario).pipe(
          catchError(err => {
            if (err?.status === 404) {
              this.perfilFaltante.set(true);
              return of(null);
            }
            return throwError(() => err);
          })
        )),
        tap(perfil => {
          this.perfil.set(perfil);
          this.perfilFaltante.set(perfil === null);
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.perfil$;
  }

  /** Fuerza una recarga: tras crear o editar el perfil desde el panel. */
  invalidar(perfil?: RespuestaPerfil): void {
    this.perfil$ = null;
    if (perfil) {
      this.perfil.set(perfil);
      this.perfilFaltante.set(false);
    }
  }
}
