import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MotivoAccesoRequerido } from '../../../core/utils/exigir-sesion';

const TEXTOS_MOTIVO: Record<MotivoAccesoRequerido, string> = {
  contratar: 'Para contratar este servicio necesitas iniciar sesión.',
  seguir: 'Para seguir a este creador necesitas iniciar sesión.',
  sorteo: 'Para participar en este sorteo necesitas iniciar sesión.'
};

const TEXTO_GENERICO = 'Necesitas iniciar sesión para continuar.';

/**
 * Muro de acceso para las acciones del catálogo público (contratar, seguir,
 * participar en un sorteo). No es un guard de ruta: el catálogo (`/explorar`,
 * la ficha de un servicio, el perfil de un creador) es público y sigue
 * navegable sin sesión. Esta pantalla solo se pisa cuando el visitante intenta
 * una acción que sí requiere cuenta — ver `exigirSesion()`.
 *
 * Los enlaces de login y registro propagan `returnUrl`, así que tras
 * autenticarse el usuario vuelve exactamente al punto que intentaba alcanzar
 * (LoginComponent y RegisterComponent ya honran ese query param).
 */
@Component({
  selector: 'app-acceso-requerido',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-3xl p-8 max-w-md w-full shadow-sm border border-slate-100 flex flex-col items-center text-center gap-4">
        <div class="w-16 h-16 rounded-full bg-teal-50 text-teal-600 flex items-center justify-center">
          <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>

        <h1 class="font-headline text-2xl font-bold text-[#0F2027]">Necesitas una cuenta</h1>

        <p class="text-sm text-slate-500">{{ mensaje() }}</p>

        <div class="flex flex-col gap-2 w-full mt-2">
          <a [routerLink]="['/auth/login']" [queryParams]="{ returnUrl: returnUrl() }"
            class="w-full bg-gradient-to-r from-[#0F9B8E] to-[#20B2AA] hover:from-[#0D8A7E] hover:to-[#1A9E98] text-white font-semibold py-3 rounded-full text-sm transition-all shadow-sm">
            Iniciar sesión
          </a>
          <a [routerLink]="['/auth/register']" [queryParams]="{ returnUrl: returnUrl() }"
            class="w-full border border-slate-200 text-slate-700 font-medium py-2.5 rounded-full text-sm hover:bg-slate-50 transition-colors">
            Crear cuenta
          </a>
          <a routerLink="/explorar"
            class="w-full text-slate-400 font-medium py-2 rounded-lg text-xs hover:text-teal-600 transition-colors">
            Seguir explorando
          </a>
        </div>
      </div>
    </div>
  `
})
export class AccesoRequeridoComponent {
  private route = inject(ActivatedRoute);

  readonly returnUrl = computed(() => this.route.snapshot.queryParamMap.get('returnUrl') || '/explorar');

  readonly mensaje = computed(() => {
    const motivo = this.route.snapshot.queryParamMap.get('motivo') as MotivoAccesoRequerido | null;
    return (motivo && TEXTOS_MOTIVO[motivo]) || TEXTO_GENERICO;
  });
}
