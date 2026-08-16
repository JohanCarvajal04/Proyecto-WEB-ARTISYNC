import { Component, inject, signal, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ToastService } from '../../../../core/services/toast.service';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import { BriefingService } from '../../../comunicacion/services/briefing.service';
import {
  MAX_NOMBRE_PLANTILLA,
  MAX_PREGUNTAS_PLANTILLA,
  RespuestaBriefing
} from '../../../comunicacion/models/comunicacion.model';
import { formatDateTime, mensajeError } from '../../utils/formato';

@Component({
  selector: 'app-briefings',
  standalone: true,
  imports: [ReactiveFormsModule, PerfilRequeridoComponent],
  templateUrl: './briefings.component.html'
})
export class BriefingsComponent implements OnInit {

  private fb = inject(FormBuilder);
  private briefingService = inject(BriefingService);
  private contexto = inject(CreadorContextoService);
  private toast = inject(ToastService);

  readonly plantillas = signal<RespuestaBriefing[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  readonly modalAbierto = signal<boolean>(false);
  readonly enEdicion = signal<RespuestaBriefing | null>(null);
  readonly guardando = signal<boolean>(false);
  readonly aEliminar = signal<RespuestaBriefing | null>(null);
  readonly eliminando = signal<boolean>(false);

  readonly perfilFaltante = this.contexto.perfilFaltante;

  readonly maxPreguntas = MAX_PREGUNTAS_PLANTILLA;

  form: FormGroup = this.fb.group({
    nombrePlantilla: ['', [Validators.required, Validators.maxLength(MAX_NOMBRE_PLANTILLA)]],
    preguntas: this.fb.array([this.nuevaPregunta()])
  });

  get preguntas(): FormArray {
    return this.form.get('preguntas') as FormArray;
  }

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
    this.isLoading.set(true);
    this.error.set('');

    this.briefingService.listarMisPlantillas().subscribe({
      next: (data) => {
        this.plantillas.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'Error al cargar tus plantillas de briefing'));
        this.isLoading.set(false);
      }
    });
  }

  private nuevaPregunta(texto = '', orden = 1): FormGroup {
    return this.fb.group({
      textoPregunta: [texto, [Validators.required]],
      numeroOrden: [orden]
    });
  }

  agregarPregunta(): void {
    if (this.preguntas.length >= MAX_PREGUNTAS_PLANTILLA) {
      this.toast.warning(`Una plantilla admite como máximo ${MAX_PREGUNTAS_PLANTILLA} preguntas.`);
      return;
    }
    this.preguntas.push(this.nuevaPregunta('', this.preguntas.length + 1));
  }

  quitarPregunta(indice: number): void {
    if (this.preguntas.length === 1) {
      this.toast.warning('La plantilla necesita al menos una pregunta.');
      return;
    }
    this.preguntas.removeAt(indice);
    this.renumerar();
  }

  /** `numeroOrden` debe quedar consecutivo desde 1: el backend valida 1..10. */
  private renumerar(): void {
    this.preguntas.controls.forEach((control, i) => {
      control.get('numeroOrden')?.setValue(i + 1);
    });
  }

  abrirModal(plantilla?: RespuestaBriefing): void {
    this.enEdicion.set(plantilla ?? null);

    const preguntas = [...(plantilla?.preguntas ?? [])].sort((a, b) => a.numeroOrden - b.numeroOrden);
    this.form.setControl('preguntas', this.fb.array(
      preguntas.length > 0
        ? preguntas.map((p, i) => this.nuevaPregunta(p.textoPregunta, i + 1))
        : [this.nuevaPregunta()]
    ));
    this.form.patchValue({ nombrePlantilla: plantilla?.nombrePlantilla ?? '' });

    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.enEdicion.set(null);
  }

  invalido(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  preguntaInvalida(indice: number): boolean {
    const control = this.preguntas.at(indice).get('textoPregunta');
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.renumerar();
    const val = this.form.getRawValue() as {
      nombrePlantilla: string;
      preguntas: { textoPregunta: string; numeroOrden: number }[];
    };

    const peticion = {
      nombrePlantilla: val.nombrePlantilla.trim(),
      preguntas: val.preguntas.map(p => ({
        textoPregunta: p.textoPregunta.trim(),
        numeroOrden: p.numeroOrden
      }))
    };

    const editando = this.enEdicion();
    this.guardando.set(true);

    const accion$ = editando
      ? this.briefingService.editarPlantilla(editando.idPlantilla, peticion)
      : this.briefingService.crearPlantilla(peticion);

    accion$.subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrarModal();
        this.toast.success(editando ? 'Plantilla actualizada' : 'Plantilla creada');
        this.cargar();
      },
      error: (err) => {
        this.guardando.set(false);
        this.toast.error(mensajeError(err, 'No se pudo guardar la plantilla'));
      }
    });
  }

  confirmarEliminar(plantilla: RespuestaBriefing): void {
    this.aEliminar.set(plantilla);
  }

  cancelarEliminar(): void {
    this.aEliminar.set(null);
  }

  eliminar(): void {
    const plantilla = this.aEliminar();
    if (!plantilla) return;

    this.eliminando.set(true);
    this.briefingService.eliminarPlantilla(plantilla.idPlantilla).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.aEliminar.set(null);
        this.toast.success('Plantilla eliminada');
        this.cargar();
      },
      error: (err) => {
        this.eliminando.set(false);
        this.toast.error(mensajeError(err, 'No se pudo eliminar la plantilla'));
      }
    });
  }

  preguntasOrdenadas(plantilla: RespuestaBriefing) {
    return [...(plantilla.preguntas ?? [])].sort((a, b) => a.numeroOrden - b.numeroOrden);
  }

  formatDateTime = formatDateTime;
}
