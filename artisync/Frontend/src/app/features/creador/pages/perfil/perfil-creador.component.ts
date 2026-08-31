import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ProfileHeaderComponent } from '../../../../shared/components/profile-header/profile-header.component';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { PerfilCreadorService } from '../../services/perfil-creador.service';
import { PortafolioService } from '../../../perfil/services/portafolio.service';
import { RespuestaPerfil } from '../../models/creador.model';
import {
  Portafolio,
  OpcionesPersonalizacion,
  COLORES_POR_DEFECTO,
  CAMPOS_COLOR_PORTAFOLIO
} from '../../../perfil/models/portafolio.model';
import { SolicitudVerificacionComponent } from '../../../perfil/components/solicitud-verificacion/solicitud-verificacion.component';
import { mensajeError } from '../../utils/formato';
import { nombreUsuario } from '../../../../shared/utils/nombre-usuario';

@Component({
  selector: 'app-perfil-creador',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, RouterLink, ProfileHeaderComponent, SolicitudVerificacionComponent],
  templateUrl: './perfil-creador.component.html'
})
export class PerfilCreadorComponent implements OnInit {

  private fb = inject(FormBuilder);
  private perfilService = inject(PerfilCreadorService);
  private contexto = inject(CreadorContextoService);
  private portafolioService = inject(PortafolioService);
  private toast = inject(ToastService);
  authService = inject(AuthService);

  readonly perfil = signal<RespuestaPerfil | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly guardando = signal<boolean>(false);
  readonly error = signal<string>('');

  // Personalización del portafolio (colores + visibilidad). Antes vivía
  // exclusivamente en la página de Portafolio; se traslada aquí porque es
  // configuración de "cómo me presento", no de gestión de obras.
  readonly portafolio = signal<Portafolio | null>(null);
  readonly cargandoPortafolio = signal<boolean>(true);
  readonly guardandoPersonalizacion = signal<boolean>(false);
  esPublico = false;
  colores: OpcionesPersonalizacion = { ...COLORES_POR_DEFECTO };
  readonly camposColor = CAMPOS_COLOR_PORTAFOLIO;

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
          this.cargarPortafolio(perfil.idPerfil);
        } else {
          this.cargandoPortafolio.set(false);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar tu perfil'));
        this.isLoading.set(false);
        this.cargandoPortafolio.set(false);
      }
    });
  }

  private cargarPortafolio(idPerfil: number): void {
    this.cargandoPortafolio.set(true);
    this.portafolioService.obtenerPorPerfil(idPerfil).pipe(
      catchError((err) => {
        // 404 = el creador todavía no ha abierto su portafolio.
        if (err?.status === 404) return of(null);
        throw err;
      })
    ).subscribe({
      next: (portafolio) => {
        this.portafolio.set(portafolio);
        if (portafolio) {
          this.esPublico = portafolio.esPublico;
          this.colores = { ...COLORES_POR_DEFECTO, ...(portafolio.opcionesPersonalizacion || {}) };
        }
        this.cargandoPortafolio.set(false);
      },
      error: () => this.cargandoPortafolio.set(false)
    });
  }

  guardarPersonalizacion(): void {
    const portafolio = this.portafolio();
    if (!portafolio) return;

    this.guardandoPersonalizacion.set(true);
    this.portafolioService.actualizar(portafolio.idPortafolio, {
      esPublico: this.esPublico,
      opcionesPersonalizacion: this.colores
    }).subscribe({
      next: (actualizado) => {
        this.portafolio.set(actualizado);
        this.guardandoPersonalizacion.set(false);
        this.toast.success('Personalización actualizada');
      },
      error: (err) => {
        this.guardandoPersonalizacion.set(false);
        this.toast.error(mensajeError(err, 'No se pudo guardar la personalización'));
      }
    });
  }

  restaurarColores(): void {
    this.colores = { ...COLORES_POR_DEFECTO };
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
          if (err?.status === 409) {
            // El backend dice que ya existe un perfil para este usuario, pero
            // el `perfil` local seguía en null: la caché compartida de
            // CreadorContextoService (usada por el resto del panel) quedó
            // desactualizada respecto al backend, típicamente porque una
            // creación anterior sí llegó a completarse en el servidor sin que
            // este componente recibiera la respuesta (red, reintento tras un
            // refresh de token, etc.). En vez de dejar al usuario atascado en
            // un formulario de "Crear perfil" que siempre va a fallar, se
            // fuerza una recarga y se pasa al modo edición con los datos reales.
            this.recuperarPerfilExistente();
          } else {
            this.toast.error(mensajeError(err, 'No se pudo crear el perfil'));
          }
        }
      });
    }
  }

  /** Descarta la caché de perfil y vuelve a cargarlo tras un 409 al crear. */
  private recuperarPerfilExistente(): void {
    this.contexto.invalidar();
    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        if (!perfil) return;
        this.perfil.set(perfil);
        this.form.patchValue({
          biografia: perfil.biografia || '',
          urlRedSocial: perfil.urlRedSocial || '',
          tituloProfesional: perfil.tituloProfesional || ''
        });
        this.cargarPortafolio(perfil.idPerfil);
        this.toast.success('Ya tenías un perfil creado: se cargó aquí para que puedas editarlo.');
      }
    });
  }

  private trasGuardar(perfil: RespuestaPerfil, mensaje: string): void {
    this.perfil.set(perfil);
    // El contexto lo consumen el resto de vistas del panel: hay que refrescarlo
    // para que dejen de mostrar el estado "perfil faltante".
    this.contexto.invalidar(perfil);
    this.guardando.set(false);
    this.toast.success(mensaje);
    if (this.portafolio() === null) {
      this.cargarPortafolio(perfil.idPerfil);
    }
  }
}
