import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
  templateUrl: './acceso-requerido.component.html',
  styleUrls: ['./acceso-requerido.component.css']
})
export class AccesoRequeridoComponent {
  private route = inject(ActivatedRoute);

  readonly returnUrl = computed(() => this.route.snapshot.queryParamMap.get('returnUrl') || '/explorar');

  readonly mensaje = computed(() => {
    const motivo = this.route.snapshot.queryParamMap.get('motivo') as MotivoAccesoRequerido | null;
    return (motivo && TEXTOS_MOTIVO[motivo]) || TEXTO_GENERICO;
  });
}
