export interface CreateUserRequest {
  nombres: string;
  apellidos: string;
  correo: string;
  contrasena: string;
  fechaNacimiento?: string;
  idPais?: number;
  roles?: string[];
  estadoCuenta?: boolean;
}

export interface AdminUpdateUserRequest {
  nombres?: string;
  apellidos?: string;
  fechaNacimiento?: string;
  idPais?: number;
  estadoCuenta?: boolean;
  roles?: string[];
  dosFactoresHabilitado?: boolean;
}

export interface AssignRolesRequest {
  roles: string[];
}

export interface ChangeEstadoRequest {
  estadoCuenta: boolean;
}

export interface RolMatrixResponse {
  idRol: number;
  nombreRol: string;
  descripcionRol: string;
  permisos: string[];
}

export interface PermisoCatalogResponse {
  idPermiso: number;
  nombrePermiso: string;
  moduloAplicacion: string;
}

export interface SyncPermissionsRequest {
  roleName: string;
  permissionCodes: string[];
}

export interface CreateRoleRequest {
  nombreRol: string;
  descripcionRol?: string;
  permisosIniciales?: string[];
}

export interface UpdateRoleRequest {
  descripcionRol?: string;
}
