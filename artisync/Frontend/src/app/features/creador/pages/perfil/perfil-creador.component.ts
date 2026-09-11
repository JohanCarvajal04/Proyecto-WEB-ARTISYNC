import { Component, inject, signal, computed, OnInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { PerfilCreadorService } from '../../services/perfil-creador.service';
import { PortafolioService } from '../../../perfil/services/portafolio.service';
import { UserService } from '../../../perfil/services/user.service';
import { PaisService } from '../../../../shared/services/pais.service';
import { SeguidorService } from '../../../social/services/seguidor.service';
import { ServicioService } from '../../services/servicio.service';
import { RespuestaPerfil } from '../../models/creador.model';
import { PaisResponse, UserResponse } from '../../../../shared/models/user.model';
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
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink, SolicitudVerificacionComponent],
  templateUrl: './perfil-creador.component.html'
})
export class PerfilCreadorComponent implements OnInit {

  private fb = inject(FormBuilder);
  private perfilService = inject(PerfilCreadorService);
  private contexto = inject(CreadorContextoService);
  private portafolioService = inject(PortafolioService);
  private userService = inject(UserService);
  private paisService = inject(PaisService);
  private seguidorService = inject(SeguidorService);
  private servicioService = inject(ServicioService);
  private toast = inject(ToastService);
  authService = inject(AuthService);

  @ViewChild('fileInputFoto') fileInputFoto?: ElementRef<HTMLInputElement>;
  @ViewChild('fileInputPortada') fileInputPortada?: ElementRef<HTMLInputElement>;

