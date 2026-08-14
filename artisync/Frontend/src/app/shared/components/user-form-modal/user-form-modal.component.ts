import { Component, input, output, effect, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { UserResponse, PaisResponse } from '../../models/user.model';
import { CreateUserRequest, AdminUpdateUserRequest } from '../../../features/administracion/models/admin.model';
import { AdminUserService } from '../../../features/administracion/services/admin-user.service';

@Component({
  selector: 'app-user-form-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    @if (isOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-fadeIn">
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 max-w-lg w-full shadow-xl flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
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
                <input formControlName="contrasena" type="password" placeholder="Mínimo 8 caracteres" 
                  class="bg-surface text-on-surface border border-outline-variant rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
              </div>
            }

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-on-surface">Fecha de nacimiento</label>
                <input formControlName="fechaNacimiento" type="date" [max]="maxDate"
                  class="bg-surface text-on-surface border border-outline-variant rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                @if ((form.get('fechaNacimiento')?.touched || form.get('fechaNacimiento')?.dirty) && form.get('fechaNacimiento')?.errors?.['underage']) {
                  <span class="text-xs text-error mt-1">Debe tener al menos 18 años</span>
                }
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-on-surface">País</label>
                <select formControlName="idPais" 
                  class="bg-surface text-on-surface border border-outline-variant rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                  <option [ngValue]="null">Seleccione un país</option>
                  @for (pais of paises(); track pais.idPais) {
                    <option [ngValue]="pais.idPais">{{ pais.nombrePais }}</option>
                  }
                </select>
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-on-surface">Rol *</label>
                <select formControlName="rol" 
                  class="bg-surface text-on-surface border border-outline-variant rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                  <option value="CLIENTE">Cliente</option>
                  <option value="CREADOR">Creador</option>
                  <option value="MODERADOR">Moderador</option>
                  <option value="SOPORTE">Soporte Técnico</option>
                  <option value="AUDITOR_FINANCIERO">Auditor Financiero</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </div>
            </div>

            <div class="flex items-center justify-between mt-2">
              <div class="flex items-center gap-2">
                <input formControlName="estadoCuenta" type="checkbox" id="estadoCuenta" class="rounded border-outline-variant text-primary focus:ring-primary">
                <label for="estadoCuenta" class="text-sm text-on-surface select-none">Cuenta activa</label>
              </div>
            </div>

            @if (mode() === 'edit' && userToEdit()) {
              <div class="mt-1 p-3 rounded-lg border border-outline-variant bg-surface-container-low flex flex-col gap-2">
                <div class="flex items-center justify-between">
                  <span class="text-xs font-medium text-on-surface">Autenticación 2FA:</span>
                  @if (userToEdit()?.dosFactoresHabilitado) {
                    <span class="px-2 py-0.5 rounded text-xs font-medium bg-tertiary-container text-on-tertiary-container">Habilitado</span>
                  } @else {
                    <span class="px-2 py-0.5 rounded text-xs font-medium bg-surface-container-high text-on-surface-variant">Deshabilitado</span>
                  }
                </div>
                @if (userToEdit()?.dosFactoresHabilitado) {
                  <div class="flex items-center gap-2 pt-1 border-t border-outline-variant/50">
                    <input type="checkbox" id="deshabilitar2Fa" [checked]="deshabilitar2Fa()" (change)="deshabilitar2Fa.set(!deshabilitar2Fa())"
                      class="rounded border-error text-error focus:ring-error">
                    <label for="deshabilitar2Fa" class="text-xs text-error font-medium select-none cursor-pointer">
                      Desactivar 2FA (restablecer acceso al usuario)
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

    effect(() => {
      if (this.isOpen()) {
        this.adminUserService.getPaises().subscribe({
          next: (list) => this.paises.set(list),
          error: () => {}
        });
        this.deshabilitar2Fa.set(false);

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
          this.form.get('correo')?.enable();
          this.form.get('contrasena')?.setValidators([Validators.required, Validators.minLength(8)]);
          this.form.get('contrasena')?.updateValueAndValidity();
        }
      }
    });
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
