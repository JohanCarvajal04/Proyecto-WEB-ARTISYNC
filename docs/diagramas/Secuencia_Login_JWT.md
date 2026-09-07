# Sequence Diagram - Login, 2FA and JWT Session Lifecycle (Artisync PFC)

This diagram traces the real login/authentication flow implemented in the backend,
based on the actual call chain (not a generic textbook flow):

- `AuthController.login()` / `verify2Fa()` / `refresh()` / `logout()` —
  `controller/seguridad/AuthController.java`
- `AuthServiceImpl` — `service/seguridad/impl/AuthServiceImpl.java`
- `TwoFactorServiceImpl.validarCodigoOBackup()` —
  `service/seguridad/impl/TwoFactorServiceImpl.java`
- `JwtAuthenticationFilter.doFilterInternal()` —
  `security/JwtAuthenticationFilter.java`
- `SessionRevocationService` — Redis-backed JWT blacklist with fail-closed behavior on
  Redis outage.

## 0. Combined flow (this is the diagram exported to `secuencia_login_jwt.png`)

```mermaid
sequenceDiagram
    actor Client
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthServiceImpl
    participant Ticket as PreAuth2faTicketService
    participant TwoFa as TwoFactorServiceImpl
    participant UDS as CustomUserDetailsService
    participant JwtSvc as JwtService
    participant SessionRepo as SesionUsuarioRepository
    participant Filter as JwtAuthenticationFilter
    participant Revoke as SessionRevocationService
    participant Redis

    Client->>AuthCtrl: POST /auth/login (email, password)
    AuthCtrl->>AuthSvc: login(request)
    AuthSvc->>AuthSvc: authenticate + resolverEstadoLogin(email)

    alt 2FA disabled
        AuthSvc->>UDS: loadUserByUsername(email)
        AuthSvc->>JwtSvc: generarToken() / generarRefreshToken()
        JwtSvc-->>AuthSvc: accessToken, refreshToken
        AuthSvc->>SessionRepo: save(SesionUsuario)
        AuthSvc-->>Client: 200 OK + Set-Cookie refreshToken
    else 2FA enabled
        AuthSvc->>Ticket: emitir(idUsuario, email)
        Ticket-->>AuthSvc: preAuthTicket
        AuthSvc-->>Client: 200 OK requiere2fa=true + Set-Cookie preAuth2fa
        Client->>AuthCtrl: POST /auth/verify-2fa (code)
        AuthCtrl->>AuthSvc: verify2Fa(preAuth2fa cookie, code)
        AuthSvc->>Ticket: resolver(preAuthTicket)
        AuthSvc->>TwoFa: validarCodigoOBackup(email, code)
        TwoFa-->>AuthSvc: valid
        AuthSvc->>Ticket: consumir(preAuthTicket)
        AuthSvc->>UDS: loadUserByUsername(email)
        AuthSvc->>JwtSvc: generarToken() / generarRefreshToken()
        JwtSvc-->>AuthSvc: accessToken, refreshToken
        AuthSvc->>SessionRepo: save(SesionUsuario)
        AuthSvc-->>Client: 200 OK + Set-Cookie refreshToken
    end

    Note over Client,Redis: Every subsequent authenticated request
    Client->>Filter: Authorization: Bearer accessToken
    Filter->>Redis: EXISTS jti:<jti>
    alt Redis unavailable
        Redis--xFilter: connection error
        Filter-->>Client: 503 fail-closed (deny access)
    else jti blacklisted
        Filter-->>Client: 401 Unauthorized (revoked token)
    else jti valid
        Filter->>JwtSvc: esAccessTokenValido(token)
        Filter->>Filter: populate SecurityContext
        Filter-->>Client: request proceeds
    end

    Note over Client,Redis: Refresh and logout
    Client->>AuthCtrl: POST /auth/refresh (refreshToken cookie)
    AuthCtrl->>AuthSvc: refreshToken(token)
    AuthSvc->>Revoke: revocarToken(oldJti)
    Revoke->>Redis: SET jti:<oldJti>=revocado
    AuthSvc->>SessionRepo: delete(oldSession) + save(newSession)
    AuthSvc-->>Client: 200 OK new accessToken/refreshToken

    Client->>AuthCtrl: POST /auth/logout
    AuthCtrl->>AuthSvc: logout(accessToken, refreshToken)
    AuthSvc->>Revoke: revocarTokenPorCabecera(accessToken) + revocarToken(refreshToken)
    Revoke->>Redis: SET jti:<accessJti>=revocado / jti:<refreshJti>=revocado
    AuthSvc->>SessionRepo: deleteByJti(refreshJti)
    AuthSvc-->>Client: 200 OK (cookies cleared)
```

---

## Detalle por escenario (referencia, no exportado como figura independiente)

### 1. Login without 2FA, and per-request JWT validation

