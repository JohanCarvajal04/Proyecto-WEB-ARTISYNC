import { Component, inject, computed, signal, ViewChild, AfterViewInit, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../../seguridad/services/auth.service';
import { UserService } from '../../../perfil/services/user.service';
import { PaisService } from '../../../../shared/services/pais.service';
import { ToastService } from '../../../../core/services/toast.service';
import { IdentityCardComponent } from '../../../../shared/components/identity-card/identity-card.component';
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
  imports: [CommonModule, ReactiveFormsModule, IdentityCardComponent],
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

  /** El ítem "Cambiar foto de perfil" del menú de usuario llega aquí con ?accion=cambiar-foto y abre el selector de la tarjeta de identidad solo. */
  @ViewChild(IdentityCardComponent) private identityCard?: IdentityCardComponent;

  readonly isLoading = signal<boolean>(true);
  readonly userProfile = signal<UserResponse | null>(null);
  readonly paises = signal<PaisResponse[]>([]);
  readonly editandoDatos = signal<boolean>(false);
  readonly guardandoDatos = signal<boolean>(false);

  form: FormGroup = this.fb.group({
    nombres: ['', [Validators.maxLength(100)]],
    apellidos: ['', [Validators.maxLength(100)]],
    fechaNacimiento: [''],
    idPais: [null]
  });

  userEmail = computed(() =>
    this.userProfile()?.correo || this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  userName = computed(() => nombreUsuario(this.userProfile(), this.userEmail()));

  totalPermisos = computed(() => this.authService.userPermissions().length);

  sinPermisos = computed(() => this.totalPermisos() === 0);

  ngOnInit(): void {
    this.paisService.getPaisesActivos().subscribe({ next: (data) => this.paises.set(data) });
    this.loadProfile();
  }

  ngAfterViewInit(): void {
    if (this.route.snapshot.queryParamMap.get('accion') === 'cambiar-foto') {
      // setTimeout: dejar que termine este ciclo de detección de cambios antes
      // de abrir el selector nativo, para no interferir con el renderizado inicial.
      setTimeout(() => this.identityCard?.abrirSelectorFoto());
    }
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.userService.getCurrentUser().subscribe({
      next: (profile) => {
        this.userProfile.set(profile);
        this.patchDatosPersonales(profile);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('Error al cargar la información del perfil');
        this.isLoading.set(false);
      }
    });
  }

  private patchDatosPersonales(u: UserResponse): void {
    this.form.patchValue({
      nombres: u.nombres || '',
      apellidos: u.apellidos || '',
      fechaNacimiento: u.fechaNacimiento || '',
      idPais: u.idPais || null
    });
  }

  editarDatos(): void {
    this.editandoDatos.set(true);
  }

  cancelarEdicionDatos(): void {
    const u = this.userProfile();
    if (u) this.patchDatosPersonales(u);
    this.editandoDatos.set(false);
  }

  guardarDatosPersonales(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.getRawValue();
    this.guardandoDatos.set(true);
    this.userService.updateCurrentUser({
      nombres: val.nombres || undefined,
      apellidos: val.apellidos || undefined,
      fechaNacimiento: val.fechaNacimiento || undefined,
      idPais: val.idPais ? Number(val.idPais) : undefined
    }).subscribe({
      next: (u) => {
        this.userProfile.set(u);
        this.guardandoDatos.set(false);
        this.editandoDatos.set(false);
        this.toastService.success('Datos personales actualizados');
      },
      error: (err) => {
        this.guardandoDatos.set(false);
        this.toastService.error(err.error?.message || 'No se pudieron guardar los cambios');
      }
    });
  }

  onFotoActualizada(profile: UserResponse): void {
    this.userProfile.set(profile);
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
}
