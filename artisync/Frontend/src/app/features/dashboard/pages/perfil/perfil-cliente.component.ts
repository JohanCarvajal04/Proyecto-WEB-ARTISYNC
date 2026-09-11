import { Component, inject, signal, computed, OnInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AuthService } from '../../../seguridad/services/auth.service';
import { UserService } from '../../../perfil/services/user.service';
import { PaisService } from '../../../../shared/services/pais.service';
import { PedidoService } from '../../../pedido/services/pedido.service';
import { ServicioService } from '../../../creador/services/servicio.service';
import { ToastService } from '../../../../core/services/toast.service';
import { UserResponse, PaisResponse } from '../../../../shared/models/user.model';
import { RespuestaPedidoResumido } from '../../../pedido/models/pedido.model';
import { nombreUsuario } from '../../../../shared/utils/nombre-usuario';

@Component({
  selector: 'app-perfil-cliente',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule],
  templateUrl: './perfil-cliente.component.html'
})
export class PerfilClienteComponent implements OnInit {
  authService = inject(AuthService);
  private userService = inject(UserService);
  private paisService = inject(PaisService);
  private pedidoService = inject(PedidoService);
  private servicioService = inject(ServicioService);
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);

  @ViewChild('fileInputCliente') fileInputCliente?: ElementRef<HTMLInputElement>;
  @ViewChild('fileInputPortadaCliente') fileInputPortadaCliente?: ElementRef<HTMLInputElement>;

  readonly usuario = signal<UserResponse | null>(null);
  readonly paises = signal<PaisResponse[]>([]);
  readonly pedidos = signal<RespuestaPedidoResumido[]>([]);
  readonly isLoading = signal<boolean>(true);

  // Estados de edición de datos personales
  readonly editandoDatos = signal<boolean>(false);
  readonly guardandoDatos = signal<boolean>(false);
  readonly subiendoFoto = signal<boolean>(false);

  // Portada personalizable (archivo o URL)
  readonly urlPortadaCliente = signal<string>('');
  readonly editandoPortada = signal<boolean>(false);
  readonly guardandoPortada = signal<boolean>(false);
  readonly modoSubidaPortada = signal<'archivo' | 'url'>('archivo');
  readonly archivoPortadaSeleccionado = signal<File | null>(null);
  readonly previewPortadaUrl = signal<string>('');
  readonly inputUrlPortada = signal<string>('');

  form: FormGroup = this.fb.group({
    nombres: ['', [Validators.required, Validators.maxLength(100)]],
    apellidos: ['', [Validators.required, Validators.maxLength(100)]],
    fechaNacimiento: [''],
    idPais: [null]
  });

  userEmail = computed(() =>
    this.usuario()?.correo || this.authService.currentUser()?.email || this.authService.currentUser()?.sub || '—'
  );

  userName = computed(() => nombreUsuario(this.usuario(), this.userEmail()));

  iniciales = computed(() => {
    const n = this.userName();
    if (!n) return 'CL';
    const partes = n.trim().split(/\s+/);
    if (partes.length === 1) return partes[0].substring(0, 2).toUpperCase();
    return (partes[0][0] + partes[1][0]).toUpperCase();
  });

  totalPedidos = computed(() => this.pedidos().length);
  pedidosEnCurso = computed(() =>
    this.pedidos().filter(p => !p.etapaActual.toLowerCase().includes('complet') && !p.etapaActual.toLowerCase().includes('cancel')).length
  );
  pedidosCompletados = computed(() =>
    this.pedidos().filter(p => p.etapaActual.toLowerCase().includes('complet')).length
  );

  edad = computed(() => {
    const f = this.usuario()?.fechaNacimiento;
    if (!f) return null;
    const b = new Date(f);
    const now = new Date();
    let age = now.getFullYear() - b.getFullYear();
    const m = now.getMonth() - b.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age--;
    return age > 0 ? age : null;
  });

  ngOnInit(): void {
    const savedPortada = localStorage.getItem('artisync_portada_cliente');
    if (savedPortada) {
      this.urlPortadaCliente.set(savedPortada);
    }

    this.paisService.getPaisesActivos().subscribe({
      next: (data) => this.paises.set(data)
    });

    this.pedidoService.listarMisPedidos().subscribe({
      next: (list) => this.pedidos.set(list),
      error: () => this.pedidos.set([])
    });

    this.userService.getCurrentUser().subscribe({
      next: (u) => {
        this.usuario.set(u);
        this.patchForm(u);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  private patchForm(u: UserResponse): void {
    this.form.patchValue({
      nombres: u.nombres || '',
      apellidos: u.apellidos || '',
      fechaNacimiento: u.fechaNacimiento || '',
      idPais: u.idPais || null
    });
  }

  abrirSelectorFoto(): void {
    this.fileInputCliente?.nativeElement.click();
  }

  onFotoSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const tiposValidos = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];
      if (!tiposValidos.includes(file.type)) {
        this.toastService.error('Formato no soportado. Se acepta JPG, PNG, WEBP o GIF.');
        input.value = '';
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        this.toastService.error('La foto supera el máximo permitido de 5 MB.');
        input.value = '';
        return;
      }

      this.subiendoFoto.set(true);
      this.userService.uploadProfilePicture(file).subscribe({
        next: (u) => {
          this.subiendoFoto.set(false);
          this.usuario.set(u);
          this.toastService.success('Foto de perfil actualizada correctamente');
        },
        error: (err) => {
          this.subiendoFoto.set(false);
          this.toastService.error(err.error?.detail || err.error?.message || 'Error al subir la foto de perfil');
        }
      });
      input.value = '';
    }
  }

  toggleEditarPortada(): void {
    const actual = this.urlPortadaCliente() || '';
    this.inputUrlPortada.set(actual);
    this.previewPortadaUrl.set(actual);
    this.archivoPortadaSeleccionado.set(null);
    this.modoSubidaPortada.set('archivo');
    this.editandoPortada.update(v => !v);
  }

  abrirSelectorArchivoPortada(): void {
    this.fileInputPortadaCliente?.nativeElement.click();
  }

  onArchivoPortadaSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const tiposValidos = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];
      if (!tiposValidos.includes(file.type)) {
        this.toastService.error('Formato no soportado. Se acepta JPG, PNG, WEBP o GIF.');
        input.value = '';
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        this.toastService.error('La imagen supera el límite de 5 MB.');
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
        this.toastService.error('Por favor selecciona un archivo de imagen primero.');
        return;
      }
      this.guardandoPortada.set(true);
      this.servicioService.subirMiniatura(file).subscribe({
        next: (res) => {
          this.guardandoPortada.set(false);
          this.aplicarPortadaEnFrontend(res.url);
        },
        error: (err) => {
          this.guardandoPortada.set(false);
          this.toastService.error(err.error?.detail || 'Error al subir la imagen de portada');
        }
      });
    } else {
      const url = this.inputUrlPortada().trim();
      if (!url) {
        this.toastService.error('Por favor selecciona una imagen o ingresa una URL.');
        return;
      }
      this.aplicarPortadaEnFrontend(url);
    }
  }

  private aplicarPortadaEnFrontend(url: string): void {
    this.urlPortadaCliente.set(url);
    localStorage.setItem('artisync_portada_cliente', url);
    this.editandoPortada.set(false);
    this.toastService.success('Portada actualizada correctamente');
  }

  eliminarPortada(): void {
    this.urlPortadaCliente.set('');
    localStorage.removeItem('artisync_portada_cliente');
    this.editandoPortada.set(false);
    this.toastService.success('Portada eliminada');
  }

  editarDatos(): void {
    this.editandoDatos.set(true);
  }

  cancelarEdicionDatos(): void {
    const u = this.usuario();
    if (u) this.patchForm(u);
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
        this.usuario.set(u);
        this.guardandoDatos.set(false);
        this.editandoDatos.set(false);
        this.toastService.success('Datos personales actualizados correctamente');
      },
      error: (err) => {
        this.guardandoDatos.set(false);
        this.toastService.error(err.error?.detail || err.error?.message || 'No se pudieron guardar los cambios');
      }
    });
  }
}
