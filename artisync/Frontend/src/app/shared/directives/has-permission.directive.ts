import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';
import { AuthService } from '../../features/seguridad/services/auth.service';

/**
 * Muestra el contenido solo si el usuario tiene alguno de los permisos dados.
 *
 *   <button *appHasPermission="'USUARIO_CREAR'">Nuevo usuario</button>
 *   <button *appHasPermission="['USUARIO_EDITAR', 'USUARIO_SUSPENDER']">…</button>
 *
 * Reemplaza el `@if (hasPermission('X') || primaryRole() === 'ADMINISTRADOR')`
 * que estaba repetido por las plantillas. El bypass de ADMIN va incluido, igual
 * que en authGuard y en los @PreAuthorize del backend, para que las tres capas
 * decidan lo mismo.
 *
 * Es reactiva: al renovarse el token con permisos distintos, la vista se
 * actualiza sin recargar.
 */
@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective {
  private templateRef = inject(TemplateRef<unknown>);
  private viewContainer = inject(ViewContainerRef);
  private auth = inject(AuthService);

  readonly appHasPermission = input.required<string | readonly string[]>();

  /** Evita destruir y recrear la vista en cada evaluación si nada ha cambiado. */
  private visible: boolean | null = null;

  constructor() {
    effect(() => {
      const requerido = this.appHasPermission();
      const codigos = Array.isArray(requerido) ? requerido : [requerido as string];

      const permitido =
        this.auth.hasAnyPermission(...codigos) ||
        this.auth.userRoles().some(r => r === 'ADMIN' || r === 'ADMINISTRADOR' || r === 'ROLE_ADMIN');

      if (permitido === this.visible) return;
      this.visible = permitido;

      this.viewContainer.clear();
      if (permitido) {
        this.viewContainer.createEmbeddedView(this.templateRef);
      }
    });
  }
}
