import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/seguridad/services/auth.service';
import { SolicitudVerificacionComponent } from '../../../features/perfil/components/solicitud-verificacion/solicitud-verificacion.component';

@Component({
  selector: 'app-force-verification-modal',
  standalone: true,
  imports: [SolicitudVerificacionComponent],
  templateUrl: './force-verification-modal.component.html'
})
export class ForceVerificationModalComponent {
  @Output() actionTaken = new EventEmitter<void>();

  private router = inject(Router);
  private authService = inject(AuthService);

  readonly solicitudCompletada = signal<boolean>(false);

  onSuccess(): void {
    const userId = this.authService.getCurrentUserId();
    if (userId) {
      localStorage.setItem(`verificacion_pendiente_${userId}`, 'true');
    }
    this.solicitudCompletada.set(true);
  }

  continuar(): void {
    this.actionTaken.emit();
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
    this.actionTaken.emit();
  }
}
