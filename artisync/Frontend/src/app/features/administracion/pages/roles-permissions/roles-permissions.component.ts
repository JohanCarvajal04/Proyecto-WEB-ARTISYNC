import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RolePermissionService } from '../../services/role-permission.service';
import { RolMatrixResponse, PermisoCatalogResponse } from '../../models/admin.model';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-roles-permissions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roles-permissions.component.html'
})
export class RolesPermissionsComponent implements OnInit {
  private rolePermissionService = inject(RolePermissionService);
  authService = inject(AuthService);
  private toastService = inject(ToastService);

  readonly roles = signal<RolMatrixResponse[]>([]);
  readonly permisos = signal<PermisoCatalogResponse[]>([]);
  readonly selectedRole = signal<RolMatrixResponse | null>(null);
  readonly assignedPermissions = signal<Set<string>>(new Set());
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);

  readonly permisosPorModulo = computed(() => {
    const map = new Map<string, PermisoCatalogResponse[]>();
    for (const p of this.permisos()) {
      const mod = p.moduloAplicacion || 'GENERAL';
      if (!map.has(mod)) {
        map.set(mod, []);
      }
      map.get(mod)!.push(p);
    }
    return Array.from(map.entries()).map(([modulo, lista]) => ({ modulo, lista }));
  });

  readonly canEdit = computed(() => 
    this.authService.hasPermission('ROL_ASIGNAR_PERMISO') || 
    this.authService.primaryRole() === 'ADMINISTRADOR'
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.rolePermissionService.getAllPermisos().subscribe({
      next: (permList) => {
        this.permisos.set(permList);
        this.rolePermissionService.getAllRoles().subscribe({
          next: (rolList) => {
            this.roles.set(rolList);
            if (rolList.length > 0) {
              this.selectRole(rolList[0]);
            }
            this.isLoading.set(false);
          },
          error: () => {
            this.toastService.error('Error al cargar la lista de roles');
            this.isLoading.set(false);
          }
        });
      },
      error: () => {
        this.toastService.error('Error al cargar el catálogo de permisos');
        this.isLoading.set(false);
      }
    });
  }

  selectRole(rol: RolMatrixResponse): void {
    this.selectedRole.set(rol);
    this.assignedPermissions.set(new Set(rol.permisos || []));
  }

  togglePermission(code: string): void {
    if (!this.canEdit()) return;
    this.assignedPermissions.update(current => {
      const next = new Set(current);
      if (next.has(code)) {
        next.delete(code);
      } else {
        next.add(code);
      }
      return next;
    });
  }

  hasPermissionAssigned(code: string): boolean {
    return this.assignedPermissions().has(code);
  }

  isModuleAllSelected(lista: PermisoCatalogResponse[]): boolean {
    if (lista.length === 0) return false;
    return lista.every(p => this.assignedPermissions().has(p.nombrePermiso));
  }

  toggleModule(lista: PermisoCatalogResponse[]): void {
    if (!this.canEdit()) return;
    const allSelected = this.isModuleAllSelected(lista);
    this.assignedPermissions.update(current => {
      const next = new Set(current);
      lista.forEach(p => {
        if (allSelected) {
          next.delete(p.nombrePermiso);
        } else {
          next.add(p.nombrePermiso);
        }
      });
      return next;
    });
  }

  savePermissions(): void {
    const rol = this.selectedRole();
    if (!rol || !this.canEdit()) return;

    this.isSaving.set(true);
    const codes = Array.from(this.assignedPermissions());

    this.rolePermissionService.syncPermissions({
      roleName: rol.nombreRol,
      permissionCodes: codes
    }).subscribe({
      next: (res) => {
        this.isSaving.set(false);
        this.toastService.success(res.mensaje || res.message || 'Matriz de permisos actualizada exitosamente');
        // Actualizar el rol seleccionado localmente
        this.roles.update(list => list.map(r => r.idRol === rol.idRol ? { ...r, permisos: codes } : r));
        this.selectedRole.update(r => r ? { ...r, permisos: codes } : r);
      },
      error: () => {
        this.isSaving.set(false);
        this.toastService.error('Ocurrió un error al sincronizar los permisos');
      }
    });
  }

  readonly showCreateModal = signal<boolean>(false);
  readonly newRoleName = signal<string>('');
  readonly newRoleDesc = signal<string>('');
  readonly isCreating = signal<boolean>(false);

  openCreateModal(): void {
    this.newRoleName.set('');
    this.newRoleDesc.set('');
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  createRole(): void {
    const nombre = this.newRoleName().trim().toUpperCase();
    if (!nombre || !/^[A-Z0-9_]+$/.test(nombre)) {
      this.toastService.error('El nombre del rol debe estar en mayúsculas (ej: SUPERVISOR) y sin caracteres especiales');
      return;
    }
    this.isCreating.set(true);
    this.rolePermissionService.createRole({
      nombreRol: nombre,
      descripcionRol: this.newRoleDesc().trim(),
      permisosIniciales: []
    }).subscribe({
      next: (nuevoRol) => {
        this.isCreating.set(false);
        this.showCreateModal.set(false);
        this.toastService.success(`Rol ${nuevoRol.nombreRol} creado exitosamente`);
        this.roles.update(list => [...list, nuevoRol]);
        this.selectRole(nuevoRol);
      },
      error: (err) => {
        this.isCreating.set(false);
        const msg = err.error?.mensaje || err.error?.message || 'Error al crear el rol';
        this.toastService.error(msg);
      }
    });
  }

  isSystemRole(rol: RolMatrixResponse | null): boolean {
    if (!rol) return false;
    const sys = ['ADMIN', 'CLIENTE', 'CREADOR', 'MODERADOR', 'SOPORTE', 'AUDITOR_FINANCIERO'];
    return sys.includes(rol.nombreRol.toUpperCase());
  }

  deleteSelectedRole(): void {
    const rol = this.selectedRole();
    if (!rol || this.isSystemRole(rol)) return;
    if (!confirm(`¿Estás seguro de que deseas eliminar el rol ${rol.nombreRol}? Esta acción no se puede deshacer.`)) return;

    this.rolePermissionService.deleteRole(rol.idRol).subscribe({
      next: () => {
        this.toastService.success(`Rol ${rol.nombreRol} eliminado`);
        this.roles.update(list => list.filter(r => r.idRol !== rol.idRol));
        const rest = this.roles();
        if (rest.length > 0) {
          this.selectRole(rest[0]);
        } else {
          this.selectedRole.set(null);
        }
      },
      error: (err) => {
        const msg = err.error?.mensaje || err.error?.message || 'No se puede eliminar el rol';
        this.toastService.error(msg);
      }
    });
  }
}
