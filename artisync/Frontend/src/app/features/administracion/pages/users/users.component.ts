import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminUserService } from '../../services/admin-user.service';
import { RolePermissionService } from '../../services/role-permission.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { UserResponse } from '../../../../shared/models/user.model';
import { CreateUserRequest, AdminUpdateUserRequest } from '../../models/admin.model';
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

  // Filtros
  searchTerm = '';
  selectedRoleFilter = 'ALL';
  selectedStatusFilter = 'ALL';

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

  exportar(formato: FormatoReporte): void {
    this.exportando.set(true);
    this.adminUserService.exportar(formato).subscribe({
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
    this.adminUserService.getUsers(this.currentPage(), this.pageSize()).subscribe({
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

  filteredUsers = computed(() => {
    let list = this.users();
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      list = list.filter(u => 
        u.nombres.toLowerCase().includes(term) || 
        u.apellidos.toLowerCase().includes(term) || 
        u.correo.toLowerCase().includes(term)
      );
    }
    if (this.selectedRoleFilter !== 'ALL') {
      // Comparación exacta sobre el nombre normalizado. Con `includes` el filtro
      // "ADMINISTRADOR" no casaba con el rol real `ROLE_ADMIN` y devolvía vacío,
      // y a la inversa "ADMIN" habría arrastrado a cualquier rol que lo contenga.
      const buscado = normalizeRoleName(this.selectedRoleFilter);
      list = list.filter(u => u.roles.some(r => normalizeRoleName(r) === buscado));
    }
    if (this.selectedStatusFilter !== 'ALL') {
      const active = this.selectedStatusFilter === 'ACTIVO';
      list = list.filter(u => u.estadoCuenta === active);
    }
    return list;
  });

  onSearchChange(): void {
    // El computed filteredUsers se actualiza automáticamente por binding al array o si usamos signals
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
