import { Component, ElementRef, HostListener, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AvatarComponent } from '../avatar/avatar.component';

/**
 * Reemplaza la "pill" estática de usuario del header (avatar + nombre + rol)
 * por un menú desplegable: antes esos datos no eran clicables y llegar a "Mi
 * Cuenta" o cerrar sesión exigía pasar por el sidebar. Compartido entre
 * DashboardLayoutComponent (admin) y ClientDashboardLayoutComponent (cliente
 * y creador) porque la pill era idéntica en ambos, solo cambiaba el color del
 * rol.
 */
@Component({
  selector: 'app-user-menu',
  standalone: true,
  imports: [RouterLink, AvatarComponent],
  template: `
    <div class="relative">
      <button type="button" (click)="toggle()"
        class="bg-white rounded-full pl-3 pr-2.5 py-1.5 flex items-center gap-2.5 shadow-sm hover:bg-slate-50 transition-colors">
        <app-avatar [name]="userName()" [imageUrl]="imageUrl()" size="sm"></app-avatar>
        <div class="text-left leading-tight pr-0.5">
          <p class="text-xs font-bold text-slate-800">{{ userName() }}</p>
          <p class="text-[10px] font-medium uppercase tracking-wider" [class]="roleClass()">{{ userRole() }}</p>
        </div>
        <svg class="w-3.5 h-3.5 text-slate-400 transition-transform shrink-0" [class.rotate-180]="abierto()"
          fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      @if (abierto()) {
        <div class="absolute right-0 top-[calc(100%+0.5rem)] w-60 bg-white rounded-2xl shadow-lg border border-slate-100 overflow-hidden z-50 animate-[fadeIn_0.15s_ease-out]">
          <a routerLink="/cuenta/configuracion" (click)="cerrar()"
            class="flex items-center gap-3 px-4 py-3 text-sm text-slate-700 hover:bg-slate-50 transition-colors">
            <svg class="w-4.5 h-4.5 text-slate-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M17.982 18.725A7.488 7.488 0 0012 15.75a7.488 7.488 0 00-5.982 2.975m11.963 0a9 9 0 10-11.963 0m11.963 0A8.966 8.966 0 0112 21a8.966 8.966 0 01-5.982-2.275M15 9.75a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Mi Cuenta
          </a>
          <a [routerLink]="['/cuenta/configuracion']" [queryParams]="{ accion: 'cambiar-foto' }" (click)="cerrar()"
            class="flex items-center gap-3 px-4 py-3 text-sm text-slate-700 hover:bg-slate-50 transition-colors">
            <svg class="w-4.5 h-4.5 text-slate-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Cambiar foto de perfil
          </a>
          <div class="border-t border-slate-100"></div>
          <button type="button" (click)="cerrarSesion()"
            class="w-full flex items-center gap-3 px-4 py-3 text-sm text-rose-600 hover:bg-rose-50 transition-colors text-left">
            <svg class="w-4.5 h-4.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Cerrar sesión
          </button>
        </div>
      }
    </div>
  `
})
export class UserMenuComponent {
  private elementRef = inject(ElementRef);

  userName = input.required<string>();
  userRole = input.required<string>();
  imageUrl = input<string | null | undefined>(null);
  /** Clase Tailwind para el color del rol: cada panel usa el acento de su propio tema. */
  roleClass = input<string>('text-slate-400');

  logout = output<void>();

  readonly abierto = signal(false);

  toggle(): void {
    this.abierto.update(v => !v);
  }

  cerrar(): void {
    this.abierto.set(false);
  }

  cerrarSesion(): void {
    this.cerrar();
    this.logout.emit();
  }

  /** Cierra al hacer clic fuera; sin esto el menú se queda abierto tras navegar dentro del mismo layout. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.abierto() && !this.elementRef.nativeElement.contains(event.target)) {
      this.cerrar();
    }
  }
}
