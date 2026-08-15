# ADR-002: Esquema de autenticación y control de acceso

**Estado:** Aceptado
**Fecha:** Tercera Entrega, 24 de julio de 2026 (revisado — ver Historial de revisiones)

## Contexto
Artisync necesita autenticación *stateless* que escale horizontalmente, soporte cuatro roles (Administrador, Creador, Cliente, Visitante) con permisos granulares (RBAC), y una capa opcional de 2FA para usuarios verificados (REQ-F-001 a REQ-F-005, REQ-NF-002, REQ-NF-003).

## Opciones consideradas
- **A — Sesiones de servidor (server-side sessions):** simples pero no escalan sin *sticky sessions* o almacén compartido; añaden estado al servidor.
- **B — JWT en `localStorage`/`sessionStorage`:** *stateless* pero vulnerable a robo de token vía XSS.
- **C — JWT firmado en cookie `HttpOnly + Secure + SameSite=Strict`:** *stateless*, protegido contra lectura por JavaScript (mitiga XSS) y contra envío cross-site (mitiga CSRF).

## Decisión
Se adopta la **Opción C, parcialmente**: el **access token** JWT (HS256, clave ≥256 bits) se devuelve en el cuerpo de la respuesta y se guarda en memoria en el cliente (signal de Angular), **no** en cookie — así puede enviarse como `Authorization: Bearer` a cualquier ruta protegida. Solo el **refresh token** y, desde la revisión de seguridad de agosto de 2026, el **ticket pre-auth de 2FA**, viajan en cookies `HttpOnly + SameSite=Strict` (`Secure` configurable vía `app.security.cookie-secure`, activo en producción). Blacklist de JTI revocados en Redis para soportar logout efectivo. Contraseñas con bcrypt (factor de coste 12). RBAC implementado con Spring Security sobre las tablas `roles`/`permisos`/`rol_permisos`.

*(Nota: una versión anterior de este ADR afirmaba que el JWT completo viajaba en cookie `HttpOnly+Secure+SameSite=Strict`; no era así — el access token siempre viajó en el body/header. Corregido en la revisión de agosto de 2026.)*

## Consecuencias positivas
- Bajo superficie de ataque XSS/CSRF respecto a almacenamiento en `localStorage`.
- Revocación de sesión posible pese al carácter *stateless* del JWT, gracias a la blacklist en Redis.
- RBAC declarativo con anotaciones de Spring Security, coherente con REQ-F-002.

## Historial de revisiones

**Julio 2026 (emisión original):** el JWT no incluía todos los siete claims estándar (`iss`, `sub`, `aud`, `exp`, `nbf`, `iat`, `jti`) ni la cabecera HSTS. Resuelto como OBS-AUTO-01 (claims) y como parte de OBS-08 (HSTS y el resto de cabeceras).

**Agosto 2026 (revisión de seguridad, OBS-AUTO-04 a OBS-AUTO-09):** seis hallazgos adicionales corregidos —

- **OBS-AUTO-04 — bypass de 2FA.** `/api/auth/2fa/verify` aceptaba `{correo, codigo}` sin ninguna prueba de que el llamante hubiera pasado por `/login`: habilitar 2FA *degradaba* la seguridad de la cuenta a "6 dígitos, sin límite de intentos". Se introduce `PreAuth2faTicketService`: un token **opaco** (no JWT — el uso único y el tope de intentos ya exigen estado en Redis, así que un JWT no aportaría nada y sí una superficie nueva de confused-deputy) emitido por `login()` tras validar la contraseña, transportado en la cookie `HttpOnly` `preAuth2fa` (TTL 5 min, tope 5 intentos, uso único).
- **OBS-AUTO-05 — rate limiting.** `LoginRateLimitFilter` solo cubría `/login`, usaba `request.getRemoteAddr()` sin resolver la IP real detrás del proxy (bucket compartido por todos los clientes, y auditoría de login inutilizable), y no existía cuota por cuenta. Ahora: `server.forward-headers-strategy=native` (Tomcat `RemoteIpValve`, no `framework`, que no tiene lista de proxies de confianza) + el backend deja de publicar el puerto 8080 al host (el límite de confianza real); `AuthRateLimitFilter` generaliza el límite por IP a las cinco rutas sensibles; `IntentosAutenticacionService` añade una cuota por cuenta (solo cuenta fallos, se limpia al acertar).
- **OBS-AUTO-06 — cuenta deshabilitada ignorada.** `JwtAuthenticationFilter` nunca comprobaba `userDetails.isEnabled()`; una cuenta suspendida seguía autenticando hasta que expirara el token si la revocación en Redis fallaba. Corregido en el nuevo `JwtService.esAccessTokenValido(...)`.
- **OBS-AUTO-07 — JWT completo en la base de datos.** `sesiones_usuario.token_jwt` guardaba el token íntegro en claro; un volcado de la tabla entregaba tomas de control de todas las sesiones activas. Ahora se guarda únicamente el `jti` (migración `V8__sesiones_usuario_jti.sql`).
- **OBS-AUTO-08 — `iss`/`aud` sin validar.** Se emitían pero `JwtService` nunca los verificaba al parsear. Ahora el `JwtParser` se construye una vez con `.requireIssuer(...)` y `.requireAudience(...)`, y el token lleva un claim `type` (`access`/`refresh`) que `JwtAuthenticationFilter` usa como lista blanca (antes era lista negra: solo rechazaba `type=refresh`, aceptando en silencio cualquier tipo futuro desconocido).
- **OBS-AUTO-09 — clave sin validar y sin caché.** La longitud mínima de la clave HS256 solo se comprobaba de forma perezosa en el primer login (`Keys.hmacShaKeyFor` lanzando en caliente), y se reconstruía en cada petición. `JwtService` pasa a inyección por constructor: valida ≥256 bits al arrancar (falla el `ApplicationContext` con un mensaje claro) y cachea la `SecretKey` y el `JwtParser` en campos `final`.

Consecuencia compartida de OBS-AUTO-04 y OBS-AUTO-08: los tokens emitidos antes de este despliegue no llevan `type` ni `aud` tipada, y las sesiones se purgaron en la migración V8 — un único cierre de sesión forzado para toda la base de usuarios.

## Referencias
RFC 7519 (JWT); RFC 6238 (TOTP); OWASP Top 10:2021; OWASP SQL Injection Prevention Cheat Sheet.
