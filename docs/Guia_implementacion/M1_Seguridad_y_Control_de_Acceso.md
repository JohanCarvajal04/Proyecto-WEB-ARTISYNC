# Módulo 1: Seguridad y Control de Acceso

## Descripción General

Este módulo es el **fundamento de toda la plataforma ARTISYNC**. Gestiona el registro de usuarios, la autenticación mediante JWT, el control de acceso basado en roles (RBAC), la recuperación de contraseñas, y la autenticación de dos factores (2FA). Todos los demás módulos dependen de este para la verificación de identidad y autorización.

---

## Requisitos Funcionales Cubiertos

### RF-01 — Registro de Usuarios con Selección de Rol

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El sistema debe permitir el registro de nuevos usuarios con selección de rol (Creador o Cliente). El rol elegido determina el conjunto de vistas y acciones disponibles durante toda la sesión. |
| **Criterio de Aceptación** | Un usuario registrado como Creador no puede acceder a rutas exclusivas del rol Cliente y viceversa; el sistema redirige al panel correspondiente ante cualquier intento de acceso no autorizado. |

**Estado actual:** ✅ Implementado
- `RegisterRequest` ya incluye campo `rol` (CLIENTE o CREADOR)
- `AuthServiceImpl.register()` asigna rol y crea `PerfilCreador` automáticamente si es CREADOR
- Validación de rol permitido (solo CLIENTE o CREADOR)

**Gaps pendientes:**
- [ ] Agregar campo `aceptaTerminos` (boolean, obligatorio `@NotNull`) al `RegisterRequest` → validar que sea `true` (RNF-12)
- [ ] En `SecurityConfig`, las rutas protegidas por rol deben usar `@PreAuthorize("hasRole('CREADOR')")` y `@PreAuthorize("hasRole('CLIENTE')")` en cada controller según corresponda
- [ ] Validar en el **frontend** que las rutas redirigen al panel correspondiente si el rol no coincide (Angular Guards)

---

### RF-02 — Control de Acceso Basado en Roles (RBAC)

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | Cada acción de la aplicación debe estar asociada a un permiso específico asignado al rol correspondiente. |
| **Criterio de Aceptación** | Al asignar un nuevo permiso a un rol desde el panel de administración, los usuarios con ese rol pueden ejecutar la acción sin reiniciar sesión; al revocar el permiso, el acceso se deniega en la siguiente solicitud. |

**Estado actual:** ✅ Implementado
- Tablas: `roles`, `permisos`, `rol_permisos`, `usuario_roles`
- `RolePermissionController` + `PermissionController` para CRUD admin
- `CustomUserDetailsService` carga roles y permisos al autenticar
- JWT contiene claims `roles` y `permisos`
- `@EnableMethodSecurity` habilitado en `SecurityConfig`

**Gaps pendientes:**
- [ ] Verificar que los permisos se validan **dinámicamente** en cada solicitud (no cacheados en el JWT). Actualmente los permisos se leen del JWT, por lo que un cambio de permisos solo surte efecto tras re-login. Para cumplir el criterio estrictamente, considerar leer los permisos de la BD en cada request (trade-off de rendimiento)
- [ ] Alternativa viable: usar JWT de corta duración (ya configurado) + invalidar tokens viejos al cambiar permisos

---

### RF-03 — Sesiones JWT con Expiración de 24 Horas

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | Gestionar sesiones mediante JSON Web Tokens (JWT) con expiración de 24 horas. Las rutas protegidas deben exigir un token válido en la cabecera de autorización. |
| **Criterio de Aceptación** | Solicitud con JWT válido → HTTP 200; JWT expirado → HTTP 401 "Token expirado"; sin cabecera de autorización → HTTP 401 "Autenticación requerida". Un token de sesión cerrada no puede reutilizarse. |

**Estado actual:** ⚠️ Parcial
- JWT funcional con `jjwt 0.12.6`
- Blacklist en Redis para tokens revocados
- Sesiones registradas en tabla `sesiones_usuario`

**Gaps pendientes:**
- [ ] **CRÍTICO:** Cambiar expiración de 1 hora a 24 horas. En `application.properties`:
  ```properties
  security.jwt.expiration-time=${JWT_EXPIRATION:86400000}
  ```
- [ ] Verificar que `JwtAuthenticationFilter` responde con mensajes exactos según el criterio:
  - JWT expirado → `"Token expirado"`
  - Sin cabecera Authorization → `"Autenticación requerida"`
- [ ] Verificar que un token post-logout (en blacklist Redis) retorna HTTP 401

---

### RF-04 — Recuperación de Contraseña

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | Enlace de recuperación de un solo uso con validez de 60 minutos, enviado al correo del usuario. |
| **Criterio de Aceptación** | Un enlace utilizado no puede reutilizarse → "Este enlace ya ha sido utilizado o ha expirado". Un enlace con más de 60 minutos produce el mismo mensaje. Tras el flujo, se puede iniciar sesión con la nueva contraseña de inmediato. |

