import { Component, ElementRef, EventEmitter, Output, ViewChild, computed, inject, input, signal } from '@angular/core';
import { AuthService } from '../../../features/seguridad/services/auth.service';
import { UserService } from '../../../features/perfil/services/user.service';
import { ToastService } from '../../../core/services/toast.service';
import { AvatarComponent } from '../avatar/avatar.component';
import { UserResponse } from '../../models/user.model';

/** Espejo de PoliticaArchivo.PERFIL (backend): solo imagen, sin SVG, 5 MB. */
const TIPOS_FOTO_PERMITIDOS = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];
const MAX_BYTES_FOTO = 5 * 1024 * 1024;

/**
 * Tarjeta de identidad de la cuenta: banner + avatar + nombre/email + resumen
 * de roles/permisos/sesión. Antes estaba duplicada línea por línea en Mi
 * Cuenta, Mi Perfil (Creador) y Mi Perfil (Cliente); ahora vive solo en Mi
 * Cuenta, que es la única página que sigue mostrando este nivel de detalle
 * de cuenta (los perfiles usan la cabecera simplificada, app-profile-header).
 */
@Component({
  selector: 'app-identity-card',
  standalone: true,
  imports: [AvatarComponent],
  templateUrl: './identity-card.component.html'
})
export class IdentityCardComponent {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private toastService = inject(ToastService);

  nombre = input.required<string>();
  correo = input.required<string>();
  fotoUrl = input<string | null | undefined>(null);
  /** Habilita el flujo de cambio de foto (hoy solo lo usa Mi Cuenta). */
  permitirEditarFoto = input<boolean>(false);

  @Output() fotoActualizada = new EventEmitter<UserResponse>();

  @ViewChild('fileInput') private fileInputRef?: ElementRef<HTMLInputElement>;

  readonly subiendoFoto = signal<boolean>(false);
  readonly fotoAmpliada = signal<boolean>(false);

  roles = computed(() => this.authService.userRoles().map(r => r.replace('ROLE_', '')));
  totalPermisos = computed(() => this.authService.userPermissions().length);
  sesionExpira = computed(() => {
    const exp = this.authService.currentUser()?.exp;
    if (!exp) return null;
    return new Date(exp * 1000).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  });

  abrirSelectorFoto(): void {
    this.fileInputRef?.nativeElement.click();
  }

  ampliarFoto(): void {
    if (this.fotoUrl()) {
      this.fotoAmpliada.set(true);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      if (!TIPOS_FOTO_PERMITIDOS.includes(file.type)) {
        this.toastService.error(`Formato no soportado: ${file.type || 'desconocido'}. Se acepta JPG, PNG, WEBP o GIF.`);
        input.value = '';
        return;
      }
      if (file.size > MAX_BYTES_FOTO) {
        this.toastService.error('La foto supera el máximo de 5 MB.');
        input.value = '';
        return;
      }

      this.subiendoFoto.set(true);
      this.userService.uploadProfilePicture(file).subscribe({
        next: (profile) => {
          this.subiendoFoto.set(false);
          this.toastService.success('Foto de perfil actualizada correctamente');
          this.fotoActualizada.emit(profile);
        },
        error: (err) => {
          this.subiendoFoto.set(false);
          this.toastService.error(err.error?.detail || 'Error al subir la foto de perfil');
        }
      });
      input.value = '';
    }
  }
}
