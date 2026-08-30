import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../../seguridad/services/auth.service';
import { AvatarComponent } from '../../../../shared/components/avatar/avatar.component';
import { SolicitudVerificacionComponent } from '../../../perfil/components/solicitud-verificacion/solicitud-verificacion.component';
import { UserService } from '../../../perfil/services/user.service';
import { PaisService } from '../../../../shared/services/pais.service';
import { PaisResponse, UserResponse } from '../../../../shared/models/user.model';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-perfil-cliente',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, AvatarComponent, SolicitudVerificacionComponent],
  templateUrl: './perfil-cliente.component.html'
})
export class PerfilClienteComponent implements OnInit {
  authService = inject(AuthService);
  private userService = inject(UserService);
  private paisService = inject(PaisService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  readonly usuario = signal<UserResponse | null>(null);
  readonly paises = signal<PaisResponse[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly guardando = signal<boolean>(false);
  readonly editando = signal<boolean>(false);

  // Datos personales (nombres, apellidos, fecha de nacimiento, país): el
  // creador ya podía editar los suyos (biografía/red social) desde su propio
  // "Mi Perfil"; el cliente solo tenía una vista de solo lectura derivada del
  // email. El backend ya exponía PUT /usuarios/me para esto (lo usa
  // CompleteProfileModalComponent solo para el onboarding inicial), pero no
  // había ninguna pantalla para volver a editarlo después.
  form: FormGroup = this.fb.group({
    nombres: ['', [Validators.maxLength(100)]],
    apellidos: ['', [Validators.maxLength(100)]],
    fechaNacimiento: [''],
    idPais: [null]
  });

  userEmail = computed(() =>
    this.usuario()?.correo || this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  userName = computed(() => {
    const u = this.usuario();
    if (u && (u.nombres || u.apellidos)) {
      return `${u.nombres || ''} ${u.apellidos || ''}`.trim();
    }
    const email = this.userEmail();
    const prefix = email.split('@')[0];
    if (!prefix || prefix === '—') return 'Usuario';
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });

  userRole = computed(() => this.authService.primaryRole() || 'CLIENTE');

  roles = computed(() => this.authService.userRoles().map(r => r.replace('ROLE_', '')));

  totalPermisos = computed(() => this.authService.userPermissions().length);

  sesionExpira = computed(() => {
    const exp = this.authService.currentUser()?.exp;
    if (!exp) return null;
    return new Date(exp * 1000).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  });

  ngOnInit(): void {
    this.paisService.getPaisesActivos().subscribe({ next: (data) => this.paises.set(data) });

    this.userService.getCurrentUser().subscribe({
      next: (u) => {
        this.usuario.set(u);
        this.form.patchValue({
          nombres: u.nombres || '',
          apellidos: u.apellidos || '',
          fechaNacimiento: u.fechaNacimiento || '',
          idPais: u.idPais || null
        });
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  editar(): void {
    this.editando.set(true);
  }

  cancelarEdicion(): void {
    const u = this.usuario();
    if (u) {
      this.form.patchValue({
        nombres: u.nombres || '',
        apellidos: u.apellidos || '',
        fechaNacimiento: u.fechaNacimiento || '',
        idPais: u.idPais || null
      });
    }
    this.editando.set(false);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.getRawValue();
    this.guardando.set(true);
    this.userService.updateCurrentUser({
      nombres: val.nombres || undefined,
      apellidos: val.apellidos || undefined,
      fechaNacimiento: val.fechaNacimiento || undefined,
      idPais: val.idPais ? Number(val.idPais) : undefined
    }).subscribe({
      next: (u) => {
        this.usuario.set(u);
        this.guardando.set(false);
        this.editando.set(false);
        this.toast.success('Datos personales actualizados');
      },
      error: (err) => {
        this.guardando.set(false);
        this.toast.error(err.error?.message || 'No se pudieron guardar los cambios');
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