**Estado actual:** ⚠️ Parcial
- `forgotPassword()` genera token SHA-256, lo almacena en `tokens_recuperacion` y envía correo
- `resetPassword()` valida el token y actualiza la contraseña

**Gaps pendientes:**
- [ ] **CRÍTICO:** Cambiar la validez del token de 24 horas a **60 minutos**. En `AuthServiceImpl.resetPassword()`:
  ```java
  // Línea actual:
  if (tokenRec.getFechaGeneracion().plusHours(24).isBefore(LocalDateTime.now()))
  // Debe ser:
  if (tokenRec.getFechaGeneracion().plusMinutes(60).isBefore(LocalDateTime.now()))
  ```
- [ ] Cambiar el mensaje de error a: `"Este enlace ya ha sido utilizado o ha expirado"` (unificar el mensaje para token usado y token expirado)

---

### RF-05 — Autenticación de Dos Factores (2FA)

| Campo | Detalle |
|---|---|
| **Prioridad** | 🟡 Media |
| **Descripción** | 2FA opcional basado en TOTP (RFC 6238), disponible únicamente para usuarios con identidad verificada. Al activarla, se presenta un código QR compatible con Google Authenticator y Authy. |
| **Criterio de Aceptación** | Con 2FA activo, el sistema solicita el código TOTP tras validar las credenciales. Un código incorrecto o expirado retorna "Código inválido o expirado". Un usuario sin identidad verificada no visualiza la opción de activar 2FA. |

**Estado actual:** ⚠️ Parcial
- `TwoFactorServiceImpl` implementa setup, confirm, disable y validación TOTP
- `TwoFactorController` expone los endpoints
- Códigos de respaldo implementados (8 códigos SHA-256)
- Integración con `GoogleAuthenticator` funcional

**Gaps pendientes:**
- [ ] **Condicionar 2FA a identidad verificada.** En `TwoFactorServiceImpl.setup2Fa()`, agregar validación:
  ```java
  // Verificar que el usuario tiene perfil verificado
  PerfilCreador perfil = perfilCreadorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, 
          "La opción de 2FA solo está disponible para creadores verificados"));
  
  // Verificar que tiene al menos un certificado aprobado
  boolean verificado = certificadoIaRepository.existsByPerfilIdPerfilAndEstadoVerificacionNombreEstado(
      perfil.getIdPerfil(), "APROBADO");
  if (!verificado) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
          "Debes verificar tu identidad antes de activar la autenticación de dos factores");
  }
  ```
- [ ] Cambiar mensaje de código incorrecto a: `"Código inválido o expirado"` (actualmente dice "Código 2FA o de respaldo incorrecto")

---

## Requisitos No Funcionales Relacionados

### RNF-02 — Contraseñas con bcrypt (costo ≥ 10)

**Estado:** ✅ Implementado — Spring Security BCrypt (`PasswordEncoder`)
- [ ] Verificar que el `BCryptPasswordEncoder` usa factor de coste ≥ 10. En `AuthConfig.java`:
  ```java
  @Bean
  public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder(12); // Verificar que el coste está explícito
  }
  ```

### RNF-03 — JWT firmado con HS256, clave ≥ 256 bits en variable de entorno

**Estado:** ✅ Implementado
- `JwtService` usa `Keys.hmacShaKeyFor()` con clave de la variable `JWT_SECRET`
- [ ] Verificar que la clave en `.env` tiene al menos 32 bytes (64 caracteres hexadecimales)
- [ ] **CRÍTICO:** Eliminar la contraseña SMTP hardcodeada en `application.properties` (línea 54: `eumk uevx yfle ptjy`) y usar solo la variable de entorno

### RNF-12 — Bloquear menores de 18

**Estado:** ✅ Implementado
- `AuthServiceImpl.register()` valida `fechaNacimiento` para confirmar ≥ 18 años
- `RegisterRequest` tiene `@NotNull` en `fechaNacimiento`
- [ ] Agregar validación de checkbox `aceptaTerminos` (campo obligatorio en registro)

---

## Tablas de Base de Datos

| Tabla | Entidad JPA | Repositorio | Estado |
|---|---|---|---|
| `roles` | `Rol.java` | `RolRepository` | ✅ |
| `permisos` | `Permiso.java` | `PermisoRepository` | ✅ |
| `rol_permisos` | `RolPermiso.java` | `RolPermisoRepository` | ✅ |
| `pais` | `Pais.java` | `PaisRepository` | ✅ |
| `usuarios` | `Usuario.java` | `UsuarioRepository` | ✅ |
| `usuario_roles` | `UsuarioRol.java` | `UsuarioRolRepository` | ✅ |
| `sesiones_usuario` | `SesionUsuario.java` | `SesionUsuarioRepository` | ✅ |
| `tokens_recuperacion` | `TokenRecuperacion.java` | `TokenRecuperacionRepository` | ✅ |
| `autenticacion_dos_factores` | `AutenticacionDosFactores.java` | `AutenticacionDosFactoresRepository` | ✅ |

