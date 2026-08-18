import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RolePermissionService } from '../../services/role-permission.service';
import { RolMatrixResponse, PermisoCatalogResponse } from '../../models/admin.model';
import { AuthService } from '../../../seguridad/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

export interface PastelStyle {
  bg: string;
  text: string;
  border: string;
  dot: string;
}

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

  // Módulo seleccionado para filtrado por fichas (Chips)
  readonly selectedModule = signal<string>('TODOS');

  // Paginación
  readonly itemsPerPage = 6;
  readonly availablePage = signal<number>(1);
  readonly assignedPage = signal<number>(1);

  // Elemento siendo arrastrado (Drag & Drop)
  readonly draggedCode = signal<string | null>(null);

  // Lista de módulos únicos para las fichas (chips) superiores
  readonly modulos = computed(() => {
    const set = new Set<string>();
    for (const p of this.permisos()) {
      set.add(p.moduloAplicacion || 'GENERAL');
    }
    return ['TODOS', ...Array.from(set)];
  });

  // Permisos disponibles (no asignados) según el módulo seleccionado
  readonly availablePermissions = computed(() => {
    const mod = this.selectedModule();
    const assigned = this.assignedPermissions();
    return this.permisos().filter(p => {
      const matchModule = mod === 'TODOS' || (p.moduloAplicacion || 'GENERAL') === mod;
      return matchModule && !assigned.has(p.nombrePermiso);
    });
  });

  // Permisos asignados según el módulo seleccionado
  readonly assignedPermissionsList = computed(() => {
    const mod = this.selectedModule();
    const assigned = this.assignedPermissions();
    return this.permisos().filter(p => {
      const matchModule = mod === 'TODOS' || (p.moduloAplicacion || 'GENERAL') === mod;
      return matchModule && assigned.has(p.nombrePermiso);
    });
  });

  // Paginación de Disponibles
  readonly availableTotalPages = computed(() => 
    Math.ceil(this.availablePermissions().length / this.itemsPerPage) || 1
  );

  readonly paginatedAvailable = computed(() => {
    const page = Math.min(this.availablePage(), this.availableTotalPages());
    const start = (page - 1) * this.itemsPerPage;
    return this.availablePermissions().slice(start, start + this.itemsPerPage);
  });

  // Paginación de Asignados
  readonly assignedTotalPages = computed(() => 
    Math.ceil(this.assignedPermissionsList().length / this.itemsPerPage) || 1
  );

  readonly paginatedAssigned = computed(() => {
    const page = Math.min(this.assignedPage(), this.assignedTotalPages());
    const start = (page - 1) * this.itemsPerPage;
    return this.assignedPermissionsList().slice(start, start + this.itemsPerPage);
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
    this.availablePage.set(1);
    this.assignedPage.set(1);
  }

  selectModule(mod: string): void {
    this.selectedModule.set(mod);
    this.availablePage.set(1);
    this.assignedPage.set(1);
  }

  // --- LÓGICA DE ASIGNACIÓN / DESASIGNACIÓN DE PERMISOS ---
  assignPermission(code: string): void {
    if (!this.canEdit()) return;
    this.assignedPermissions.update(current => {
      const next = new Set(current);
      next.add(code);
      return next;
    });
  }

  unassignPermission(code: string): void {
    if (!this.canEdit()) return;
    this.assignedPermissions.update(current => {
      const next = new Set(current);
      next.delete(code);
      return next;
    });
  }

  assignAllFiltered(): void {
    if (!this.canEdit()) return;
    const toAdd = this.availablePermissions().map(p => p.nombrePermiso);
    this.assignedPermissions.update(current => {
      const next = new Set(current);
      toAdd.forEach(code => next.add(code));
      return next;
    });
  }

  unassignAllFiltered(): void {
    if (!this.canEdit()) return;
    const toRemove = new Set(this.assignedPermissionsList().map(p => p.nombrePermiso));
    this.assignedPermissions.update(current => {
      const next = new Set(current);
      toRemove.forEach(code => next.delete(code));
      return next;
    });
  }

  // --- HTML5 DRAG AND DROP ---
  onDragStart(event: DragEvent, code: string): void {
    if (!this.canEdit()) return;
    this.draggedCode.set(code);
    event.dataTransfer?.setData('text/plain', code);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  onDragOver(event: DragEvent): void {
    if (!this.canEdit()) return;
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onDropToAssigned(event: DragEvent): void {
    if (!this.canEdit()) return;
    event.preventDefault();
    const code = this.draggedCode() || event.dataTransfer?.getData('text/plain');
    if (code) {
      this.assignPermission(code);
    }
    this.draggedCode.set(null);
  }

  onDropToAvailable(event: DragEvent): void {
    if (!this.canEdit()) return;
    event.preventDefault();
    const code = this.draggedCode() || event.dataTransfer?.getData('text/plain');
    if (code) {
      this.unassignPermission(code);
    }
    this.draggedCode.set(null);
  }

  // --- CONTROLES DE PAGINACIÓN ---
  prevAvailablePage(): void {
    if (this.availablePage() > 1) {
      this.availablePage.update(p => p - 1);
    }
  }

  nextAvailablePage(): void {
    if (this.availablePage() < this.availableTotalPages()) {
      this.availablePage.update(p => p + 1);
    }
  }

  prevAssignedPage(): void {
    if (this.assignedPage() > 1) {
      this.assignedPage.update(p => p - 1);
    }
  }

  nextAssignedPage(): void {
    if (this.assignedPage() < this.assignedTotalPages()) {
      this.assignedPage.update(p => p + 1);
    }
  }

  // --- PERSISTENCIA Y CREACIÓN DE ROLES ---
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
        this.roles.update(list => list.map(r => r.idRol === rol.idRol ? { ...r, permisos: codes } : r));
        this.selectedRole.update(r => r ? { ...r, permisos: codes } : r);
        this.refrescarPermisosPropiosSiAplica(rol.nombreRol);
      },
      error: () => {
        this.isSaving.set(false);
        this.toastService.error('Ocurrió un error al sincronizar los permisos');
      }
    });
  }

  /**
   * Si el rol que se acaba de editar es uno de los del propio usuario, vuelve a
   * pedir sus permisos para que el menú y los guards reflejen el cambio en el
   * acto.
   *
   * Hace falta porque los permisos se cachean en AuthService (y en
   * localStorage) a partir del claim `permisos` del JWT, que sigue siendo el
   * antiguo. El backend no tiene ese problema: JwtAuthenticationFilter recarga
   * los UserDetails desde la base en cada petición, así que GET
   * /api/permissions/me ya devuelve la lista nueva.
   */
  private refrescarPermisosPropiosSiAplica(nombreRol: string): void {
    const propio = this.authService.userRoles()
      .some(r => r.replace(/^ROLE_/, '').toUpperCase() === nombreRol.toUpperCase());
    if (!propio) return;

    this.authService.fetchUserPermissions().subscribe({
      next: () => this.toastService.success('Tus permisos se han actualizado'),
      error: () => this.toastService.error('Permisos guardados, pero no se pudieron recargar los tuyos. Vuelve a iniciar sesión.')
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
        const msg = err.error?.detail || 'Error al crear el rol';
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
        const msg = err.error?.detail || 'No se puede eliminar el rol';
        this.toastService.error(msg);
      }
    });
  }

  // --- MAPEO DE COLORES PASTEL ARMONIOSOS PARA ROLES Y MÓDULOS ---
  getRoleStyle(roleName: string): PastelStyle {
    switch (roleName.toUpperCase()) {
      case 'ADMIN':
      case 'ADMINISTRADOR':
        return { bg: 'bg-[#E2F0CB]', text: 'text-[#2D5A1E]', border: 'border-emerald-300', dot: 'bg-emerald-600' };
      case 'MODERADOR':
        return { bg: 'bg-[#FFD8B1]', text: 'text-[#7A3E00]', border: 'border-amber-300', dot: 'bg-amber-600' };
      case 'SOPORTE':
        return { bg: 'bg-[#E0F2FE]', text: 'text-[#0369A1]', border: 'border-sky-300', dot: 'bg-sky-600' };
      case 'AUDITOR_FINANCIERO':
        return { bg: 'bg-[#F3E8FF]', text: 'text-[#7E22CE]', border: 'border-purple-300', dot: 'bg-purple-600' };
      case 'CREADOR':
        return { bg: 'bg-[#FDE2E4]', text: 'text-[#7A1C28]', border: 'border-pink-300', dot: 'bg-pink-600' };
      case 'CLIENTE':
        return { bg: 'bg-[#CCFBF1]', text: 'text-[#0F766E]', border: 'border-teal-300', dot: 'bg-teal-600' };
      default:
        return { bg: 'bg-[#EAEFF9]', text: 'text-[#2D1B4E]', border: 'border-purple-200', dot: 'bg-purple-500' };
    }
  }

  getModuleStyle(moduleName: string): string {
    switch (moduleName.toUpperCase()) {
      case 'SEGURIDAD': return 'bg-[#E2F0CB] text-[#2D5A1E]';
      case 'SISTEMA': return 'bg-[#E0F2FE] text-[#0369A1]';
      case 'PORTAFOLIO': return 'bg-[#FFD8B1] text-[#7A3E00]';
      case 'CATALOGO': return 'bg-[#FDE2E4] text-[#7A1C28]';
      case 'PEDIDO': return 'bg-[#F3E8FF] text-[#7E22CE]';
      case 'COMUNICACION': return 'bg-[#CCFBF1] text-[#0F766E]';
      default: return 'bg-[#EAEFF9] text-[#2D1B4E]';
    }
  }

  formatPermissionName(code: string): string {
    const map: Record<string, string> = {
      'USUARIO_VER': 'Usuario ver',
      'USUARIO_CREAR': 'Usuario crear',
      'USUARIO_EDITAR': 'Usuario editar',
      'USUARIO_ELIMINAR': 'Usuario eliminar',
      'USUARIO_SUSPENDER': 'Usuario suspender',
      'ROL_VER': 'Rol ver',
      'ROL_GESTIONAR': 'Rol gestionar',
      'PERMISO_VER': 'Permiso ver',
      'ROL_ASIGNAR_PERMISO': 'Rol asignar permiso',
      'SESION_REVOCAR': 'Sesión revocar',
      'PAIS_VER': 'País ver',
      'PAIS_CREAR': 'País crear',
      'PAIS_EDITAR': 'País editar',
      'PAIS_ELIMINAR': 'País eliminar',
      'PORTAFOLIO_CREAR': 'Portafolio crear',
      'PORTAFOLIO_MODERAR': 'Portafolio moderar',
      'CERTIFICADO_REVISAR': 'Certificado revisar',
      'CATEGORIA_GESTIONAR': 'Categoría gestionar',
      'SERVICIO_CREAR': 'Servicio crear',
      'SERVICIO_MODERAR': 'Servicio moderar',
      'PEDIDO_CREAR': 'Pedido crear',
      'PEDIDO_GESTIONAR': 'Pedido gestionar',
      'TICKET_REVISAR': 'Ticket revisar',
      'TICKET_RESOLVER': 'Ticket resolver',
      'CONTRATO_VER': 'Contrato ver',
      'CONTRATO_FIRMAR': 'Contrato firmar',
      'PAGO_AUDITAR': 'Pago auditar',
      'FONDOS_LIBERAR': 'Fondos liberar',
      'TRANSACCION_VER': 'Transacción ver',
      'SALA_VER': 'Sala ver',
      'MENSAJE_ENVIAR': 'Mensaje enviar',
      'MENSAJE_MODERAR': 'Mensaje moderar',
      'NOTIFICACION_ENVIAR': 'Notificación enviar'
    };

    if (map[code]) return map[code];

    return code
      .toLowerCase()
      .split('_')
      .map((w, i) => i === 0 ? w.charAt(0).toUpperCase() + w.slice(1) : w)
      .join(' ');
  }

  getPermissionIconType(code: string): string {
    const c = code.toUpperCase();
    if (c.includes('VER') || c.includes('REVISAR') || c.includes('AUDITAR')) return 'eye';
    if (c.includes('CREAR')) return 'plus';
    if (c.includes('EDITAR') || c.includes('GESTIONAR')) return 'edit';
    if (c.includes('ELIMINAR') || c.includes('REVOCAR') || c.includes('SUSPENDER')) return 'trash';
    if (c.includes('ASIGNAR') || c.includes('RESOLVER') || c.includes('FIRMAR') || c.includes('LIBERAR')) return 'check';
    if (c.includes('ENVIAR') || c.includes('MENSAJE') || c.includes('NOTIFICACION')) return 'send';
    return 'shield';
  }
}
