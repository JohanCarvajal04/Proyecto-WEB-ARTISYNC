import { Component, input, computed } from '@angular/core';
import { NAV_ICON_PATHS, NAV_ICON_FALLBACK } from './nav-icon.paths';

/**
 * Icono de trazo (outline) de 24x24, resuelto por nombre lógico contra
 * NAV_ICON_PATHS. Sustituye la cadena de `@if/@else if` que antes se repetía,
 * con variaciones incompletas, dentro de cada layout.
 *
 * El input se llama `svgClass` y no `class`: un input llamado `class` chocaría
 * con el atributo `class` del propio host.
 */
@Component({
  selector: 'app-nav-icon',
  standalone: true,
  template: `
    <svg [class]="svgClass()" fill="none" viewBox="0 0 24 24"
         stroke="currentColor" stroke-width="2" aria-hidden="true" focusable="false">
      <path stroke-linecap="round" stroke-linejoin="round" [attr.d]="d()" />
    </svg>
  `
})
export class NavIconComponent {
  readonly name = input.required<string>();
  readonly svgClass = input<string>('w-5 h-5');

  protected readonly d = computed(() => NAV_ICON_PATHS[this.name()] ?? NAV_ICON_FALLBACK);
}