  readonly perfil = signal<RespuestaPerfil | null>(null);
  readonly userProfile = signal<UserResponse | null>(null);
  readonly paises = signal<PaisResponse[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly guardando = signal<boolean>(false);
  readonly error = signal<string>('');

  // Datos personales (trasladados de Configuración)
  readonly editandoDatosPersonales = signal<boolean>(false);
  readonly guardandoDatosPersonales = signal<boolean>(false);

  // Foto y portada
  readonly subiendoFoto = signal<boolean>(false);
  readonly editandoPortada = signal<boolean>(false);
  readonly guardandoPortada = signal<boolean>(false);
  readonly modoSubidaPortada = signal<'archivo' | 'url'>('archivo');
  readonly archivoPortadaSeleccionado = signal<File | null>(null);
  readonly previewPortadaUrl = signal<string>('');
  readonly inputUrlPortada = signal<string>('');

  // Personalización del portafolio (colores + visibilidad).
  readonly portafolio = signal<Portafolio | null>(null);
  readonly cargandoPortafolio = signal<boolean>(true);
  readonly guardandoPersonalizacion = signal<boolean>(false);
  esPublico = false;
  colores: OpcionesPersonalizacion = { ...COLORES_POR_DEFECTO };
  readonly camposColor = CAMPOS_COLOR_PORTAFOLIO;

  formPersonales: FormGroup = this.fb.group({
    nombres: ['', [Validators.required, Validators.maxLength(100)]],
    apellidos: ['', [Validators.required, Validators.maxLength(100)]],
    fechaNacimiento: [''],
    idPais: [null]
  });

  form: FormGroup = this.fb.group({
    biografia: ['', [Validators.maxLength(500)]],
    urlRedSocial: ['', [Validators.maxLength(255)]],
    tituloProfesional: ['', [Validators.maxLength(150)]]
  });

  nombreCompleto = computed(() => {
    const u = this.userProfile();
    const p = this.perfil();
    const nombres = u?.nombres || p?.nombresUsuario;
    const apellidos = u?.apellidos || p?.apellidosUsuario;
    return nombreUsuario(
      (nombres || apellidos) ? { nombres: nombres || '', apellidos: apellidos || '' } : null,
      this.correo(),
      'Creador'
    );
  });

  correo = computed(() =>
    this.userProfile()?.correo || this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  biografiaLength = computed(() => (this.form.get('biografia')?.value || '').length);

  esNuevo = computed(() => this.perfil() === null);

  iniciales = computed(() => {
    const n = this.nombreCompleto();
    if (!n) return 'CR';
    const partes = n.trim().split(/\s+/);
    if (partes.length === 1) return partes[0].substring(0, 2).toUpperCase();
    return (partes[0][0] + partes[1][0]).toUpperCase();
  });

  urlFoto = computed(() => this.userProfile()?.urlFotoPerfil || this.perfil()?.urlFotoPerfil);
  urlPortada = computed(() => this.perfil()?.urlPortada);

  totalSeguidores = signal<number>(0);
  paisNombre = computed(() => this.userProfile()?.nombrePais);

  edad = computed(() => {
    const f = this.userProfile()?.fechaNacimiento;
    if (!f) return null;
    const b = new Date(f);
    const now = new Date();
    let age = now.getFullYear() - b.getFullYear();
    const m = now.getMonth() - b.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age--;
    return age > 0 ? age : null;
  });

  ngOnInit(): void {
    this.cargarDatosUsuario();
    this.paisService.getPaisesActivos().subscribe({
      next: (data) => this.paises.set(data)
    });

    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        if (perfil) {
          this.form.patchValue({
            biografia: perfil.biografia || '',
            urlRedSocial: perfil.urlRedSocial || '',
            tituloProfesional: perfil.tituloProfesional || ''
          });
          this.inputUrlPortada.set(perfil.urlPortada || '');
          this.cargarPortafolio(perfil.idPerfil);
          this.seguidorService.listarSeguidores(perfil.idPerfil).subscribe({
            next: (s) => this.totalSeguidores.set(s.length),
            error: () => this.totalSeguidores.set(0)
          });
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

  cargarDatosUsuario(): void {
    this.userService.getCurrentUser().subscribe({
      next: (u) => {
        this.userProfile.set(u);
        this.formPersonales.patchValue({
          nombres: u.nombres || '',
          apellidos: u.apellidos || '',
          fechaNacimiento: u.fechaNacimiento || '',
          idPais: u.idPais || null
        });
      }
    });
  }

  abrirSelectorFoto(): void {
    this.fileInputFoto?.nativeElement.click();
  }

  onFotoSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const tiposValidos = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];
      if (!tiposValidos.includes(file.type)) {
        this.toast.error('Formato no soportado. Se acepta JPG, PNG, WEBP o GIF.');
        input.value = '';
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        this.toast.error('La foto supera el máximo permitido de 5 MB.');
        input.value = '';
        return;
      }

      this.subiendoFoto.set(true);
      this.userService.uploadProfilePicture(file).subscribe({
        next: (usuario) => {
          this.subiendoFoto.set(false);
          this.userProfile.set(usuario);
          const p = this.perfil();
          if (p) {
            this.perfil.set({ ...p, urlFotoPerfil: usuario.urlFotoPerfil });
          }
          this.toast.success('Foto de perfil actualizada correctamente');
        },
        error: (err) => {
          this.subiendoFoto.set(false);
          this.toast.error(err.error?.detail || err.error?.message || 'Error al subir la foto de perfil');
        }
      });
      input.value = '';
    }
  }

  toggleEditarPortada(): void {
    const actual = this.urlPortada() || '';
    this.inputUrlPortada.set(actual);
    this.previewPortadaUrl.set(actual);
    this.archivoPortadaSeleccionado.set(null);
    this.modoSubidaPortada.set('archivo');
    this.editandoPortada.update(v => !v);
  }

  abrirSelectorArchivoPortada(): void {
    this.fileInputPortada?.nativeElement.click();
  }

  onArchivoPortadaSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const tiposValidos = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];
      if (!tiposValidos.includes(file.type)) {
        this.toast.error('Formato no soportado. Se acepta JPG, PNG, WEBP o GIF.');
        input.value = '';
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        this.toast.error('La imagen supera el límite de 5 MB.');
        input.value = '';
        return;
      }

