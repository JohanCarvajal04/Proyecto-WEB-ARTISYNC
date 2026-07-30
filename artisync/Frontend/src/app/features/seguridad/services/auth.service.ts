import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, finalize, catchError, throwError } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../../../environments/environment';
import {
  LoginRequest, RegisterRequest, TwoFactorRequest,
  ForgotPasswordRequest, ResetPasswordRequest, TokenResponse,
  DecodedToken, TwoFactorSetupResponse, TwoFactorConfirmRequest
} from '../models/auth.model';
import { MessageResponse } from '../../../shared/models/common.model';
import { UserResponse } from '../../../shared/models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly _currentUser = signal<DecodedToken | null>(null);
  private readonly _accessToken = signal<string | null>(null);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _userPermissions = signal<string[]>(
    JSON.parse(localStorage.getItem('userPermissions') || '[]')
  );

  readonly currentUser = this._currentUser.asReadonly();
  readonly accessToken = this._accessToken.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly userPermissions = this._userPermissions.asReadonly();

  readonly isLoggedIn = computed(() => this._currentUser() !== null);
  readonly userRoles = computed(() => this._currentUser()?.roles ?? (this._currentUser()?.rol ? [this._currentUser()?.rol!] : []));
  readonly primaryRole = computed(() => {
    const roles = this.userRoles();
    if (roles.includes('ROLE_ADMIN') || roles.includes('ADMINISTRADOR') || roles.includes('ADMIN')) return 'ADMINISTRADOR';
    if (roles.includes('ROLE_MODERADOR') || roles.includes('MODERADOR')) return 'MODERADOR';
    if (roles.includes('ROLE_SOPORTE') || roles.includes('SOPORTE')) return 'SOPORTE';
    if (roles.includes('ROLE_AUDITOR_FINANCIERO') || roles.includes('AUDITOR_FINANCIERO')) return 'AUDITOR_FINANCIERO';
    if (roles.includes('ROLE_CREADOR') || roles.includes('CREADOR')) return 'CREADOR';
    if (roles.includes('ROLE_CLIENTE') || roles.includes('CLIENTE')) return 'CLIENTE';
    return roles[0] ? roles[0].replace('ROLE_', '') : null;
  });

  constructor() {
    this.tryRestoreSession();
  }

  hasPermission(permissionCode: string): boolean {
    return this._userPermissions().includes(permissionCode);
  }

  hasAnyPermission(...codes: string[]): boolean {
    return codes.some(c => this._userPermissions().includes(c));
  }

  fetchUserPermissions(): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}/permissions/me`).pipe(
      tap(permisos => {
        this._userPermissions.set(permisos);
        localStorage.setItem('userPermissions', JSON.stringify(permisos));
      }),
      catchError(err => {
        console.warn('No se pudieron obtener permisos remotos en /permissions/me', err);
        return throwError(() => err);
      })
    );
  }

  tryRestoreSession(): void {
    // Restaurar sesión silenciosamente mediante cookie HttpOnly de refreshToken
    this.refreshToken().subscribe({
      error: () => this.clearSession()
    });
  }

  login(credentials: LoginRequest): Observable<TokenResponse> {
    this._isLoading.set(true);
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap(response => this.handleAuthentication(response)),
      finalize(() => this._isLoading.set(false))
    );
  }

  register(data: RegisterRequest): Observable<UserResponse> {
    this._isLoading.set(true);
    return this.http.post<UserResponse>(`${environment.apiUrl}/auth/registro`, data).pipe(
      finalize(() => this._isLoading.set(false))
    );
  }

  verify2fa(data: TwoFactorRequest): Observable<TokenResponse> {
    this._isLoading.set(true);
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/2fa/verify`, data).pipe(
      tap(response => this.handleAuthentication(response)),
      finalize(() => this._isLoading.set(false))
    );
  }

  forgotPassword(data: ForgotPasswordRequest): Observable<MessageResponse> {
    this._isLoading.set(true);
    return this.http.post<MessageResponse>(`${environment.apiUrl}/auth/forgot-password`, data).pipe(
      finalize(() => this._isLoading.set(false))
    );
  }

  resetPassword(data: ResetPasswordRequest): Observable<MessageResponse> {
    this._isLoading.set(true);
    return this.http.post<MessageResponse>(`${environment.apiUrl}/auth/reset-password`, data).pipe(
      finalize(() => this._isLoading.set(false))
    );
  }

  setup2fa(): Observable<TwoFactorSetupResponse> {
    this._isLoading.set(true);
    return this.http.post<TwoFactorSetupResponse>(`${environment.apiUrl}/2fa/setup`, {}).pipe(
      finalize(() => this._isLoading.set(false))
    );
  }

  confirm2fa(data: TwoFactorConfirmRequest): Observable<MessageResponse> {
    this._isLoading.set(true);
    return this.http.post<MessageResponse>(`${environment.apiUrl}/2fa/confirm`, data).pipe(
      finalize(() => this._isLoading.set(false))
    );
  }

  disable2fa(data: TwoFactorConfirmRequest): Observable<MessageResponse> {
    this._isLoading.set(true);
    return this.http.delete<MessageResponse>(`${environment.apiUrl}/2fa/disable`, { body: data }).pipe(
      finalize(() => this._isLoading.set(false))
    );
  }

  refreshToken(): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/refresh`, {}, { withCredentials: true }).pipe(
      tap(response => this.handleAuthentication(response)),
      catchError(err => {
        this.clearSession();
        return throwError(() => err);
      })
    );
  }

  logout(): void {
    this.http.post<MessageResponse>(`${environment.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe({
      next: () => this.clearSessionAndRedirect(),
      error: () => this.clearSessionAndRedirect()
    });
  }

  private handleAuthentication(response: TokenResponse): void {
    if (response.requiere2fa) {
      // Si requiere 2FA no guardamos token aún, se redirigirá desde la vista
      return;
    }
    if (response.accessToken) {
      this._accessToken.set(response.accessToken);
      try {
        const decoded = jwtDecode<DecodedToken>(response.accessToken);
        this._currentUser.set(decoded);
        const permisos = decoded.permisos ?? response.permisos ?? [];
        if (permisos.length > 0) {
          this._userPermissions.set(permisos);
          localStorage.setItem('userPermissions', JSON.stringify(permisos));
        } else {
          this.fetchUserPermissions().subscribe();
        }
      } catch (e) {
        console.error('Error decoding JWT', e);
      }
    }
  }

  private clearSession(): void {
    this._accessToken.set(null);
    this._currentUser.set(null);
    this._userPermissions.set([]);
    localStorage.removeItem('userPermissions');
  }

  private clearSessionAndRedirect(): void {
    this.clearSession();
    this.router.navigate(['/auth/login']);
  }
}