```mermaid
sequenceDiagram
    actor Client
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthServiceImpl
    participant AuthMgr as AuthenticationManager
    participant Quota as IntentosAutenticacionService
    participant UserRepo as UsuarioRepository
    participant UDS as CustomUserDetailsService
    participant JwtSvc as JwtService
    participant SessionRepo as SesionUsuarioRepository
    participant Filter as JwtAuthenticationFilter
    participant Redis

    Client->>AuthCtrl: POST /auth/login (email, password)
    AuthCtrl->>AuthSvc: login(request)
    AuthSvc->>AuthMgr: authenticate(credentials)
    alt invalid credentials
        AuthMgr-->>AuthSvc: AuthenticationException
        AuthSvc->>Quota: verificarCuota(email)
        Quota-->>AuthSvc: attempt registered / account locked
        AuthSvc-->>Client: 401 Unauthorized
    else valid credentials
        AuthMgr-->>AuthSvc: authenticated principal
        AuthSvc->>UserRepo: resolverEstadoLogin(email)
        UserRepo-->>AuthSvc: idUsuario, dosFactoresHabilitado, roles
        Note over AuthSvc: dosFactoresHabilitado = false, continue below
        AuthSvc->>UDS: loadUserByUsername(email)
        UDS-->>AuthSvc: UserDetails
        AuthSvc->>JwtSvc: generarToken(userDetails)
        JwtSvc-->>AuthSvc: accessToken (jti, type=access)
        AuthSvc->>JwtSvc: generarRefreshToken(userDetails)
        JwtSvc-->>AuthSvc: refreshToken (jti)
        AuthSvc->>SessionRepo: save(SesionUsuario)
        SessionRepo-->>AuthSvc: persisted
        AuthSvc-->>AuthCtrl: accessToken, refreshToken
        AuthCtrl-->>Client: 200 OK + Set-Cookie refreshToken
    end

    Note over Client,Redis: Every subsequent authenticated request
    Client->>Filter: GET/POST ... (Authorization: Bearer accessToken)
    Filter->>Filter: extract token, require claim type=access
    Filter->>Redis: EXISTS jti:<jti>
    alt Redis unavailable
        Redis--xFilter: connection error
        Filter-->>Client: 503 fail-closed (deny access)
    else jti blacklisted
        Redis-->>Filter: true
        Filter-->>Client: 401 Unauthorized (revoked token)
    else jti not blacklisted
        Redis-->>Filter: false
        Filter->>JwtSvc: esAccessTokenValido(token)
        JwtSvc-->>Filter: valid
        Filter->>Filter: populate SecurityContext
        Filter-->>Client: request proceeds to controller
    end
```

## 2. Login with 2FA enabled

```mermaid
sequenceDiagram
    actor Client
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthServiceImpl
    participant UserRepo as UsuarioRepository
    participant Ticket as PreAuth2faTicketService
    participant TwoFa as TwoFactorServiceImpl
    participant UDS as CustomUserDetailsService
    participant JwtSvc as JwtService
    participant SessionRepo as SesionUsuarioRepository

    Client->>AuthCtrl: POST /auth/login (email, password)
    AuthCtrl->>AuthSvc: login(request)
    AuthSvc->>UserRepo: resolverEstadoLogin(email)
    UserRepo-->>AuthSvc: idUsuario, dosFactoresHabilitado=true, roles
    AuthSvc->>Ticket: emitir(idUsuario, email)
    Ticket-->>AuthSvc: preAuthTicket (one-time)
    AuthSvc-->>AuthCtrl: requiere2fa=true, preAuthTicket
    AuthCtrl-->>Client: 200 OK + Set-Cookie preAuth2fa (no JWT yet)

    Client->>AuthCtrl: POST /auth/verify-2fa (code)
    AuthCtrl->>AuthSvc: verify2Fa(preAuth2fa cookie, code)
    AuthSvc->>Ticket: resolver(preAuthTicket)
    Ticket-->>AuthSvc: idUsuario, email
    AuthSvc->>UserRepo: resolverEstadoLogin(email)
    UserRepo-->>AuthSvc: current login state
    AuthSvc->>TwoFa: validarCodigoOBackup(email, code)
    alt invalid code
        TwoFa-->>AuthSvc: invalid
        AuthSvc-->>Client: 401 Unauthorized
    else valid code
        TwoFa-->>AuthSvc: valid
        AuthSvc->>Ticket: consumir(preAuthTicket)
        AuthSvc->>UDS: loadUserByUsername(email)
        UDS-->>AuthSvc: UserDetails
        AuthSvc->>JwtSvc: generarToken() / generarRefreshToken()
        JwtSvc-->>AuthSvc: accessToken, refreshToken
        AuthSvc->>SessionRepo: save(SesionUsuario)
        AuthSvc-->>AuthCtrl: accessToken, refreshToken
        AuthCtrl-->>Client: 200 OK + Set-Cookie refreshToken
    end
```

## 3. Refresh token and logout (session revocation)

```mermaid
sequenceDiagram
    actor Client
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthServiceImpl
    participant SessionRepo as SesionUsuarioRepository
    participant JwtSvc as JwtService
    participant Revoke as SessionRevocationService
    participant Redis

    Client->>AuthCtrl: POST /auth/refresh (refreshToken cookie)
    AuthCtrl->>AuthSvc: refreshToken(token)
    AuthSvc->>SessionRepo: findByJti(jti)
    SessionRepo-->>AuthSvc: session found
    AuthSvc->>JwtSvc: validar(refreshToken)
    JwtSvc-->>AuthSvc: valid
    AuthSvc->>Revoke: revocarToken(oldJti)
    Revoke->>Redis: SET jti:<oldJti>=revocado (TTL = remaining expiry)
    AuthSvc->>SessionRepo: delete(oldSession)
    AuthSvc->>JwtSvc: generarToken() / generarRefreshToken() (new pair)
    AuthSvc-->>AuthCtrl: new accessToken, refreshToken
    AuthCtrl-->>Client: 200 OK + Set-Cookie refreshToken

    Client->>AuthCtrl: POST /auth/logout
    AuthCtrl->>AuthSvc: logout(accessToken, refreshToken)
    AuthSvc->>Revoke: revocarTokenPorCabecera(accessToken)
    Revoke->>Redis: SET jti:<accessJti>=revocado
    AuthSvc->>Revoke: revocarToken(refreshToken)
    Revoke->>Redis: SET jti:<refreshJti>=revocado
    AuthSvc->>SessionRepo: deleteByJti(refreshJti)
    AuthSvc-->>AuthCtrl: logged out
    AuthCtrl-->>Client: 200 OK (cookies cleared)
```
