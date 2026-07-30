export interface LoginRequest {
  correo: string;
  contrasena: string;
}

export interface RegisterRequest {
  nombres: string;
  apellidos: string;
  correo: string;
  contrasena: string;
  fechaNacimiento: string;
  rol?: string;
  aceptaTerminos: boolean;
}

export interface TwoFactorRequest {
  correo: string;
  codigo: string;
}

export interface ForgotPasswordRequest {
  correo: string;
}

export interface ResetPasswordRequest {
  token: string;
  nuevaContrasena: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  idUsuario: number;
  correo: string;
  roles: string[];
  permisos?: string[];
  requiere2fa: boolean;
}

export interface DecodedToken {
  sub: string;
  email?: string;
  rol?: string;
  roles: string[];
  permisos?: string[];
  exp: number;
  iat: number;
  jti?: string;
}

export interface TwoFactorSetupResponse {
  secreto: string;
  otpauthUri: string;
  codigosRespaldo: string[];
}

export interface TwoFactorConfirmRequest {
  codigo: string;
}
