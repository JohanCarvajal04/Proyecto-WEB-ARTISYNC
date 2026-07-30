export interface PaisResponse {
  idPais: number;
  nombrePais: string;
}

export interface UserResponse {
  idUsuario: number;
  nombres: string;
  apellidos: string;
  correo: string;
  fechaNacimiento: string;
  idPais?: number;
  nombrePais?: string;
  fechaRegistro: string;
  estadoCuenta: boolean;
  roles: string[];
  permisos?: string[];
  dosFactoresHabilitado: boolean;
}

export type UserRole = 'ADMINISTRADOR' | 'ADMIN' | 'CREADOR' | 'CLIENTE' | 'MODERADOR' | 'SOPORTE' | 'AUDITOR_FINANCIERO';
export type UserStatus = 'Activo' | 'Pendiente' | 'Suspendido';
