import { Component, inject, computed, signal, ViewChild, AfterViewInit, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../../seguridad/services/auth.service';
import { UserService } from '../../../perfil/services/user.service';
import { PaisService } from '../../../../shared/services/pais.service';
import { ToastService } from '../../../../core/services/toast.service';
import { PaisResponse, UserResponse } from '../../../../shared/models/user.model';
import { nombreUsuario } from '../../../../shared/utils/nombre-usuario';

/**
 * Configuración de la cuenta: identidad, datos personales, roles/permisos
 * vigentes y accesos de seguridad (contraseña, 2FA). Los datos personales
 * (nombres, apellidos, fecha de nacimiento, país) se editan aquí para
 * cualquier rol — antes solo el cliente tenía dónde hacerlo, desde su propio
 * "Mi Perfil". Las preferencias de notificación se muestran deshabilitadas
 * hasta que exista un endpoint que las persista.
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
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './configuracion-cuenta.component.html'
})
export class ConfiguracionCuentaComponent implements OnInit, AfterViewInit {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private paisService = inject(PaisService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  readonly isLoading = signal<boolean>(true);
  readonly userProfile = signal<UserResponse | null>(null);

  /** REQ-NF-018: confirmación explícita antes de la supresión — es irreversible y el correo actual queda libre. */
  readonly isConfirmSupresionOpen = signal<boolean>(false);
  readonly suprimiendoDatos = signal<boolean>(false);

  /** REQ-NF-018 (ajuste de seguimiento): step-up de 2FA — solo se exige si el usuario lo tiene activo. */
  readonly requiere2FaParaSuprimir = computed(() => this.userProfile()?.dosFactoresHabilitado === true);
  supresionForm: FormGroup = this.fb.group({
    codigo: ['']
  });



  userEmail = computed(() =>
    this.userProfile()?.correo || this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  userName = computed(() => nombreUsuario(this.userProfile(), this.userEmail()));

  totalPermisos = computed(() => this.authService.userPermissions().length);

  sinPermisos = computed(() => this.totalPermisos() === 0);

  ngOnInit(): void {
    this.loadProfile();
  }

  ngAfterViewInit(): void {
    // Left empty for now, as photo editing moved to profile
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

  /** REQ-NF-018: abre la confirmación — el texto de advertencia vive en el template, junto al resto de "Zona de peligro". */
  abrirConfirmSupresion(): void {
    this.isConfirmSupresionOpen.set(true);
  }

  cerrarConfirmSupresion(): void {
    this.isConfirmSupresionOpen.set(false);
    this.supresionForm.reset();
  }

  confirmarSupresionDatos(): void {
    if (this.requiere2FaParaSuprimir() && !this.supresionForm.value.codigo) {
      this.toastService.error('Ingresa tu código de autenticación de dos factores para continuar');
      return;
    }

    this.suprimiendoDatos.set(true);
    this.userService.solicitarSupresionDatos(this.supresionForm.value.codigo || undefined).subscribe({
      next: (res) => {
        this.suprimiendoDatos.set(false);
        this.isConfirmSupresionOpen.set(false);
        this.toastService.success(res.mensaje || res.message || 'Datos personales suprimidos');
        // El backend ya revocó todas las sesiones; cerrar sesión localmente también.
        this.authService.logout();
      },
      error: (err) => {
        this.suprimiendoDatos.set(false);
        // El diálogo se deja abierto (no se cierra ni se resetea el código) para
        // que, si el 2FA fue lo que falló, el usuario pueda corregirlo sin
        // reiniciar todo el flujo de confirmación.
        this.toastService.error(err.error?.detail || err.error?.message || 'No se pudo suprimir los datos personales');
      }
    });
  }
}
