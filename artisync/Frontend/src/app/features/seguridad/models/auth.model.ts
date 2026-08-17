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
  // §2.1 (OBS-AUTO-05): el correo ya no viaja aquí — el backend resuelve el
  // usuario desde el ticket pre-auth (cookie HttpOnly "preAuth2fa") emitido
  // por /auth/login tras validar la contraseña.
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
  // Cuando requiere2fa=true, el backend solo envía correo/idUsuario/requiere2fa
  // (el ticket pre-auth va en cookie HttpOnly, no en este body) — de ahí que
  // el resto de campos sean opcionales.
  accessToken?: string;
  tokenType?: string;
  idUsuario?: number;
  correo?: string;
  roles?: string[];
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
