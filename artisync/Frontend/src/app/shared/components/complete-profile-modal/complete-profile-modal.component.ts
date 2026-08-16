import { Component, EventEmitter, Input, Output, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaisResponse, UserResponse } from '../../models/user.model';
import { PaisService } from '../../services/pais.service';
import { UserService } from '../../../features/perfil/services/user.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-complete-profile-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './complete-profile-modal.component.html'
})
export class CompleteProfileModalComponent implements OnInit {
  @Input({ required: true }) user!: UserResponse;
  @Output() completed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private paisService = inject(PaisService);
  private userService = inject(UserService);
  private toastService = inject(ToastService);

  readonly paises = signal<PaisResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly maxDate = signal<string>('');

  profileForm!: FormGroup;

  ngOnInit(): void {
    // Set max date to today
    const today = new Date();
    this.maxDate.set(today.toISOString().split('T')[0]);

    this.profileForm = this.fb.group({
      idPais: [this.user.idPais || null, [Validators.required]],
      fechaNacimiento: [this.user.fechaNacimiento || '', [Validators.required]]
    });

    this.loadPaises();
  }

  private loadPaises(): void {
    this.paisService.getPaises().subscribe({
      next: (data) => this.paises.set(data),
      error: () => this.toastService.error('Error al cargar la lista de países')
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const val = this.profileForm.value;
    const selectedDate = new Date(val.fechaNacimiento);
    const today = new Date();
    
    if (selectedDate > today) {
      this.toastService.error('La fecha de nacimiento no puede ser futura');
      return;
    }

    this.isLoading.set(true);
    this.userService.updateCurrentUser({
      idPais: Number(val.idPais),
      fechaNacimiento: val.fechaNacimiento
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.toastService.success('Perfil completado exitosamente');
        this.completed.emit();
      },
      error: () => {
        this.isLoading.set(false);
        this.toastService.error('Ocurrió un error al guardar tu perfil');
      }
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.profileForm.get(fieldName);
    return field ? (field.invalid && (field.dirty || field.touched)) : false;
  }
}
