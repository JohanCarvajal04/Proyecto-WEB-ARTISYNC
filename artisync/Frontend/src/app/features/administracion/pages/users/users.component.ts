import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminUserService } from '../../services/admin-user.service';
import { RolePermissionService } from '../../services/role-permission.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { UserResponse } from '../../../../shared/models/user.model';
import { CreateUserRequest, AdminUpdateUserRequest, FiltroUsuario } from '../../models/admin.model';
import { getRoleDisplay, getRoleLabel, normalizeRoleName, RoleDisplay } from '../../../../core/constants/role-display';
import { AvatarComponent } from '../../../../shared/components/avatar/avatar.component';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { UserFormModalComponent } from '../../../../shared/components/user-form-modal/user-form-modal.component';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { BotonExportarComponent } from '../../../../shared/components/boton-exportar/boton-exportar.component';
import { FormatoReporte } from '../../../../shared/models/formato-reporte.model';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [FormsModule, AvatarComponent, ConfirmDialogComponent, UserFormModalComponent, HasPermissionDirective, BotonExportarComponent],
  templateUrl: './users.component.html'
})
export class UsersComponent implements OnInit {
  private adminUserService = inject(AdminUserService);
  private rolePermissionService = inject(RolePermissionService);
  private toastService = inject(ToastService);
  authService = inject(AuthService);

