import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { AvatarComponent } from '../../../../shared/components/avatar/avatar.component';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { PerfilCreadorService } from '../../services/perfil-creador.service';
import { RespuestaPerfil } from '../../models/creador.model';
import { SolicitudVerificacionComponent } from '../../../perfil/components/solicitud-verificacion/solicitud-verificacion.component';
import { mensajeError } from '../../utils/formato';
import { nombreUsuario } from '../../../../shared/utils/nombre-usuario';

@Component({
  selector: 'app-perfil-creador',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AvatarComponent, SolicitudVerificacionComponent],
  templateUrl: './perfil-creador.component.html'
})
export class PerfilCreadorComponent implements OnInit {

  private fb = inject(FormBuilder);
  private perfilService = inject(PerfilCreadorService);
  private contexto = inject(CreadorContextoService);
  private toast = inject(ToastService);
  authService = inject(AuthService);

  readonly perfil = signal<RespuestaPerfil | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly guardando = signal<boolean>(false);
  readonly error = signal<string>('');

  form: FormGroup = this.fb.group({
    biografia: ['', [Validators.maxLength(500)]],
    urlRedSocial: ['', [Validators.maxLength(255)]],
    tituloProfesional: ['', [Validators.maxLength(150)]]
  });

  nombreCompleto = computed(() => {
    const p = this.perfil();
    return nombreUsuario(
      p ? { nombres: p.nombresUsuario, apellidos: p.apellidosUsuario } : null,
      this.correo(),
      'Creador'
    );
  });

  correo = computed(() =>
    this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  roles = computed(() => this.authService.userRoles().map(r => r.replace('ROLE_', '')));

  totalPermisos = computed(() => this.authService.userPermissions().length);

  sesionExpira = computed(() => {
    const exp = this.authService.currentUser()?.exp;
    if (!exp) return null;
    return new Date(exp * 1000).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  });

  biografiaLength = computed(() => (this.form.get('biografia')?.value || '').length);

  esNuevo = computed(() => this.perfil() === null);

  ngOnInit(): void {
    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        if (perfil) {
          this.form.patchValue({
            biografia: perfil.biografia || '',
            urlRedSocial: perfil.urlRedSocial || '',
            tituloProfesional: perfil.tituloProfesional || ''
          });
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar tu perfil'));
        this.isLoading.set(false);
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.getRawValue();
    const datos = {
      biografia: val.biografia || null,
      urlRedSocial: val.urlRedSocial || null,
      tituloProfesional: val.tituloProfesional || null
    };

    this.guardando.set(true);

    const actual = this.perfil();
    if (actual) {
      this.perfilService.actualizar(actual.idPerfil, datos).subscribe({
        next: (perfil) => this.trasGuardar(perfil, 'Perfil actualizado'),
        error: (err) => {
          this.guardando.set(false);
          this.toast.error(mensajeError(err, 'No se pudo actualizar el perfil'));
        }
      });
    } else {
      const idUsuario = this.contexto.idUsuario();
      if (!idUsuario) {
        this.guardando.set(false);
        this.toast.error('No se pudo identificar tu usuario');
        return;
      }
      this.perfilService.crear({ idUsuario, ...datos }).subscribe({
        next: (perfil) => this.trasGuardar(perfil, 'Perfil de creador creado'),
        error: (err) => {
          this.guardando.set(false);
          this.toast.error(mensajeError(err, 'No se pudo crear el perfil'));
        }
      });
    }
  }

  private trasGuardar(perfil: RespuestaPerfil, mensaje: string): void {
    this.perfil.set(perfil);
    // El contexto lo consumen el resto de vistas del panel: hay que refrescarlo
    // para que dejen de mostrar el estado "perfil faltante".
    this.contexto.invalidar(perfil);
    this.guardando.set(false);
    this.toast.success(mensaje);
  }

  logout(): void {
    this.authService.logout();
  }
}
