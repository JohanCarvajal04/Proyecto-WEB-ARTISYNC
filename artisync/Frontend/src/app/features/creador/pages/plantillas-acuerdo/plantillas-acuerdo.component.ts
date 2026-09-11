import { Component, ElementRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PlantillaContratoService } from '../../../legal/services/plantilla-contrato.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  PeticionActualizarPlantillaAcuerdoPropia,
  PeticionCrearPlantillaAcuerdoPropia,
  RespuestaPlantillaContrato
} from '../../../legal/models/legal.model';
import {
  htmlPlantillaAMarkdown,
  markdownAHtmlFragmento,
  markdownAHtmlPlantilla
} from '../../../legal/utils/plantilla-contrato-markdown';

/**
 * Autoservicio de plantillas de acuerdo propias del creador (V45). Aparte
 * del catálogo general que cura ADMIN (ver plantillas-contrato.component en
 * administración): aquí el creador redacta sus propias plantillas, visibles
 * solo para él al elegir "Plantilla de acuerdo" en su servicio-form.
 *
 * "Acuerdo" y no "Contrato": es el mismo documento bilateral que firman
 * Creador y Cliente (ver ContratoVistaComponent), solo que el rótulo que ve
 * el creador aquí usa el término con el que se presenta esa relación; las
 * entidades y endpoints internos siguen llamándose "Contrato" (REQ-F-017).
 *
 * El editor reutiliza el mismo patrón Markdown → HTML que la pantalla de
 * ADMIN (plantilla-contrato-markdown.ts), pero agrega variables agrupadas
 * por categoría, clicables, que se insertan en la posición del cursor.
 */
@Component({
  selector: 'app-plantillas-acuerdo',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './plantillas-acuerdo.component.html'
})
export class PlantillasAcuerdoComponent implements OnInit {

  private plantillaContratoService = inject(PlantillaContratoService);
  private toast = inject(ToastService);

  @ViewChild('cuerpoTextarea') private cuerpoTextareaRef?: ElementRef<HTMLTextAreaElement>;

  readonly plantillas = signal<RespuestaPlantillaContrato[]>([]);
  readonly isLoading = signal<boolean>(true);

  readonly isFormOpen = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly editingId = signal<number | null>(null);

  /** Mismo set de placeholders que sustituye ContratoServicioImpl, agrupado para el picker (patrón del catálogo curado por ADMIN). */
  readonly categoriasVariables: { titulo: string; variables: string[] }[] = [
    { titulo: 'Partes', variables: ['{{nombre_creador}}', '{{nombre_cliente}}'] },
    { titulo: 'Servicio y precio', variables: ['{{descripcion_servicio}}', '{{precio_pactado}}', '{{limite_revisiones}}'] },
    { titulo: 'Fechas', variables: ['{{fecha_entrega}}', '{{fecha_actual}}'] }
  ];

  readonly placeholderEjemplo =
    '# Acuerdo de Prestación de Servicios\n\nEntre **{{nombre_creador}}** y **{{nombre_cliente}}**...';

  /** Lo que escribe el creador. `formData.cuerpoHtmlPlantilla` se calcula recién al guardar. */
  readonly cuerpoMarkdown = signal<string>('');
  readonly previewHtml = computed(() => markdownAHtmlFragmento(this.cuerpoMarkdown()));

  formData = {
    nombrePlantilla: '',
    cuerpoHtmlPlantilla: '',
    activa: true
  };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.isLoading.set(true);
    this.plantillaContratoService.listarPropias().subscribe({
      next: (data) => {
        this.plantillas.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.toast.error('No se pudieron cargar tus plantillas de acuerdo');
        this.isLoading.set(false);
      }
    });
  }

  openNewForm(): void {
    this.editingId.set(null);
    this.formData = { nombrePlantilla: '', cuerpoHtmlPlantilla: '', activa: true };
    this.cuerpoMarkdown.set('');
    this.isFormOpen.set(true);
  }

  openEditForm(plantilla: RespuestaPlantillaContrato): void {
    this.editingId.set(plantilla.idPlantilla);
    this.formData = {
      nombrePlantilla: plantilla.nombrePlantilla,
      cuerpoHtmlPlantilla: plantilla.cuerpoHtmlPlantilla,
      activa: plantilla.activa
    };
    // Mejor esfuerzo, mismo criterio que el editor de ADMIN: reconstruye
    // Markdown a partir del HTML guardado.
    this.cuerpoMarkdown.set(htmlPlantillaAMarkdown(plantilla.cuerpoHtmlPlantilla));
    this.isFormOpen.set(true);
  }

  closeForm(): void {
    this.isFormOpen.set(false);
  }

  onCuerpoMarkdown(evento: Event): void {
    this.cuerpoMarkdown.set((evento.target as HTMLTextAreaElement).value);
  }

  /**
   * Inserta la variable en la posición actual del cursor del editor (patrón
   * del picker de variables), no siempre al final del texto — así el
   * creador puede colocarla en medio de una frase ya escrita.
   */
  insertarVariable(variable: string): void {
    const textarea = this.cuerpoTextareaRef?.nativeElement;
    const actual = this.cuerpoMarkdown();

    if (!textarea) {
      this.cuerpoMarkdown.set(actual + variable);
      return;
    }

    const inicio = textarea.selectionStart ?? actual.length;
    const fin = textarea.selectionEnd ?? actual.length;
    this.cuerpoMarkdown.set(actual.slice(0, inicio) + variable + actual.slice(fin));

    const posicionCursor = inicio + variable.length;
    // Se pospone al siguiente ciclo: el textarea es [value] enlazado al
    // signal, así que su value en el DOM todavía no cambió en este mismo tick.
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(posicionCursor, posicionCursor);
    });
  }

  formularioValido(): boolean {
    return !!this.formData.nombrePlantilla.trim() && !!this.cuerpoMarkdown().trim();
  }

  submitForm(): void {
    if (!this.formularioValido()) {
      this.toast.error('El nombre y el contenido del acuerdo son obligatorios');
      return;
    }

    this.formData.cuerpoHtmlPlantilla = markdownAHtmlPlantilla(this.cuerpoMarkdown());

    this.isSubmitting.set(true);
    const id = this.editingId();

    if (id) {
      const payload: PeticionActualizarPlantillaAcuerdoPropia = { ...this.formData };
      this.plantillaContratoService.editarPropia(id, payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeForm();
          this.toast.success('Plantilla de acuerdo actualizada');
          this.cargar();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.toast.error(err.error?.detail || err.error?.message || 'No se pudo actualizar la plantilla');
        }
      });
    } else {
      const payload: PeticionCrearPlantillaAcuerdoPropia = {
        nombrePlantilla: this.formData.nombrePlantilla.trim(),
        cuerpoHtmlPlantilla: this.formData.cuerpoHtmlPlantilla
      };
      this.plantillaContratoService.crearPropia(payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeForm();
          this.toast.success('Plantilla de acuerdo creada');
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
    if (!confirm(`¿Desactivar la plantilla «${plantilla.nombrePlantilla}»? Dejará de estar disponible para tus servicios.`)) return;

    this.plantillaContratoService.desactivarPropia(plantilla.idPlantilla).subscribe({
      next: () => {
        this.toast.success('Plantilla desactivada');
        this.cargar();
      },
      error: (err) => this.toast.error(err.error?.detail || err.error?.message || 'No se pudo desactivar la plantilla')
    });
  }
}
