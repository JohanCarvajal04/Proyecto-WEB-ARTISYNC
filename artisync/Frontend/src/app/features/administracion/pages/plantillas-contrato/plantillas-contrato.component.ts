import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PlantillaContratoService } from '../../../legal/services/plantilla-contrato.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  PeticionActualizarPlantillaContrato,
  PeticionCrearPlantillaContrato,
  RespuestaPlantillaContrato
} from '../../../legal/models/legal.model';
import {
  PLACEHOLDERS_PLANTILLA_CONTRATO,
  htmlPlantillaAMarkdown,
  markdownAHtmlFragmento,
  markdownAHtmlPlantilla
} from '../../../legal/utils/plantilla-contrato-markdown';

/**
 * Catálogo de plantillas de contrato (REQ-F-017 ampliado). Curado por ADMIN:
 * el creador solo elige entre estas al crear/editar su servicio (ver
 * servicio-form.component), nunca escribe el texto legal.
 *
 * El admin escribe el texto en Markdown (normal, sin etiquetas) y este
 * componente lo convierte a HTML antes de guardarlo — el backend sigue
 * recibiendo `cuerpoHtmlPlantilla` exactamente como antes, sin cambios.
 */
@Component({
  selector: 'app-plantillas-contrato',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './plantillas-contrato.component.html'
})
export class PlantillasContratoComponent implements OnInit {

  private plantillaContratoService = inject(PlantillaContratoService);
  private toast = inject(ToastService);

  readonly plantillas = signal<RespuestaPlantillaContrato[]>([]);
  readonly isLoading = signal<boolean>(true);

  readonly isFormOpen = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly editingId = signal<number | null>(null);

  readonly placeholders = PLACEHOLDERS_PLANTILLA_CONTRATO;
  readonly placeholderEjemplo =
    '# Contrato de Prestación de Servicios\n\nEntre **{{nombre_creador}}** y **{{nombre_cliente}}**...';

  /** Lo que escribe el admin. `formData.cuerpoHtmlPlantilla` se calcula recién al guardar. */
  readonly cuerpoMarkdown = signal<string>('');
  readonly previewHtml = computed(() => markdownAHtmlFragmento(this.cuerpoMarkdown()));

  formData = {
    nombrePlantilla: '',
    versionLegal: '',
    cuerpoHtmlPlantilla: '',
    esPredeterminada: false,
    activa: true
  };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.isLoading.set(true);
    this.plantillaContratoService.listarTodas().subscribe({
      next: (data) => {
        this.plantillas.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.toast.error('No se pudieron cargar las plantillas de contrato');
        this.isLoading.set(false);
      }
    });
  }

  openNewForm(): void {
    this.editingId.set(null);
    this.formData = { nombrePlantilla: '', versionLegal: '', cuerpoHtmlPlantilla: '', esPredeterminada: false, activa: true };
    this.cuerpoMarkdown.set('');
    this.isFormOpen.set(true);
  }

  openEditForm(plantilla: RespuestaPlantillaContrato): void {
    this.editingId.set(plantilla.idPlantilla);
    this.formData = {
      nombrePlantilla: plantilla.nombrePlantilla,
      versionLegal: plantilla.versionLegal,
      cuerpoHtmlPlantilla: plantilla.cuerpoHtmlPlantilla,
      esPredeterminada: plantilla.esPredeterminada,
      activa: plantilla.activa
    };
    // Mejor esfuerzo: reconstruye Markdown a partir del HTML guardado. Si la
    // plantilla no se creó desde este editor, el resultado puede no calzar
    // exacto con el documento original, pero conserva títulos/párrafos/negritas.
    this.cuerpoMarkdown.set(htmlPlantillaAMarkdown(plantilla.cuerpoHtmlPlantilla));
    this.isFormOpen.set(true);
  }

  closeForm(): void {
    this.isFormOpen.set(false);
  }

  onCuerpoMarkdown(evento: Event): void {
    this.cuerpoMarkdown.set((evento.target as HTMLTextAreaElement).value);
  }

  formularioValido(): boolean {
    return !!this.formData.nombrePlantilla.trim()
      && !!this.formData.versionLegal.trim()
      && !!this.cuerpoMarkdown().trim();
  }

  submitForm(): void {
    if (!this.formularioValido()) {
      this.toast.error('Nombre, versión legal y contenido son obligatorios');
      return;
    }

    this.formData.cuerpoHtmlPlantilla = markdownAHtmlPlantilla(this.cuerpoMarkdown());

    this.isSubmitting.set(true);
    const id = this.editingId();

    if (id) {
      const payload: PeticionActualizarPlantillaContrato = { ...this.formData };
      this.plantillaContratoService.editar(id, payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeForm();
          this.toast.success('Plantilla actualizada');
          this.cargar();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.toast.error(err.error?.detail || err.error?.message || 'No se pudo actualizar la plantilla');
        }
      });
    } else {
      const payload: PeticionCrearPlantillaContrato = {
        nombrePlantilla: this.formData.nombrePlantilla.trim(),
        versionLegal: this.formData.versionLegal.trim(),
        cuerpoHtmlPlantilla: this.formData.cuerpoHtmlPlantilla,
        esPredeterminada: this.formData.esPredeterminada
      };
      this.plantillaContratoService.crear(payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeForm();
          this.toast.success('Plantilla creada');
          this.cargar();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.toast.error(err.error?.detail || err.error?.message || 'No se pudo crear la plantilla');
        }
      });
    }
  }

  desactivar(plantilla: RespuestaPlantillaContrato): void {
    if (plantilla.esPredeterminada) {
      this.toast.warning('No puedes desactivar la plantilla predeterminada; marca otra como predeterminada primero');
      return;
    }
    if (!confirm(`¿Desactivar la plantilla «${plantilla.nombrePlantilla}»? Dejará de estar disponible para nuevos servicios.`)) return;

    this.plantillaContratoService.desactivar(plantilla.idPlantilla).subscribe({
      next: () => {
        this.toast.success('Plantilla desactivada');
        this.cargar();
      },
      error: (err) => this.toast.error(err.error?.detail || err.error?.message || 'No se pudo desactivar la plantilla')
    });
  }
}
