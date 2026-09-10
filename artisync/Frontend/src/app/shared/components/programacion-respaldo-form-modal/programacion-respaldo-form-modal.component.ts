import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CrearProgramacionRequest, PRESETS_CRON, ProgramacionRespaldoResponse } from '../../../features/administracion/models/respaldo.model';

@Component({
  selector: 'app-programacion-respaldo-form-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './programacion-respaldo-form-modal.component.html'
})
export class ProgramacionRespaldoFormModalComponent implements OnInit {
  @Input() mode: 'create' | 'edit' = 'create';
  @Input() programacion: ProgramacionRespaldoResponse | null = null;
  @Input() isLoading = false;

  @Output() closeModal = new EventEmitter<void>();
  @Output() save = new EventEmitter<CrearProgramacionRequest>();

  private fb = inject(FormBuilder);

  readonly presetsCron = PRESETS_CRON;

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      tipoRespaldo: ['FULL', [Validators.required]],
      expresionCron: ['0 0 3 * * *', [Validators.required]],
      retencionDias: [30, [Validators.required, Validators.min(1), Validators.max(3650)]]
    });

    if (this.mode === 'edit' && this.programacion) {
      this.form.patchValue({
        nombre: this.programacion.nombre,
        tipoRespaldo: this.programacion.tipoRespaldo,
        expresionCron: this.programacion.expresionCron,
        retencionDias: this.programacion.retencionDias
      });
    }
  }

  aplicarPreset(valor: string): void {
    this.form.get('expresionCron')?.setValue(valor);
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.save.emit(this.form.value as CrearProgramacionRequest);
    } else {
      Object.keys(this.form.controls).forEach(key => this.form.get(key)?.markAsTouched());
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return field ? (field.invalid && (field.dirty || field.touched)) : false;
  }
}
