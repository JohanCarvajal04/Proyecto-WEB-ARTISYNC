import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ToastService } from '../../../../core/services/toast.service';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { SorteoService } from '../../services/sorteo.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import {
  RespuestaSorteo,
  RespuestaParticipante,
  RespuestaGanador,
  PeticionCrearSorteo,
  PeticionActualizarSorteo
} from '../../models/creador.model';
import { formatDateTime, mensajeError } from '../../utils/formato';

@Component({
  selector: 'app-sorteos',
  standalone: true,
  imports: [ReactiveFormsModule, PerfilRequeridoComponent],
  templateUrl: './sorteos.component.html',
  styleUrl: './sorteos.component.css'
})
export class SorteosComponent implements OnInit {

  private fb = inject(FormBuilder);
  private sorteoService = inject(SorteoService);
  private contexto = inject(CreadorContextoService);
  private toast = inject(ToastService);

  readonly sorteos = signal<RespuestaSorteo[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  readonly modalAbierto = signal<boolean>(false);
  readonly sorteoEnEdicion = signal<RespuestaSorteo | null>(null);
  readonly guardando = signal<boolean>(false);
  readonly aEliminar = signal<RespuestaSorteo | null>(null);

  /** Sorteo cuyo detalle de participantes está desplegado. */
  readonly detalleAbierto = signal<number | null>(null);
  readonly participantes = signal<RespuestaParticipante[]>([]);
  readonly ganadores = signal<RespuestaGanador[]>([]);
  readonly cargandoParticipantes = signal<boolean>(false);

  readonly perfilFaltante = this.contexto.perfilFaltante;

  form: FormGroup = this.fb.group({
    tituloSorteo: ['', [Validators.required, Validators.maxLength(150)]],
    descripcionPremios: ['', [Validators.required]],
    cantidadGanadores: [1, [Validators.required, Validators.min(1)]],
    fechaInicio: ['', [Validators.required]],
    fechaCierre: ['', [Validators.required]],
    requiereSeguidor: [false]
  });

  activos = computed(() => this.sorteos().filter(s => (s.estadoSorteo || '').toLowerCase().includes('activ')).length);

  totalParticipantes = computed(() => this.sorteos().reduce((suma, s) => suma + Number(s.totalParticipantes || 0), 0));

  finalizados = computed(() => this.sorteos().filter(s => (s.estadoSorteo || '').toLowerCase().includes('finaliz')).length);

  ngOnInit(): void {
    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        if (!perfil) {
          this.isLoading.set(false);
          return;
        }
        this.cargar();
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar tu perfil de creador'));
        this.isLoading.set(false);
      }
    });
  }

  cargar(): void {
    const perfil = this.contexto.perfil();
    if (!perfil) return;
    this.isLoading.set(true);
    this.error.set('');
    this.sorteoService.listarPorCreador(perfil.idPerfil).subscribe({
      next: (data) => {
        this.sorteos.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'Error al cargar tus sorteos'));
        this.isLoading.set(false);
      }
    });
  }

  // ── Alta / edición ──

  abrirNuevo(): void {
    this.sorteoEnEdicion.set(null);
    this.form.reset({
      tituloSorteo: '',
      descripcionPremios: '',
      cantidadGanadores: 1,
      fechaInicio: '',
      fechaCierre: '',
      requiereSeguidor: false
    });
    this.form.get('fechaInicio')?.enable();
    this.modalAbierto.set(true);
  }

  abrirEdicion(sorteo: RespuestaSorteo): void {
    this.sorteoEnEdicion.set(sorteo);
    this.form.patchValue({
      tituloSorteo: sorteo.tituloSorteo,
      descripcionPremios: sorteo.descripcionPremios,
      cantidadGanadores: sorteo.cantidadGanadores,
      fechaInicio: this.aValorInput(sorteo.fechaInicio),
      fechaCierre: this.aValorInput(sorteo.fechaCierre),
      requiereSeguidor: sorteo.requiereSeguidor
    });
    // La fecha de inicio no se puede mover una vez creado el sorteo.
    this.form.get('fechaInicio')?.disable();
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.sorteoEnEdicion.set(null);
  }

  invalido(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.getRawValue();
    this.guardando.set(true);

    const enEdicion = this.sorteoEnEdicion();
    if (enEdicion) {
      const peticion: PeticionActualizarSorteo = {
        tituloSorteo: val.tituloSorteo!,
        descripcionPremios: val.descripcionPremios!,
        cantidadGanadores: Number(val.cantidadGanadores),
        fechaCierre: this.aIsoLocal(val.fechaCierre!)
      };
      this.sorteoService.actualizar(enEdicion.idSorteo, peticion).subscribe({
        next: (actualizado) => {
          this.sorteos.update(lista => lista.map(s => s.idSorteo === actualizado.idSorteo ? actualizado : s));
          this.guardando.set(false);
          this.cerrarModal();
          this.toast.success('Sorteo actualizado');
        },
        error: (err) => {
          this.guardando.set(false);
          this.toast.error(mensajeError(err, 'No se pudo actualizar el sorteo'));
        }
      });
    } else {
      const peticion: PeticionCrearSorteo = {
        tituloSorteo: val.tituloSorteo!,
        descripcionPremios: val.descripcionPremios!,
        cantidadGanadores: Number(val.cantidadGanadores),
        fechaInicio: this.aIsoLocal(val.fechaInicio!),
        fechaCierre: this.aIsoLocal(val.fechaCierre!),
        requiereSeguidor: !!val.requiereSeguidor
      };
      this.sorteoService.crear(peticion).subscribe({
        next: (creado) => {
          this.sorteos.update(lista => [creado, ...lista]);
          this.guardando.set(false);
          this.cerrarModal();
          this.toast.success('Sorteo creado');
        },
        error: (err) => {
          this.guardando.set(false);
          this.toast.error(mensajeError(err, 'No se pudo crear el sorteo'));
        }
      });
    }
  }

  // ── Baja ──

  pedirConfirmacion(sorteo: RespuestaSorteo): void {
    this.aEliminar.set(sorteo);
  }

  cancelarEliminacion(): void {
    this.aEliminar.set(null);
  }

  confirmarEliminacion(): void {
    const sorteo = this.aEliminar();
    if (!sorteo) return;
    this.guardando.set(true);
    this.sorteoService.eliminar(sorteo.idSorteo).subscribe({
      next: () => {
        this.sorteos.update(lista => lista.filter(s => s.idSorteo !== sorteo.idSorteo));
        this.guardando.set(false);
        this.aEliminar.set(null);
        this.toast.success('Sorteo eliminado');
      },
      error: (err) => {
        this.guardando.set(false);
        this.aEliminar.set(null);
        this.toast.error(mensajeError(err, 'No se pudo eliminar el sorteo'));
      }
    });
  }

  // ── Participantes ──

  alternarDetalle(sorteo: RespuestaSorteo): void {
    if (this.detalleAbierto() === sorteo.idSorteo) {
      this.detalleAbierto.set(null);
      this.participantes.set([]);
      return;
    }

    this.detalleAbierto.set(sorteo.idSorteo);
    this.participantes.set([]);
    this.ganadores.set([]);
    this.cargandoParticipantes.set(true);
    this.sorteoService.listarParticipantes(sorteo.idSorteo).subscribe({
      next: (data) => {
        this.participantes.set(data);
        this.cargandoParticipantes.set(false);
      },
      error: (err) => {
        this.cargandoParticipantes.set(false);
        this.toast.error(mensajeError(err, 'No se pudieron cargar los participantes'));
      }
    });

    // Los ganadores solo existen tras el cierre; el backend responde error si
    // se piden antes, así que ni se intenta mientras el sorteo siga abierto.
    if ((sorteo.estadoSorteo || '').toLowerCase().includes('finaliz')) {
      this.sorteoService.listarGanadores(sorteo.idSorteo).subscribe({
        next: (data) => this.ganadores.set(data),
        error: () => this.ganadores.set([])
      });
    }
  }

  badgeSorteo(estado: string): string {
    const lower = (estado || '').toLowerCase();
    if (lower.includes('activ')) return 'cr-badge cr-badge--ok';
    if (lower.includes('finaliz')) return 'cr-badge cr-badge--info';
    if (lower.includes('cancel')) return 'cr-badge cr-badge--danger';
    return 'cr-badge cr-badge--warn';
  }

  /** `LocalDateTime` del backend → valor de un `<input type="datetime-local">`. */
  private aValorInput(fecha: string): string {
    if (!fecha) return '';
    return fecha.length >= 16 ? fecha.slice(0, 16) : fecha;
  }

  /** Valor del input → `LocalDateTime` sin zona, que es lo que espera el backend. */
  private aIsoLocal(valor: string): string {
    return valor.length === 16 ? `${valor}:00` : valor;
  }

  formatDateTime = formatDateTime;
}