      this.archivoPortadaSeleccionado.set(file);
      const reader = new FileReader();
      reader.onload = () => {
        this.previewPortadaUrl.set(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  onUrlPortadaCambiado(url: string): void {
    this.inputUrlPortada.set(url);
    this.previewPortadaUrl.set(url);
  }

  guardarPortada(): void {
    if (this.modoSubidaPortada() === 'archivo') {
      const file = this.archivoPortadaSeleccionado();
      if (!file) {
        this.toast.error('Por favor selecciona un archivo de imagen primero.');
        return;
      }
      this.guardandoPortada.set(true);
      this.servicioService.subirMiniatura(file).subscribe({
        next: (res) => {
          this.aplicarPortadaEnBackend(res.url);
        },
        error: (err) => {
          this.guardandoPortada.set(false);
          this.toast.error(err.error?.detail || 'Error al subir la imagen de portada');
        }
      });
    } else {
      const url = this.inputUrlPortada().trim();
      if (!url) {
        this.toast.error('Por favor introduce una URL válida.');
        return;
      }
      this.guardandoPortada.set(true);
      this.aplicarPortadaEnBackend(url);
    }
  }

  private aplicarPortadaEnBackend(url: string): void {
    this.seguidorService.actualizarPortada(url, this.form.get('tituloProfesional')?.value).subscribe({
      next: () => {
        this.guardandoPortada.set(false);
        this.editandoPortada.set(false);
        const p = this.perfil();
        if (p) {
          this.perfil.set({ ...p, urlPortada: url || null });
        }
        this.toast.success('Portada actualizada correctamente');
      },
      error: (err) => {
        this.guardandoPortada.set(false);
        this.toast.error(err.error?.detail || err.error?.message || 'No se pudo actualizar la portada');
      }
    });
  }

  eliminarPortada(): void {
    this.guardandoPortada.set(true);
    this.seguidorService.actualizarPortada('', this.form.get('tituloProfesional')?.value).subscribe({
      next: () => {
        this.guardandoPortada.set(false);
        this.editandoPortada.set(false);
        const p = this.perfil();
        if (p) {
          this.perfil.set({ ...p, urlPortada: null });
        }
        this.toast.success('Portada eliminada');
      },
      error: (err) => {
        this.guardandoPortada.set(false);
        this.toast.error(err.error?.detail || 'No se pudo eliminar la portada');
      }
    });
  }

  editarDatosPersonales(): void {
    this.editandoDatosPersonales.set(true);
  }

  cancelarEdicionDatosPersonales(): void {
    const u = this.userProfile();
    if (u) {
      this.formPersonales.patchValue({
        nombres: u.nombres || '',
        apellidos: u.apellidos || '',
        fechaNacimiento: u.fechaNacimiento || '',
        idPais: u.idPais || null
      });
    }
    this.editandoDatosPersonales.set(false);
  }

  guardarDatosPersonales(): void {
    if (this.formPersonales.invalid) {
      this.formPersonales.markAllAsTouched();
      return;
    }

    const val = this.formPersonales.getRawValue();
    this.guardandoDatosPersonales.set(true);
    this.userService.updateCurrentUser({
      nombres: val.nombres || undefined,
      apellidos: val.apellidos || undefined,
      fechaNacimiento: val.fechaNacimiento || undefined,
      idPais: val.idPais ? Number(val.idPais) : undefined
    }).subscribe({
      next: (u) => {
        this.userProfile.set(u);
        this.guardandoDatosPersonales.set(false);
        this.editandoDatosPersonales.set(false);
        const p = this.perfil();
        if (p) {
          this.perfil.set({ ...p, nombresUsuario: u.nombres, apellidosUsuario: u.apellidos });
        }
        this.toast.success('Datos personales actualizados correctamente');
      },
      error: (err) => {
        this.guardandoDatosPersonales.set(false);
        this.toast.error(err.error?.detail || err.error?.message || 'No se pudieron guardar los cambios');
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
