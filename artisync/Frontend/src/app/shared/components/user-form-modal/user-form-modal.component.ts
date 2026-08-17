import { Component, input, output, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { UserResponse, PaisResponse } from '../../models/user.model';
import { CreateUserRequest, AdminUpdateUserRequest } from '../../../features/administracion/models/admin.model';
import { AdminUserService } from '../../../features/administracion/services/admin-user.service';

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
  selector: 'app-user-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    @if (isOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-fadeIn overflow-y-auto" (click)="closeAllPopups()">
        <div (click)="$event.stopPropagation()" class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 max-w-lg w-full my-auto shadow-2xl flex flex-col gap-5 relative overflow-visible">
          <div class="flex items-center justify-between border-b border-outline-variant pb-4">
            <h3 class="font-headline text-xl font-bold text-primary">
              {{ mode() === 'create' ? 'Crear Nuevo Usuario' : 'Editar Usuario' }}
            </h3>
            <button (click)="onCancel.emit()" class="text-on-surface-variant hover:text-primary p-1">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-on-surface">Nombres *</label>
                <input formControlName="nombres" type="text" placeholder="Ej: Elena" 
                  class="bg-surface text-on-surface border border-outline-variant rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-on-surface">Apellidos *</label>
                <input formControlName="apellidos" type="text" placeholder="Ej: Rodriguez" 
                  class="bg-surface text-on-surface border border-outline-variant rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
              </div>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-medium text-on-surface">Correo electrónico *</label>
              <input formControlName="correo" type="email" placeholder="ejemplo@artisync.com" [readonly]="mode() === 'edit'"
                [class.opacity-60]="mode() === 'edit'"
                class="bg-surface text-on-surface border border-outline-variant rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
            </div>

            @if (mode() === 'create') {
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-on-surface">Contraseña *</label>
                <div class="relative flex items-center w-full">
                  <input 
                    formControlName="contrasena" 
                    [type]="showModalPassword() ? 'text' : 'password'" 
                    placeholder="Mínimo 8 caracteres" 
                    class="w-full bg-surface text-on-surface border border-outline-variant rounded-lg px-3 py-2 pr-10 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                  <button
                    type="button"
                    (click)="showModalPassword.set(!showModalPassword())"
                    class="absolute right-3 text-slate-400 hover:text-purple-600 focus:outline-none transition-colors cursor-pointer"
                    [title]="showModalPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                    @if (!showModalPassword()) {
                      <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    } @else {
                      <svg class="w-4 h-4 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858-5.908a9.954 9.954 0 012.122-.363c4.478 0 8.268 2.943 9.542 7a10.025 10.025 0 01-4.132 5.411m-4.24-4.24a3 3 0 11-4.243-4.243M3 3l18 18" />
                      </svg>
                    }
                  </button>
                </div>
              </div>
            }

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              
              <!-- Floating Inline Datepicker -->
              <div class="flex flex-col gap-1.5 relative">
                <label class="text-xs font-medium text-on-surface">Fecha de nacimiento</label>
                <button 
                  type="button" 
                  (click)="toggleDatePicker($event)"
                  class="w-full flex items-center justify-between px-3 py-2 text-sm bg-surface border border-outline-variant rounded-lg text-on-surface hover:bg-slate-50 transition-colors focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                  <span [ngClass]="formattedSelectedDate ? 'text-slate-800 font-medium' : 'text-slate-400'">
                    {{ formattedSelectedDate ? formattedSelectedDate : 'dd/mm/aaaa' }}
                  </span>
                  <svg class="w-4 h-4 text-purple-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </button>

                <!-- Floating Inline Datepicker Popover -->
                @if (isDatePickerOpen()) {
                  <div 
                    (click)="$event.stopPropagation()"
                    class="absolute z-50 top-full left-0 mt-1 p-3 bg-white rounded-xl shadow-xl border border-slate-200 w-72 animate-fadeIn flex flex-col gap-3">
                    
                    <!-- Month & Year Controls -->
                    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                      <button 
                        type="button" 
                        (click)="prevMonth()" 
                        class="p-1 rounded-lg text-slate-500 hover:bg-slate-100 transition-colors">
                        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
                        </svg>
                      </button>
                      
                      <div class="flex items-center gap-1">
                        <select 
                          [value]="currentMonthIndex" 
                          (change)="onMonthChange($event)"
                          class="text-xs font-semibold py-1 px-1.5 rounded border border-slate-200 bg-slate-50 text-slate-700 cursor-pointer">
                          <option *ngFor="let m of monthsList; let idx = index" [value]="idx">{{ m }}</option>
                        </select>
                        <select 
                          [value]="currentYear" 
                          (change)="onYearChange($event)"
                          class="text-xs font-semibold py-1 px-1.5 rounded border border-slate-200 bg-slate-50 text-slate-700 cursor-pointer">
                          <option *ngFor="let y of yearsList()" [value]="y">{{ y }}</option>
                        </select>
                      </div>

                      <button 
                        type="button" 
                        (click)="nextMonth()" 
                        class="p-1 rounded-lg text-slate-500 hover:bg-slate-100 transition-colors">
                        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                        </svg>
                      </button>
                    </div>

                    <!-- Weekdays -->
                    <div class="grid grid-cols-7 text-center text-xs font-semibold text-slate-400">
                      <span *ngFor="let w of weekDays" class="py-0.5">{{ w }}</span>
                    </div>

                    <!-- Days Grid -->
                    <div class="grid grid-cols-7 gap-1 text-center">
                      <button 
                        *ngFor="let day of calendarDays" 
                        type="button"
                        (click)="selectDay(day)"
                        [disabled]="day.isDisabled"
                        class="h-7 w-7 mx-auto flex items-center justify-center text-xs rounded-lg transition-all font-medium select-none"
                        [ngClass]="{
                          'text-slate-300 pointer-events-none opacity-40': day.isDisabled,
                          'bg-purple-600 text-white font-bold shadow-md': day.isSelected,
                          'border border-purple-400 font-bold text-purple-600': day.isToday && !day.isSelected,
                          'text-slate-700 hover:bg-purple-50 hover:text-purple-700': !day.isSelected && !day.isToday && day.isCurrentMonth && !day.isDisabled,
                          'text-slate-400 hover:bg-slate-100': !day.isSelected && !day.isToday && !day.isCurrentMonth && !day.isDisabled
                        }">
                        {{ day.dayNumber }}
                      </button>
                    </div>
                  </div>
                }

                @if ((form.get('fechaNacimiento')?.touched || form.get('fechaNacimiento')?.dirty) && form.get('fechaNacimiento')?.errors?.['underage']) {
                  <span class="text-xs text-error mt-1">Debe tener al menos 18 años</span>
                }
              </div>

              <!-- Floating Dropdown for País -->
              <div class="flex flex-col gap-1.5 relative">
                <label class="text-xs font-medium text-on-surface">País</label>
                <button 
                  type="button"
                  data-dropdown-toggle="dropdownPais"
                  (click)="toggleDropdown('pais', $event)"
                  class="w-full flex items-center justify-between px-3 py-2 text-sm bg-surface border border-outline-variant rounded-lg text-on-surface hover:bg-slate-50 transition-colors focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                  <span class="truncate pr-1">{{ selectedPaisName() }}</span>
                  <svg class="w-4 h-4 text-slate-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                @if (activeDropdown() === 'pais') {
                  <div 
                    id="dropdownPais"
                    (click)="$event.stopPropagation()"
                    class="absolute z-50 top-full left-0 right-0 mt-1 max-h-52 overflow-y-auto bg-white rounded-xl shadow-xl border border-slate-200 p-1 flex flex-col gap-0.5 animate-fadeIn">
                    <button 
                      type="button" 
                      (click)="selectPais(null)" 
                      class="w-full text-left px-3 py-2 text-xs rounded-lg hover:bg-purple-50 hover:text-purple-700 transition-colors"
                      [class.bg-purple-50]="form.get('idPais')?.value === null">
                      Seleccione un país
                    </button>
                    @for (pais of paises(); track pais.idPais) {
                      <button 
                        type="button" 
                        (click)="selectPais(pais.idPais)"
                        class="w-full text-left px-3 py-2 text-xs font-medium rounded-lg hover:bg-purple-50 hover:text-purple-700 transition-colors flex items-center justify-between"
                        [class.bg-purple-50]="form.get('idPais')?.value === pais.idPais"
                        [class.text-purple-700]="form.get('idPais')?.value === pais.idPais">
                        <span>{{ pais.nombrePais }}</span>
                        @if (form.get('idPais')?.value === pais.idPais) {
                          <svg class="w-4 h-4 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                          </svg>
                        }
                      </button>
                    }
                  </div>
                }
              </div>

              <!-- Floating Dropdown for Rol -->
              <div class="flex flex-col gap-1.5 relative">
                <label class="text-xs font-medium text-on-surface">Rol *</label>
                <button 
                  type="button"
                  data-dropdown-toggle="dropdownRol"
                  (click)="toggleDropdown('rol', $event)"
                  class="w-full flex items-center justify-between px-3 py-2 text-sm bg-surface border border-outline-variant rounded-lg text-on-surface hover:bg-slate-50 transition-colors focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                  <span class="truncate font-medium pr-1">{{ selectedRolLabel() }}</span>
                  <svg class="w-4 h-4 text-slate-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                @if (activeDropdown() === 'rol') {
                  <div 
                    id="dropdownRol"
                    (click)="$event.stopPropagation()"
                    class="absolute z-50 top-full left-0 right-0 mt-1 bg-white rounded-xl shadow-xl border border-slate-200 p-1 flex flex-col gap-0.5 animate-fadeIn">
                    @for (r of rolesList; track r.key) {
                      <button 
                        type="button" 
                        (click)="selectRol(r.key)"
                        class="w-full text-left px-3 py-2 text-xs font-medium rounded-lg hover:bg-purple-50 hover:text-purple-700 transition-colors flex items-center justify-between"
                        [class.bg-purple-50]="form.get('rol')?.value === r.key"
                        [class.text-purple-700]="form.get('rol')?.value === r.key">
                        <span>{{ r.label }}</span>
                        @if (form.get('rol')?.value === r.key) {
                          <svg class="w-4 h-4 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                          </svg>
                        }
                      </button>
                    }
                  </div>
                }
              </div>

            </div>

            <!-- Toggle Switch for Cuenta Activa -->
            <div class="flex items-center justify-between mt-2 pt-2 border-t border-slate-100">
              <label class="relative inline-flex items-center cursor-pointer gap-3 select-none">
                <input formControlName="estadoCuenta" type="checkbox" id="estadoCuenta" class="sr-only peer">
                <div class="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-purple-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-purple-600"></div>
                <span class="text-sm font-medium text-slate-700">Cuenta activa</span>
              </label>
            </div>

            @if (mode() === 'edit' && userToEdit()) {
              <div class="mt-1 p-3 rounded-xl border border-outline-variant bg-surface-container-low flex flex-col gap-2">
                <div class="flex items-center justify-between">
                  <span class="text-xs font-medium text-on-surface">Autenticación 2FA:</span>
                  @if (userToEdit()?.dosFactoresHabilitado) {
                    <span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-800">Habilitado</span>
                  } @else {
                    <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600">Deshabilitado</span>
                  }
                </div>
                @if (userToEdit()?.dosFactoresHabilitado) {
                  <!-- Toggle Switch for Desactivar 2FA -->
                  <div class="flex items-center gap-2 pt-2 border-t border-outline-variant/50">
                    <label class="relative inline-flex items-center cursor-pointer gap-2.5 select-none">
                      <input type="checkbox" id="deshabilitar2Fa" [checked]="deshabilitar2Fa()" (change)="deshabilitar2Fa.set(!deshabilitar2Fa())" class="sr-only peer">
                      <div class="w-9 h-5 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-rose-600"></div>
                      <span class="text-xs text-rose-600 font-medium">
                        Desactivar 2FA (restablecer acceso al usuario)
                      </span>
                    </label>
                  </div>
                }
              </div>
            }

            <div class="flex justify-end gap-3 border-t border-outline-variant pt-4 mt-2">
              <button type="button" (click)="onCancel.emit()" 
                class="px-4 py-2 rounded-lg border border-outline-variant text-on-surface text-sm font-medium hover:bg-surface-container transition-colors">
                Cancelar
              </button>
              <button type="submit" [disabled]="form.invalid || isLoading()"
                class="px-5 py-2 rounded-lg bg-primary text-on-primary text-sm font-medium hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2">
                @if (isLoading()) {
                  <div class="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></div>
                }
                <span>{{ mode() === 'create' ? 'Crear Usuario' : 'Guardar Cambios' }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class UserFormModalComponent {
  private fb = inject(FormBuilder);
  private adminUserService = inject(AdminUserService);

  isOpen = input<boolean>(false);
  mode = input<'create' | 'edit'>('create');
  userToEdit = input<UserResponse | null>(null);
  isLoading = input<boolean>(false);

  onSubmitCreate = output<CreateUserRequest>();
  onSubmitEdit = output<AdminUpdateUserRequest>();
  onCancel = output<void>();

  readonly paises = signal<PaisResponse[]>([]);
  readonly deshabilitar2Fa = signal<boolean>(false);
  readonly showModalPassword = signal<boolean>(false);
  readonly isDatePickerOpen = signal<boolean>(false);
  readonly activeDropdown = signal<'pais' | 'rol' | null>(null);

  readonly calendarViewDate = signal<Date>(new Date(2000, 0, 1));
  readonly monthsList = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
  ];
  readonly weekDays = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá', 'Do'];
  readonly yearsList = signal<number[]>([]);

  readonly rolesList = [
    { key: 'CLIENTE', label: 'Cliente' },
    { key: 'CREADOR', label: 'Creador' },
    { key: 'MODERADOR', label: 'Moderador' },
    { key: 'SOPORTE', label: 'Soporte Técnico' },
    { key: 'AUDITOR_FINANCIERO', label: 'Auditor Financiero' },
    { key: 'ADMIN', label: 'Administrador' }
  ];

  maxDate: string;

  form: FormGroup = this.fb.group({
    nombres: ['', [Validators.required, Validators.maxLength(100)]],
    apellidos: ['', [Validators.required, Validators.maxLength(100)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    contrasena: ['', [Validators.minLength(8)]],
    fechaNacimiento: ['', [this.ageValidator]],
    idPais: [null as number | null],
    rol: ['CLIENTE', Validators.required],
    estadoCuenta: [true]
  });

  ageValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    const birthDate = new Date(control.value);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age >= 18 ? null : { underage: true };
  }

  constructor() {
    const today = new Date();
    today.setFullYear(today.getFullYear() - 18);
    this.maxDate = today.toISOString().split('T')[0];

    const currentYr = new Date().getFullYear();
    const yrs: number[] = [];
    for (let y = currentYr; y >= 1930; y--) {
      yrs.push(y);
    }
    this.yearsList.set(yrs);

    effect(() => {
      if (this.isOpen()) {
        this.adminUserService.getPaises().subscribe({
          next: (list) => this.paises.set(list),
          error: () => {}
        });
        this.deshabilitar2Fa.set(false);
        this.closeAllPopups();

        const u = this.userToEdit();
        if (this.mode() === 'edit' && u) {
          this.form.patchValue({
            nombres: u.nombres,
            apellidos: u.apellidos,
            correo: u.correo,
            fechaNacimiento: u.fechaNacimiento || '',
            idPais: u.idPais || null,
            rol: u.roles[0] ? u.roles[0].replace('ROLE_', '') : 'CLIENTE',
            estadoCuenta: u.estadoCuenta
          });
          if (u.fechaNacimiento) {
            const parts = u.fechaNacimiento.split('-');
            if (parts.length === 3) {
              this.calendarViewDate.set(new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])));
            }
          }
          this.form.get('correo')?.disable();
          this.form.get('contrasena')?.clearValidators();
          this.form.get('contrasena')?.updateValueAndValidity();
        } else {
          this.form.reset({
            nombres: '',
            apellidos: '',
            correo: '',
            contrasena: '',
            fechaNacimiento: '',
            idPais: null,
            rol: 'CLIENTE',
            estadoCuenta: true
          });
          this.calendarViewDate.set(new Date(2000, 0, 1));
          this.form.get('correo')?.enable();
          this.form.get('contrasena')?.setValidators([Validators.required, Validators.minLength(8)]);
          this.form.get('contrasena')?.updateValueAndValidity();
        }
      }
    });
  }

  // --- FLOATING POPUP CONTROLS ---
  closeAllPopups(): void {
    this.isDatePickerOpen.set(false);
    this.activeDropdown.set(null);
  }

  toggleDatePicker(event: Event): void {
    event.stopPropagation();
    const state = this.isDatePickerOpen();
    this.closeAllPopups();
    this.isDatePickerOpen.set(!state);
  }

  toggleDropdown(name: 'pais' | 'rol', event: Event): void {
    event.stopPropagation();
    const current = this.activeDropdown();
    this.closeAllPopups();
    this.activeDropdown.set(current === name ? null : name);
  }

  selectPais(id: number | null): void {
    this.form.get('idPais')?.setValue(id);
    this.activeDropdown.set(null);
  }

  selectRol(rolKey: string): void {
    this.form.get('rol')?.setValue(rolKey);
    this.activeDropdown.set(null);
  }

  selectedPaisName(): string {
    const val = this.form.get('idPais')?.value;
    if (val === null || val === undefined) return 'Seleccione un país';
    const found = this.paises().find(p => p.idPais === Number(val));
    return found ? found.nombrePais : 'Seleccione un país';
  }

  selectedRolLabel(): string {
    const val = this.form.get('rol')?.value;
    const found = this.rolesList.find(r => r.key === val);
    return found ? found.label : 'Cliente';
  }

  // --- FLOATING DATEPICKER LOGIC ---
  get currentYear(): number {
    return this.calendarViewDate().getFullYear();
  }

  get currentMonthIndex(): number {
    return this.calendarViewDate().getMonth();
  }

  get calendarDays(): CalendarDay[] {
    const viewDate = this.calendarViewDate();
    const year = viewDate.getFullYear();
    const month = viewDate.getMonth();

    const firstDayOfMonth = new Date(year, month, 1);
    let dayIndex = (firstDayOfMonth.getDay() + 6) % 7;

    const startDate = new Date(year, month, 1 - dayIndex);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const selectedVal = this.form?.get('fechaNacimiento')?.value || '';

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
    this.form.get('fechaNacimiento')?.setValue(day.dateString);
    this.form.get('fechaNacimiento')?.markAsTouched();
    this.isDatePickerOpen.set(false);
  }

  get formattedSelectedDate(): string {
    const val = this.form?.get('fechaNacimiento')?.value;
    if (!val) return '';
    const parts = val.split('-');
    if (parts.length !== 3) return val;
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }

  submit(): void {
    if (this.form.invalid) return;

    const val = this.form.getRawValue();
    const roles = [val.rol];
    const idPaisVal = val.idPais !== null && val.idPais !== undefined && val.idPais !== '' ? Number(val.idPais) : (this.mode() === 'edit' ? 0 : undefined);

    if (this.mode() === 'create') {
      const req: CreateUserRequest = {
        nombres: val.nombres,
        apellidos: val.apellidos,
        correo: val.correo,
        contrasena: val.contrasena,
        fechaNacimiento: val.fechaNacimiento ? val.fechaNacimiento : undefined,
        idPais: val.idPais ? Number(val.idPais) : undefined,
        roles: roles,
        estadoCuenta: val.estadoCuenta
      };
      this.onSubmitCreate.emit(req);
    } else {
      const req: AdminUpdateUserRequest = {
        nombres: val.nombres,
        apellidos: val.apellidos,
        fechaNacimiento: val.fechaNacimiento ? val.fechaNacimiento : undefined,
        idPais: idPaisVal,
        roles: roles,
        estadoCuenta: val.estadoCuenta,
        dosFactoresHabilitado: this.deshabilitar2Fa() ? false : undefined
      };
      this.onSubmitEdit.emit(req);
    }
  }
}
