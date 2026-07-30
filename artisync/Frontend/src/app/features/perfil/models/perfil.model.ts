export interface UpdateUserRequest {
  nombres?: string;
  apellidos?: string;
  fechaNacimiento?: string;
  idPais?: number;
}

export interface ChangePasswordRequest {
  contrasenaActual: string;
  nuevaContrasena: string;
}
