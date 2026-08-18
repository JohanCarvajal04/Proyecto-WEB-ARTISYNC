import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/seguridad/services/auth.service';

/**
 * Último recurso: solo se llega aquí cuando el usuario no tiene NINGUNA página
 * accesible. Las denegaciones corrientes ya no aterrizan en esta pantalla —
 * authGuard devuelve al usuario a su inicio con un aviso, y un 403 de una
 * llamada suelta solo muestra el aviso sin moverlo de sitio.
 *
 * El único botón que había aquí llamaba a logout(), así que quedarse sin un
 * permiso cerraba la sesión. Ahora cerrar sesión es la acción secundaria y solo
 * se ofrece salir de verdad cuando no queda ningún destino.
 */
@Component({
  selector: 'app-unauthorized',
  standalone: true,
  template: `
    <div class="min-h-screen bg-surface-container-low flex items-center justify-center p-4">
      <div class="bg-surface-container-lowest rounded-2xl p-8 max-w-md w-full shadow-sm border border-outline-variant flex flex-col items-center text-center gap-4">
        <div class="w-16 h-16 rounded-full bg-error-container text-error flex items-center justify-center">
          <span class="material-symbols-outlined text-[36px]">no_accounts</span>
        </div>

        <h1 class="font-headline text-2xl font-bold text-primary">Sin acceso disponible</h1>

        @if (tieneDestino()) {
          <p class="text-sm text-on-surface-variant">
            No tienes permiso para abrir esa sección. Puedes seguir trabajando en el resto de la aplicación.
          </p>
        } @else {
          <p class="text-sm text-on-surface-variant">
            Tu cuenta no tiene ningún permiso asignado todavía, así que no hay secciones que mostrar.
            Pide a un administrador que revise los permisos de tu rol.
          </p>
        }

        <div class="flex flex-col gap-2 w-full mt-2">
          @if (tieneDestino()) {
            <button (click)="irAInicio()"
              class="w-full bg-primary text-on-primary font-medium py-3 rounded-lg text-sm transition-all shadow-sm">
              Volver a mi inicio
            </button>
            <button (click)="volverAtras()"
              class="w-full border border-outline-variant text-on-surface font-medium py-2.5 rounded-lg text-sm hover:bg-surface-container transition-colors">
              Volver atrás
            </button>
          }
          <button (click)="cerrarSesion()"
            class="w-full text-on-surface-variant font-medium py-2 rounded-lg text-xs hover:text-error transition-colors">
            Cerrar sesión
          </button>
        </div>
      </div>
    </div>
  `
})
export class UnauthorizedComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  /** Hay algún sitio al que devolver al usuario (su rol tiene al menos una página). */
  readonly tieneDestino = computed(() =>
    this.authService.isLoggedIn() && this.authService.homeRoute() !== '/no-autorizado'
  );

  irAInicio(): void {
    this.router.navigateByUrl(this.authService.homeRoute());
  }

  volverAtras(): void {
    history.back();
  }

  cerrarSesion(): void {
    this.authService.logout();
  }
}
