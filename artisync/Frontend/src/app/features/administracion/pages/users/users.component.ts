import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminUserService } from '../../services/admin-user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { UserResponse } from '../../../../shared/models/user.model';
import { CreateUserRequest, AdminUpdateUserRequest } from '../../models/admin.model';
import { AvatarComponent } from '../../../../shared/components/avatar/avatar.component';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { UserFormModalComponent } from '../../../../shared/components/user-form-modal/user-form-modal.component';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [FormsModule, AvatarComponent, ConfirmDialogComponent, UserFormModalComponent],
  templateUrl: './users.component.html'
})
export class UsersComponent implements OnInit {
  private adminUserService = inject(AdminUserService);
  private toastService = inject(ToastService);
  authService = inject(AuthService);

  readonly users = signal<UserResponse[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly isLoading = signal<boolean>(false);
  readonly isActionLoading = signal<boolean>(false);

  // Filtros
  searchTerm = '';
  selectedRoleFilter = 'ALL';
  selectedStatusFilter = 'ALL';

  // Modales
  readonly isFormModalOpen = signal<boolean>(false);
  readonly formModalMode = signal<'create' | 'edit'>('create');
  readonly selectedUser = signal<UserResponse | null>(null);

  readonly isConfirmOpen = signal<boolean>(false);
  readonly confirmActionType = signal<'delete' | 'status'>('delete');

  ngOnInit(): void {
    this.loadUsers();
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
      list = list.filter(u => u.roles.some(r => r.toUpperCase().includes(this.selectedRoleFilter)));
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

  executeConfirmAction(): void {
    const user = this.selectedUser();
    if (!user) return;

    this.isActionLoading.set(true);

    if (this.confirmActionType() === 'status') {
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

  formatRoleBadge(role: string): { label: string, classes: string } {
    const r = role.replace('ROLE_', '').toUpperCase();
    switch (r) {
      case 'ADMINISTRADOR':
      case 'ADMIN':
        return { label: 'Administrador', classes: 'bg-error-container text-on-error-container font-medium' };
      case 'MODERADOR':
        return { label: 'Moderador', classes: 'bg-primary-container text-on-primary-container font-medium' };
      case 'SOPORTE':
        return { label: 'Soporte', classes: 'bg-tertiary-container text-on-tertiary-container font-medium' };
      case 'AUDITOR_FINANCIERO':
        return { label: 'Auditor Financiero', classes: 'bg-surface-container-highest text-on-surface font-medium' };
      case 'CREADOR':
        return { label: 'Creador', classes: 'bg-secondary-container text-on-secondary-container font-medium' };
      case 'CLIENTE':
      default:
        return { label: 'Cliente', classes: 'bg-surface-container-high text-on-surface-variant' };
    }
  }

  formatLastLogin(): string {
    // El backend actualmente tiene fechaRegistro pero podemos simular o usar fechaRegistro para visualización
    return 'Hace 2 horas';
  }
}
