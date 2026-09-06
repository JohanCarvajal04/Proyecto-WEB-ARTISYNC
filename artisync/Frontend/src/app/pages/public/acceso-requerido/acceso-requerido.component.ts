import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MotivoAccesoRequerido } from '../../../core/utils/exigir-sesion';

const TEXTOS_MOTIVO: Record<MotivoAccesoRequerido, string> = {
  contratar: 'Debes iniciar sesión para interactuar con el creador y solicitar comisiones.',
  seguir: 'Debes iniciar sesión para seguir a este creador e interactuar.',
  sorteo: 'Debes iniciar sesión para participar en este sorteo.'
};

const TEXTO_GENERICO = 'Debes iniciar sesión para interactuar con el creador.';

@Component({
  selector: 'app-acceso-requerido',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl p-8 max-w-md w-full shadow-sm border border-slate-200/80 flex flex-col items-center text-center gap-4">
        <div class="w-16 h-16 rounded-full bg-purple-50 text-purple-700 flex items-center justify-center">
          <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>

        <h1 class="font-headline text-2xl font-bold text-slate-900">Iniciar sesión requerido</h1>

        <p class="text-sm text-slate-600 leading-relaxed">{{ mensaje() }}</p>

        <div class="flex flex-col gap-2.5 w-full mt-2">
          <a [routerLink]="['/auth/login']" [queryParams]="{ returnUrl: returnUrl() }"
            class="w-full bg-purple-700 hover:bg-purple-800 text-white font-bold py-3 rounded-xl text-sm transition-all shadow-sm">
            Iniciar sesión
          </a>
          <a [routerLink]="['/auth/register']" [queryParams]="{ returnUrl: returnUrl() }"
            class="w-full border border-slate-200 text-slate-700 font-bold py-2.5 rounded-xl text-sm hover:bg-purple-50 hover:text-purple-700 hover:border-purple-200 transition-colors">
            Crear cuenta
          </a>
          <a routerLink="/explorar"
            class="w-full text-slate-500 font-medium py-2 rounded-lg text-xs hover:text-purple-700 transition-colors">
            Volver a Explorar
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
