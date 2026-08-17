import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, finalize, catchError, throwError, firstValueFrom, map, of, shareReplay } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../../../environments/environment';
import {
  LoginRequest, RegisterRequest, TwoFactorRequest,
  ForgotPasswordRequest, ResetPasswordRequest, TokenResponse,
  DecodedToken, TwoFactorSetupResponse, TwoFactorConfirmRequest
} from '../models/auth.model';
import { MessageResponse } from '../../../shared/models/common.model';
import { UserResponse } from '../../../shared/models/user.model';
import { NAV_CONFIG } from '../../../core/config/nav.config';

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

  /** Renovación de sesión en curso, compartida entre todos los 401 simultáneos. */
  private refreshEnVuelo: Observable<TokenResponse> | null = null;

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

  /**
   * Ruta de aterrizaje tras autenticarse: la primera entrada del menú del rol.
   * Se deriva de NAV_CONFIG para que añadir un panel nuevo (p. ej. el de creador)
   * no obligue a tocar también el redirect de login.
   */
  readonly homeRoute = computed(() => {
    const role = this.primaryRole();
    const config = role ? NAV_CONFIG[role] : null;
    if (!config) return '/dashboard/overview';
    const primeraRuta = config.items[0]?.route;
    return primeraRuta ? `${config.basePath}/${primeraRuta}` : config.basePath;
  });

  /**
   * Promesa compartida y cacheada del intento de restauración de sesión al arrancar
   * la app (vía cookie HttpOnly de refreshToken). Nunca rechaza — si el refresh falla,
   * refreshToken() ya limpia la sesión en su propio catchError. Los guards la esperan
   * antes de leer isLoggedIn()/userRoles(), evitando la carrera en la que el guard
   * síncrono se evaluaba antes de que esta petición asíncrona hubiera vuelto.
   */
  private readonly sessionRestored: Promise<void>;

  constructor() {
    // El intento de restauración NO puede lanzarse de forma síncrona aquí. Al
    // suscribirse, HttpClient ejecuta la cadena de interceptores en el acto, y
    // tanto authInterceptor como errorInterceptor hacen inject(AuthService).
    // Como este servicio todavía está a medio construir, Angular aborta con
    // NG0200 (dependencia circular): la petición a /auth/refresh nunca llega a
    // salir, el catchError se traga el error y la sesión queda vacía — de ahí
    // que un F5 en cualquier ruta protegida rebotara siempre a /auth/login.
    // Aplazarlo un microtask es suficiente: para entonces la instancia ya está
    // registrada en el inyector y los interceptores pueden resolverla.
    this.sessionRestored = Promise.resolve().then(() =>
      firstValueFrom(
        this.refreshToken().pipe(
          map(() => void 0),
          catchError(() => of(void 0))
        )
      )
    );
  }

  /** Resuelve en cuanto termina el intento de restauración de sesión (de inmediato si ya terminó). */
  waitForSessionRestore(): Promise<void> {
    return this.sessionRestored;
  }

  hasPermission(permissionCode: string): boolean {
    return this._userPermissions().includes(permissionCode);
  }

  hasAnyPermission(...codes: string[]): boolean {
    return codes.some(c => this._userPermissions().includes(c));
  }

  hasRole(role: string): boolean {
    const roles = this.userRoles();
    return roles.some(r => r === role || r === `ROLE_${role}`);
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  isAuthenticated(): boolean {
    return this.isLoggedIn();
  }

  /**
   * El id del usuario viaja en `sub`: JwtService lo fija al idUsuario cuando el
   * principal es un CustomUserDetails. Los claims `idUsuario`/`sub_id` que se
   * leían antes no los emite el backend, así que esto siempre devolvía null.
   *
   * Si el token es antiguo o el principal no tenía id, `sub` cae al email y el
   * parseo da NaN; en ese caso se devuelve null en vez de un id inventado.
   */
  getCurrentUserId(): number | null {
    const sub = this._currentUser()?.sub;
    if (!sub) return null;
    const id = Number(sub);
    return Number.isFinite(id) ? id : null;
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

  login(credentials: LoginRequest): Observable<TokenResponse> {
    this._isLoading.set(true);
    // withCredentials: obligatorio para que el navegador acepte el Set-Cookie
    // del ticket pre-auth de 2FA (preAuth2fa) y el de refreshToken cuando el
    // login es exitoso — §2.1 (OBS-AUTO-05).
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/login`, credentials, { withCredentials: true }).pipe(
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
    // withCredentials: envía la cookie preAuth2fa (que prueba que la contraseña
    // ya se validó en /auth/login) y recibe el Set-Cookie de refreshToken.
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/2fa/verify`, data, { withCredentials: true }).pipe(
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

  /**
   * Renovación de sesión con single-flight: todas las peticiones que fallen con
   * 401 a la vez comparten una única llamada a /auth/refresh.
   *
   * Es imprescindible porque el backend ROTA el refresh token: al renovar
   * revoca el jti anterior y emite uno nuevo (AuthServiceImpl §refrescar). Sin
   * esta guarda, dos 401 simultáneos lanzaban dos refresh; el primero rotaba el
   * token y el segundo llegaba con el jti ya borrado, respondía 401 y su
   * catchError ejecutaba clearSession(), tirando abajo una sesión que acababa
   * de renovarse correctamente.
   *
   * Se notaba sobre todo en pantallas con varios sondeos a la vez (el detalle
   * de pedido tiene el del seguimiento y el del chat), donde la coincidencia de
   * 401 es casi segura en cuanto caduca el access token.
   */
  refreshToken(): Observable<TokenResponse> {
    if (this.refreshEnVuelo) {
      return this.refreshEnVuelo;
    }

    this.refreshEnVuelo = this.http
      .post<TokenResponse>(`${environment.apiUrl}/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        tap(response => this.handleAuthentication(response)),
        catchError(err => {
          this.clearSession();
          return throwError(() => err);
        }),
        // La renovación deja de estar "en vuelo" al terminar, con éxito o no.
        finalize(() => this.refreshEnVuelo = null),
        // Sin esto, cada suscriptor dispararía su propio POST: shareReplay
        // multiplexa el mismo resultado a todos los que estén esperando.
        shareReplay({ bufferSize: 1, refCount: false })
      );

    return this.refreshEnVuelo;
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