  readonly users = signal<UserResponse[]>([]);
  /** Opciones del filtro por rol, traídas de la BD (antes eran 6 <option> fijos). */
  readonly rolesFiltro = signal<{ key: string; label: string }[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly isLoading = signal<boolean>(false);
  readonly isActionLoading = signal<boolean>(false);
  readonly exportando = signal<boolean>(false);

  // Borrador de filtros: lo que el usuario está escribiendo/eligiendo en
  // pantalla. No se manda al backend hasta que pulsa "Filtrar" — así el
  // listado no se recarga a media escritura.
  searchTerm = '';
  selectedRoleFilter = 'ALL';
  selectedStatusFilter = 'ALL';

  /**
   * Filtros efectivamente aplicados. Se separa del borrador porque paginar y
   * exportar deben usar lo que la tabla está mostrando, no lo que el usuario
   * dejó a medio escribir sin pulsar "Filtrar".
   */
  private readonly filtroAplicado = signal<FiltroUsuario>({});

  /**
   * Distingue "no hay usuarios" de "ningún usuario coincide con el filtro",
   * para no invitar a "ajustar los filtros" cuando no hay ninguno puesto.
   * Solo lee la señal `filtroAplicado` (los campos del borrador son planos y
   * no notificarían a un computed en una app zoneless).
   */
  readonly hayFiltrosAplicados = computed(() => {
    const f = this.filtroAplicado();
    return f.busqueda !== undefined || f.rol !== undefined || f.estadoCuenta !== undefined;
  });

  // Modales
  readonly isFormModalOpen = signal<boolean>(false);
  readonly formModalMode = signal<'create' | 'edit'>('create');
  readonly selectedUser = signal<UserResponse | null>(null);

  readonly isConfirmOpen = signal<boolean>(false);
  readonly confirmActionType = signal<'delete' | 'status' | 'sessions'>('delete');

  ngOnInit(): void {
    this.loadUsers();
    this.loadRolesFiltro();
  }

  /** Espejo de FiltroUsuario.java: 'ALL' es el sentinel de "sin filtro" en los <select>. */
  private filtroDesdeBorrador(): FiltroUsuario {
    return {
      busqueda: this.searchTerm.trim() || undefined,
      rol: this.selectedRoleFilter !== 'ALL' ? this.selectedRoleFilter : undefined,
      estadoCuenta: this.selectedStatusFilter === 'ALL' ? undefined : this.selectedStatusFilter === 'ACTIVO'
    };
  }

  /** Aplica el borrador y vuelve a la primera página (el filtro cambia el total). */
  aplicarFiltros(): void {
    this.filtroAplicado.set(this.filtroDesdeBorrador());
    this.currentPage.set(0);
    this.loadUsers();
  }

  limpiarFiltros(): void {
    this.searchTerm = '';
    this.selectedRoleFilter = 'ALL';
    this.selectedStatusFilter = 'ALL';
    this.aplicarFiltros();
  }

  exportar(formato: FormatoReporte): void {
    this.exportando.set(true);
    this.adminUserService.exportar(this.filtroAplicado(), formato).subscribe({
      next: (respuesta) => {
        this.exportando.set(false);
        descargarRespuesta(respuesta, `usuarios.${formato.toLowerCase()}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        const mensaje = await mensajeErrorBlob(err, 'No se pudo exportar el listado de usuarios');
        this.toastService.error(mensaje);
      }
    });
  }

  private loadRolesFiltro(): void {
    this.rolePermissionService.getAllRoles().subscribe({
      next: (roles) => this.rolesFiltro.set(
        roles.map(r => ({ key: normalizeRoleName(r.nombreRol), label: getRoleLabel(r.nombreRol) }))
      ),
      error: () => this.rolesFiltro.set([])
    });
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.adminUserService.getUsers(this.filtroAplicado(), this.currentPage(), this.pageSize()).subscribe({
      next: (res) => {
        this.users.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  changePage(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages()) {
      this.currentPage.set(newPage);
      this.loadUsers();
    }
  }

  openCreateModal(): void {
    this.formModalMode.set('create');
    this.selectedUser.set(null);
    this.isFormModalOpen.set(true);
  }

  openEditModal(user: UserResponse): void {
    this.formModalMode.set('edit');
    this.selectedUser.set(user);
    this.isFormModalOpen.set(true);
  }

  handleCreate(request: CreateUserRequest): void {
    this.isActionLoading.set(true);
    this.adminUserService.createUser(request).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.isFormModalOpen.set(false);
        this.toastService.success('Usuario creado exitosamente');
        this.loadUsers();
      },
      error: () => {
        this.isActionLoading.set(false);
      }
    });
  }

  handleEdit(request: AdminUpdateUserRequest): void {
    const user = this.selectedUser();
    if (!user) return;

    this.isActionLoading.set(true);
    this.adminUserService.updateUser(user.idUsuario, request).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.isFormModalOpen.set(false);
        this.toastService.success('Usuario actualizado exitosamente');
        this.loadUsers();
      },
      error: () => {
        this.isActionLoading.set(false);
      }
    });
  }

  confirmToggleStatus(user: UserResponse): void {
    this.selectedUser.set(user);
    this.confirmActionType.set('status');
    this.isConfirmOpen.set(true);
  }

  confirmDelete(user: UserResponse): void {
    this.selectedUser.set(user);
    this.confirmActionType.set('delete');
    this.isConfirmOpen.set(true);
  }

  /** Cierra las sesiones activas del usuario sin tocar su cuenta. */
  confirmRevokeSessions(user: UserResponse): void {
    this.selectedUser.set(user);
    this.confirmActionType.set('sessions');
    this.isConfirmOpen.set(true);
  }

  executeConfirmAction(): void {
    const user = this.selectedUser();
    if (!user) return;

    this.isActionLoading.set(true);

    if (this.confirmActionType() === 'sessions') {
      this.adminUserService.revokeSessions(user.idUsuario).subscribe({
        next: (res) => {
          this.isActionLoading.set(false);
          this.isConfirmOpen.set(false);
          this.toastService.success(res.message || res.mensaje || 'Sesiones revocadas');
        },
        error: (err) => {
          this.isActionLoading.set(false);
          this.toastService.error(err.error?.message || 'No se pudieron revocar las sesiones');
        }
      });
    } else if (this.confirmActionType() === 'status') {
      const nuevoEstado = !user.estadoCuenta;
      this.adminUserService.changeEstado(user.idUsuario, { estadoCuenta: nuevoEstado }).subscribe({
        next: () => {
          this.isActionLoading.set(false);
          this.isConfirmOpen.set(false);
          this.toastService.success(`Cuenta ${nuevoEstado ? 'activada' : 'suspendida'} exitosamente`);
          this.loadUsers();
        },
        error: () => this.isActionLoading.set(false)
      });
    } else {
      this.adminUserService.deleteUser(user.idUsuario).subscribe({
        next: () => {
          this.isActionLoading.set(false);
          this.isConfirmOpen.set(false);
          this.toastService.success('Usuario eliminado exitosamente');
          this.loadUsers();
        },
        error: () => this.isActionLoading.set(false)
      });
    }
  }

  /**
   * El `switch` anterior caía en `default: Cliente`, así que cualquier rol
   * creado por el administrador se pintaba como Cliente en la tabla.
   */
  formatRoleBadge(role: string): RoleDisplay {
    return getRoleDisplay(role);
  }

  formatLastLogin(): string {
    // El backend actualmente tiene fechaRegistro pero podemos simular o usar fechaRegistro para visualización
    return 'Hace 2 horas';
  }
}
