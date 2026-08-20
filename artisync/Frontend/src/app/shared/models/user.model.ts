export interface PaisResponse {
  idPais: number;
  nombrePais: string;
  estado: boolean;
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

/**
 * Los roles son datos administrables, no un conjunto cerrado: el administrador
 * crea los suyos desde "Roles y Permisos" y el backend los acepta tal cual
 * (`CreateUserRequest.roles` es `List<String>`, sin enum ni @Pattern). La unión
 * de literales anterior no aportaba seguridad real y dejaba fuera esos roles.
 */
export type UserRole = string;
export type UserStatus = 'Activo' | 'Pendiente' | 'Suspendido';
