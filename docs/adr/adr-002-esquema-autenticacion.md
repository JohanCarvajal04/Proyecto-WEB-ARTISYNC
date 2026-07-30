# ADR-002: Esquema de autenticación y control de acceso

**Estado:** Aceptado (con acción pendiente — ver Consecuencias negativas)
**Fecha:** Tercera Entrega, 24 de julio de 2026

## Contexto
Artisync necesita autenticación *stateless* que escale horizontalmente, soporte cuatro roles (Administrador, Creador, Cliente, Visitante) con permisos granulares (RBAC), y una capa opcional de 2FA para usuarios verificados (REQ-F-001 a REQ-F-005, REQ-NF-002, REQ-NF-003).

## Opciones consideradas
- **A — Sesiones de servidor (server-side sessions):** simples pero no escalan sin *sticky sessions* o almacén compartido; añaden estado al servidor.
- **B — JWT en `localStorage`/`sessionStorage`:** *stateless* pero vulnerable a robo de token vía XSS.
- **C — JWT firmado en cookie `HttpOnly + Secure + SameSite=Strict`:** *stateless*, protegido contra lectura por JavaScript (mitiga XSS) y contra envío cross-site (mitiga CSRF).

## Decisión
Se adopta la **Opción C**: JWT firmado con HS256 (clave ≥256 bits en variable de entorno), transportado en cookie `HttpOnly+Secure+SameSite=Strict`, con blacklist de JTI revocados en Redis para soportar logout efectivo. Contraseñas con bcrypt (factor de coste ≥10). RBAC implementado con Spring Security sobre las tablas `roles`/`permisos`/`rol_permisos`.

## Consecuencias positivas
- Bajo superficie de ataque XSS/CSRF respecto a almacenamiento en `localStorage`.
- Revocación de sesión posible pese al carácter *stateless* del JWT, gracias a la blacklist en Redis.
- RBAC declarativo con anotaciones de Spring Security, coherente con REQ-F-002.

## Consecuencias negativas — acción pendiente
- El JWT actual (`JwtService.java`) no incluye todos los siete claims estándar exigidos por la guía de la Tercera Entrega (`iss`, `sub`, `aud`, `exp`, `nbf`, `iat`, `jti`); faltan explícitamente `iss`, `aud` y `nbf`. **Se registra como OBS-AUTO-01 a resolver antes del cierre de esta entrega** (ver `docs/observaciones/OBSERVACIONES.md`).
- `SecurityConfig.java` ya configura X-Content-Type-Options, X-Frame-Options (DENY), Content-Security-Policy, Referrer-Policy y Permissions-Policy (resuelto como parte de OBS-08). **Sigue faltando únicamente la cabecera HSTS** (`Strict-Transport-Security`), necesaria para el control A05 de la auditoría OWASP del bloque C.2; se añade como acción pendiente de esta entrega.

## Referencias
RFC 7519 (JWT); RFC 6238 (TOTP); OWASP Top 10:2021; OWASP SQL Injection Prevention Cheat Sheet.
