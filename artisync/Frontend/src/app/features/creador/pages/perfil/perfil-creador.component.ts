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

@Component({
  selector: 'app-perfil-creador',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AvatarComponent, SolicitudVerificacionComponent],
  templateUrl: './perfil-creador.component.html',
  styleUrl: './perfil-creador.component.css'
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
    urlRedSocial: ['', [Validators.maxLength(255)]]
  });

  nombreCompleto = computed(() => {
    const p = this.perfil();
    if (p) return `${p.nombresUsuario} ${p.apellidosUsuario}`.trim();
    const correo = this.authService.currentUser()?.email || this.authService.currentUser()?.sub || 'Creador';
    const prefijo = correo.split('@')[0];
    return prefijo.charAt(0).toUpperCase() + prefijo.slice(1);
  });

  correo = computed(() =>
    this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  roles = computed(() => this.authService.userRoles().map(r => r.replace('ROLE_', '')));

  biografiaLength = computed(() => (this.form.get('biografia')?.value || '').length);

  esNuevo = computed(() => this.perfil() === null);

  ngOnInit(): void {
    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        if (perfil) {
          this.form.patchValue({
            biografia: perfil.biografia || '',
            urlRedSocial: perfil.urlRedSocial || ''
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
      urlRedSocial: val.urlRedSocial || null
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
