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
import { FormatoReporte, TipoGraficaReporte, OPCIONES_GRAFICA_REPORTE, FORMATOS_REPORTE, OpcionesExportacion, TOPES_FORMATO } from '../../../../shared/models/formato-reporte.model';
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

  // Filtros aplicados efectivamente: estos son los que usa loadUsers() y
  // exportar(). Cambian únicamente cuando el usuario pulsa "Filtrar" o
  // "Limpiar". Así garantizamos que lo que se ve en la tabla y lo que sale en
  // el reporte coincidan exactamente.
  readonly filtroAplicado = signal<FiltroUsuario>({
    busqueda: undefined,
    rol: undefined,
    estadoCuenta: undefined,
  });

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

  // Modal de exportación enriquecida
  readonly isExportModalOpen = signal<boolean>(false);
  readonly exportFormato = signal<FormatoReporte>('PDF');
  readonly exportGrafica = signal<TipoGraficaReporte>('AMBAS');
  readonly opcionesGrafica = OPCIONES_GRAFICA_REPORTE;
  readonly formatosReporte = FORMATOS_REPORTE;

  iniciarExportacion(opcion: FormatoReporte | OpcionesExportacion): void {
    if (typeof opcion === 'string') {
      this.exportFormato.set(opcion);
      this.isExportModalOpen.set(true);
    } else {
      this.procesarExportacionPaginada(opcion);
    }
  }

  cerrarModalExportar(): void {
    this.isExportModalOpen.set(false);
  }

  confirmarExportacion(): void {
    const formato = this.exportFormato();
    const grafica = this.exportGrafica();
    this.cerrarModalExportar();

    const tope = TOPES_FORMATO[formato];
    if (tope && this.totalElements() > tope) {
      this.exportar(formato, grafica, 0, tope);
      return;
    }
    this.exportar(formato, grafica);
  }

  private procesarExportacionPaginada(opcion: OpcionesExportacion): void {
    if (opcion.todasLasPartes) {
      this.descargarTodasLasPartes(opcion.formato, opcion.size);
    } else {
      this.exportar(opcion.formato, undefined, opcion.page, opcion.size);
    }
  }

  exportar(formato: FormatoReporte, grafica?: TipoGraficaReporte, page?: number, size?: number): void {
    this.exportando.set(true);
    const graficaElegida = grafica ?? (formato === 'CSV' ? 'NINGUNA' : this.exportGrafica());
    this.adminUserService.exportar(this.filtroAplicado(), formato, graficaElegida, page, size).subscribe({
      next: (respuesta) => {
        this.exportando.set(false);
        const sufijo = page !== undefined ? `_parte_${page + 1}` : '';
        descargarRespuesta(respuesta, `usuarios${sufijo}.${formato.toLowerCase()}`);
        this.toastService.success(`Reporte de usuarios exportado exitosamente en ${formato}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        const mensaje = await mensajeErrorBlob(err, 'No se pudo exportar el listado de usuarios');
        this.toastService.error(mensaje);
      }
    });
  }

  private descargarTodasLasPartes(formato: FormatoReporte, tamanoLote?: number): void {
    const total = this.totalElements();
    const tope = tamanoLote ?? TOPES_FORMATO[formato] ?? 5000;
    const totalPartes = Math.max(1, Math.ceil(total / tope));
    let parteActual = 0;

    this.exportando.set(true);
    this.toastService.info(`Iniciando descarga de ${totalPartes} partes (${formato})...`);

    const descargarSiguiente = () => {
      if (parteActual >= totalPartes) {
        this.exportando.set(false);
        this.toastService.success(`Se completó la descarga de las ${totalPartes} partes de usuarios.`);
        return;
      }

      const paginaADescargar = parteActual;
      parteActual++;

      this.adminUserService.exportar(this.filtroAplicado(), formato, this.exportGrafica(), paginaADescargar, tope).subscribe({
        next: (respuesta) => {
          descargarRespuesta(respuesta, `usuarios_parte_${paginaADescargar + 1}_de_${totalPartes}.${formato.toLowerCase()}`);
          setTimeout(descargarSiguiente, 700);
        },
        error: async (err) => {
          this.exportando.set(false);
          const mensaje = await mensajeErrorBlob(err, `Error al descargar la parte ${paginaADescargar + 1}`);
          this.toastService.error(mensaje);
        }
      });
    };

    descargarSiguiente();
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
      error: (err) => {
        this.isLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo cargar el listado de usuarios');
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
      error: (err) => {
        this.isActionLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo crear el usuario');
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
      error: (err) => {
        this.isActionLoading.set(false);
        this.toastService.error(err.error?.detail || 'No se pudo actualizar el usuario');
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
          this.toastService.error(err.error?.detail || 'No se pudieron revocar las sesiones');
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
        error: (err) => {
          this.isActionLoading.set(false);
          this.toastService.error(err.error?.detail || 'No se pudo cambiar el estado de la cuenta');
        }
      });
    } else {
      this.adminUserService.deleteUser(user.idUsuario).subscribe({
        next: () => {
          this.isActionLoading.set(false);
          this.isConfirmOpen.set(false);
          this.toastService.success('Usuario eliminado exitosamente');
          this.loadUsers();
        },
        error: (err) => {
          this.isActionLoading.set(false);
          this.toastService.error(err.error?.detail || 'No se pudo eliminar el usuario');
        }
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
