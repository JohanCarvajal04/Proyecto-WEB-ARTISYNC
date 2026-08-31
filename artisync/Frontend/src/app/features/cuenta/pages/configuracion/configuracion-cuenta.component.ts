import { Component, inject, computed, signal, ElementRef, ViewChild, AfterViewInit, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../seguridad/services/auth.service';
import { UserService } from '../../../perfil/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AvatarComponent } from '../../../../shared/components/avatar/avatar.component';
import { UserResponse } from '../../../../shared/models/user.model';
import { nombreUsuario } from '../../../../shared/utils/nombre-usuario';

/**
 * Configuración de la cuenta: identidad, roles/permisos vigentes y accesos de
 * seguridad (contraseña, 2FA). Las preferencias de notificación se muestran
 * deshabilitadas hasta que exista un endpoint que las persista.
 *
 * Es la página única de configuración personal, alcanzable sin ningún permiso
 * desde cualquier panel (admin, creador, cliente, cuenta) — ver cuenta.routes.ts
 * y los ítems "Mi Cuenta" en nav.config.ts.
 *
 * Muestra explícitamente "Permisos activos: 0" en vez de esconder el dato:
 * es la explicación de por qué no ve más secciones, sin tener que adivinarlo.
 */
@Component({
  selector: 'app-configuracion-cuenta',
  standalone: true,
  imports: [CommonModule, AvatarComponent],
  templateUrl: './configuracion-cuenta.component.html'
})
export class ConfiguracionCuentaComponent implements OnInit, AfterViewInit {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  /** Referencia al input oculto: el ítem "Cambiar foto de perfil" del menú de usuario llega aquí con ?accion=cambiar-foto y lo abre solo. */
  @ViewChild('fileInput') private fileInputRef?: ElementRef<HTMLInputElement>;

  readonly isLoading = signal<boolean>(true);
  readonly isSubmitting = signal<boolean>(false);
  readonly userProfile = signal<UserResponse | null>(null);
  readonly fotoAmpliada = signal<boolean>(false);

  userEmail = computed(() =>
    this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  userName = computed(() => nombreUsuario(this.userProfile(), this.userEmail()));

  roles = computed(() => this.authService.userRoles().map(r => r.replace('ROLE_', '')));

  totalPermisos = computed(() => this.authService.userPermissions().length);

  sinPermisos = computed(() => this.totalPermisos() === 0);

  sesionExpira = computed(() => {
    const exp = this.authService.currentUser()?.exp;
    if (!exp) return null;
    return new Date(exp * 1000).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  ngAfterViewInit(): void {
    if (this.route.snapshot.queryParamMap.get('accion') === 'cambiar-foto') {
      // setTimeout: dejar que termine este ciclo de detección de cambios antes
      // de abrir el selector nativo, para no interferir con el renderizado inicial.
      setTimeout(() => this.fileInputRef?.nativeElement.click());
    }
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.userService.getCurrentUser().subscribe({
      next: (profile) => {
        this.userProfile.set(profile);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('Error al cargar la información del perfil');
        this.isLoading.set(false);
      }
    });
  }

  openPasswordModal(): void {
    this.router.navigate(['/profile/change-password']);
  }

  toggle2FA(): void {
    this.router.navigate(['/profile/two-factor']);
  }

  logout(): void {
    this.authService.logout();
  }

  ampliarFoto(): void {
    if (this.userProfile()?.urlFotoPerfil) {
      this.fotoAmpliada.set(true);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.isSubmitting.set(true);
      this.userService.uploadProfilePicture(file).subscribe({
        next: (profile) => {
          this.userProfile.set(profile);
          this.toastService.success('Foto de perfil actualizada correctamente');
          this.isSubmitting.set(false);
        },
        error: () => {
          this.toastService.error('Error al subir la foto de perfil');
          this.isSubmitting.set(false);
        }
      });
    }
  }
}
