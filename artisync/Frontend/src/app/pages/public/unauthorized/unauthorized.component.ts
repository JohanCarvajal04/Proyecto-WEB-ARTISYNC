import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/seguridad/services/auth.service';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  template: `
    <div class="min-h-screen bg-surface-container-low flex items-center justify-center p-4">
      <div class="bg-surface-container-lowest rounded-2xl p-8 max-w-md w-full shadow-sm border border-outline-variant flex flex-col items-center text-center gap-4">
        <div class="w-16 h-16 rounded-full bg-error-container text-error flex items-center justify-center">
          <span class="material-symbols-outlined text-[36px]">no_accounts</span>
        </div>
        <h1 class="font-headline text-2xl font-bold text-primary">Acceso Denegado</h1>
        <p class="text-sm text-on-surface-variant">
          No tienes los permisos de rol necesarios para acceder a esta sección de Artisync.
        </p>
        <button (click)="goBack()" class="mt-2 w-full bg-primary text-on-primary font-medium py-3 rounded-lg text-sm transition-all shadow-sm">
          Volver al Inicio
        </button>
      </div>
    </div>
  `
})
export class UnauthorizedComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  goBack(): void {
    if (this.authService.isLoggedIn()) {
      const role = this.authService.primaryRole();
      if (role === 'ADMINISTRADOR') {
        this.router.navigate(['/admin/users']);
      } else {
        this.router.navigate(['/admin/users']);
      }
    } else {
      this.router.navigate(['/auth/login']);
    }
  }
}
