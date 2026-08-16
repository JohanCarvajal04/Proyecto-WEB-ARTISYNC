import { Component, EventEmitter, Input, Output, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaisResponse, UserResponse } from '../../models/user.model';
import { PaisService } from '../../services/pais.service';
import { UserService } from '../../../features/perfil/services/user.service';
import { ToastService } from '../../../core/services/toast.service';

export interface CalendarDay {
  date: Date;
  dateString: string;
  dayNumber: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  isDisabled: boolean;
}

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

  // Inline Datepicker State
  readonly monthsList = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
  ];
  readonly weekDays = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá', 'Do'];
  readonly yearsList = signal<number[]>([]);
  readonly calendarViewDate = signal<Date>(new Date(2000, 0, 1));
  readonly calendarViewMode = signal<'days' | 'months' | 'years'>('days');

  profileForm!: FormGroup;

  ngOnInit(): void {
    const today = new Date();
    this.maxDate.set(today.toISOString().split('T')[0]);

    this.profileForm = this.fb.group({
      idPais: [this.user.idPais || null, [Validators.required]],
      fechaNacimiento: [this.user.fechaNacimiento || '', [Validators.required]]
    });

    // Populate years list (from current year down to 1930)
    const currentYr = today.getFullYear();
    const yrs: number[] = [];
    for (let y = currentYr; y >= 1930; y--) {
      yrs.push(y);
    }
    this.yearsList.set(yrs);

    // Initial calendar view date
    if (this.user.fechaNacimiento) {
      const parts = this.user.fechaNacimiento.split('-');
      if (parts.length === 3) {
        this.calendarViewDate.set(new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])));
      }
    } else {
      // Default to year 2000 for birthdate selection convenience
      this.calendarViewDate.set(new Date(2000, 0, 1));
    }

    this.loadPaises();
  }

  get currentYear(): number {
    return this.calendarViewDate().getFullYear();
  }

  get currentMonthIndex(): number {
    return this.calendarViewDate().getMonth();
  }

  get currentMonthName(): string {
    return this.monthsList[this.currentMonthIndex];
  }

  get calendarDays(): CalendarDay[] {
    const viewDate = this.calendarViewDate();
    const year = viewDate.getFullYear();
    const month = viewDate.getMonth();

    const firstDayOfMonth = new Date(year, month, 1);
    let dayIndex = (firstDayOfMonth.getDay() + 6) % 7; // Monday = 0, ..., Sunday = 6

    const startDate = new Date(year, month, 1 - dayIndex);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const selectedVal = this.profileForm?.get('fechaNacimiento')?.value || '';

    const days: CalendarDay[] = [];
    let curr = new Date(startDate);

    for (let i = 0; i < 42; i++) {
      const dYear = curr.getFullYear();
      const dMonth = String(curr.getMonth() + 1).padStart(2, '0');
      const dDay = String(curr.getDate()).padStart(2, '0');
      const dateStr = `${dYear}-${dMonth}-${dDay}`;

      const currZero = new Date(curr);
      currZero.setHours(0, 0, 0, 0);

      days.push({
        date: new Date(curr),
        dateString: dateStr,
        dayNumber: curr.getDate(),
        isCurrentMonth: curr.getMonth() === month,
        isToday: currZero.getTime() === today.getTime(),
        isSelected: selectedVal === dateStr,
        isDisabled: currZero > today
      });

      curr.setDate(curr.getDate() + 1);
    }

    return days;
  }

  prevMonth(): void {
    const d = new Date(this.calendarViewDate());
    d.setMonth(d.getMonth() - 1);
    this.calendarViewDate.set(d);
  }

  nextMonth(): void {
    const d = new Date(this.calendarViewDate());
    d.setMonth(d.getMonth() + 1);
    this.calendarViewDate.set(d);
  }

  onYearChange(event: Event): void {
    const year = Number((event.target as HTMLSelectElement).value);
    const d = new Date(this.calendarViewDate());
    d.setFullYear(year);
    this.calendarViewDate.set(d);
  }

  onMonthChange(event: Event): void {
    const month = Number((event.target as HTMLSelectElement).value);
    const d = new Date(this.calendarViewDate());
    d.setMonth(month);
    this.calendarViewDate.set(d);
  }

  selectDay(day: CalendarDay): void {
    if (day.isDisabled) return;
    this.profileForm.get('fechaNacimiento')?.setValue(day.dateString);
    this.profileForm.get('fechaNacimiento')?.markAsTouched();

    if (!day.isCurrentMonth) {
      this.calendarViewDate.set(new Date(day.date));
    }
  }

  get formattedSelectedDate(): string {
    const val = this.profileForm?.get('fechaNacimiento')?.value;
    if (!val) return '';
    const parts = val.split('-');
    if (parts.length !== 3) return val;
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
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
