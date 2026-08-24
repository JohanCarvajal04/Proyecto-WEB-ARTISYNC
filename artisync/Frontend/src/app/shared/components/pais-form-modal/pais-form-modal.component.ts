import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaisResponse } from '../../models/user.model';
import { PaisRequest } from '../../../features/administracion/models/admin.model';

@Component({
  selector: 'app-pais-form-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './pais-form-modal.component.html'
})
export class PaisFormModalComponent implements OnInit {
  @Input() mode: 'create' | 'edit' = 'create';
  @Input() pais: PaisResponse | null = null;
  @Input() isLoading = false;
  
  @Output() closeModal = new EventEmitter<void>();
  @Output() save = new EventEmitter<PaisRequest>();

  private fb = inject(FormBuilder);
  
  paisForm!: FormGroup;

  ngOnInit(): void {
    this.initForm();
    if (this.mode === 'edit' && this.pais) {
      this.paisForm.patchValue({
        nombrePais: this.pais.nombrePais
      });
    }
  }

  private initForm(): void {
    this.paisForm = this.fb.group({
      nombrePais: ['', [Validators.required, Validators.maxLength(100)]]
    });
  }

  onSubmit(): void {
    if (this.paisForm.valid) {
      this.save.emit(this.paisForm.value as PaisRequest);
    } else {
      Object.keys(this.paisForm.controls).forEach(key => {
        const control = this.paisForm.get(key);
        if (control?.invalid) {
          control.markAsTouched();
        }
      });
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.paisForm.get(fieldName);
    return field ? (field.invalid && (field.dirty || field.touched)) : false;
  }
}