---

## Archivos Existentes

### Controllers
- `AuthController.java` — Registro, login, 2FA verify, refresh, logout, forgot/reset password
- `TwoFactorController.java` — Setup 2FA, confirm, disable
- `AdminUserController.java` — CRUD de usuarios (admin)
- `UserController.java` — Perfil del usuario autenticado
- `RolePermissionController.java` — Gestión de roles y permisos
- `PermissionController.java` — CRUD de permisos
- `PaisController.java` — Catálogo de países

### Services
- `AuthService` / `AuthServiceImpl` — Lógica de autenticación
- `TwoFactorService` / `TwoFactorServiceImpl` — Lógica 2FA
- `UserService` / `UserServiceImpl` — Gestión de usuario
- `AdminUserService` / `AdminUserServiceImpl` — Admin
- `RolePermissionService` / `RolePermissionServiceImpl` — RBAC
- `PaisService` / `PaisServiceImpl` — Países
- `EmailService` — Envío de correos SMTP
- `SessionRevocationService` — Blacklist de tokens en Redis

### Security
- `SecurityConfig.java` — Filtros HTTP, CORS, headers de seguridad
- `JwtService.java` — Generación y validación de JWT
- `JwtAuthenticationFilter.java` — Filtro de autenticación en cada request
- `CustomUserDetailsService.java` — Carga de usuarios desde BD
- `CustomUserDetails.java` — Wrapper de UserDetails con ID

---

## Endpoints Existentes

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/registro` | No | Registro con rol |
| `POST` | `/api/auth/login` | No | Login → JWT |
| `POST` | `/api/auth/2fa/verify` | No | Verificar código 2FA |
| `POST` | `/api/auth/refresh` | RefreshToken | Renovar accessToken |
| `POST` | `/api/auth/logout` | JWT | Cerrar sesión |
| `POST` | `/api/auth/forgot-password` | No | Solicitar recuperación |
| `POST` | `/api/auth/reset-password` | No | Restablecer contraseña |
| `GET` | `/api/usuarios/me` | JWT | Perfil autenticado |
| `PUT` | `/api/usuarios/me` | JWT | Actualizar perfil |
| `GET/POST/PUT/DELETE` | `/api/admin/usuarios/**` | JWT + Admin | CRUD usuarios |
| `GET/POST/PUT/DELETE` | `/api/admin/roles/**` | JWT + Admin | CRUD roles/permisos |

---

## Checklist de Validación del Módulo

### Funcionalidad Core
- [x] Registro con selección de rol (CLIENTE/CREADOR)
- [x] Login con JWT (accessToken + refreshToken)
- [x] Logout con invalidación en Redis
- [x] Refresh token (rotación)
- [x] Recuperación de contraseña por email
- [x] 2FA con TOTP + códigos de respaldo
- [x] RBAC con roles y permisos dinámicos
- [x] Validación de mayoría de edad (≥ 18)

### Ajustes Pendientes para Validación
- [ ] JWT expira en 24h (no 1h)
- [ ] Token de recuperación expira en 60 min (no 24h)
- [ ] Mensajes de error exactos según criterios de aceptación
- [ ] Campo `aceptaTerminos` obligatorio en registro
- [ ] 2FA condicionado a identidad verificada (requiere Módulo 2)
- [ ] BCrypt con coste explícito ≥ 10
- [ ] Credenciales SMTP movidas a variables de entorno
- [ ] JWT Secret solo en variables de entorno (no en código)

### Pruebas Requeridas
- [ ] Test: Registro con datos válidos → 201 Created
- [ ] Test: Registro con email duplicado → 409 Conflict
- [ ] Test: Registro con menor de 18 años → 400 Bad Request
- [ ] Test: Login exitoso → 200 + tokens
- [ ] Test: Login con credenciales incorrectas → 401
- [ ] Test: Acceso con JWT válido → 200
- [ ] Test: Acceso con JWT expirado → 401 "Token expirado"
- [ ] Test: Acceso sin JWT → 401 "Autenticación requerida"
- [ ] Test: Token post-logout → 401
- [ ] Test: Refresh token exitoso → 200 + nuevo accessToken
- [ ] Test: Enlace de recuperación usado → error
- [ ] Test: Enlace de recuperación expirado (>60min) → error
- [ ] Test: Creador sin verificación intenta activar 2FA → 403

---

## Dependencias con Otros Módulos

| Módulo | Relación |
|---|---|
| **M2 (Perfiles)** | RF-05 requiere que el usuario tenga identidad verificada para activar 2FA → depende de `CertificadoIa` + `EstadoVerificacion` |
| **Todos** | Todos los módulos dependen de M1 para autenticación y autorización |

---

## Prerrequisitos del Sistema

- [x] PostgreSQL corriendo (Docker)
- [x] Redis corriendo (Docker)
- [x] Flyway migrations ejecutadas
- [x] Variables de entorno configuradas (.env)
- [x] Spring Boot compilando correctamente
