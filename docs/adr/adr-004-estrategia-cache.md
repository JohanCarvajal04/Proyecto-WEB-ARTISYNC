# ADR-004: Estrategia de caché

**Estado:** Aceptado
**Fecha:** Tercera Entrega, 24 de julio de 2026

## Contexto
El catálogo de servicios es el endpoint de mayor frecuencia de lectura (REQ-NF-004: LCP ≤2s) y candidato natural a caché. Adicionalmente, la blacklist de JTI revocados (ADR-002) requiere un almacén rápido de expiración automática.

## Opciones consideradas
- **A — Caché en memoria de la propia JVM (Caffeine/EhCache):** rápida, pero no compartida entre réplicas del backend si se escala horizontalmente.
- **B — Redis:** almacén clave-valor externo, compartido entre instancias, con soporte nativo de TTL, adecuado tanto para caché de catálogo como para blacklist de tokens.

## Decisión
Se adopta **Redis 7** para varios usos concretos: (1) caché del endpoint de listado de servicios del catálogo, con TTL declarado en configuración externa (no en código); (2) blacklist de JTI de tokens JWT revocados por logout; (3) cuotas de intentos para el rate limiting de las rutas de autenticación (OBS-AUTO-05); y (4) tickets pre-auth de 2FA de un solo uso (OBS-AUTO-04, ver ADR-002).

Cada familia de claves tiene una postura distinta ante una caída de Redis, documentada en el javadoc de la clase que la escribe:

| Prefijo de clave | Escrita/leída por | Postura ante fallo de Redis | Motivo |
|---|---|---|---|
| `catalogo::*` | `ServicioCatalogoServicioImpl` (`@Cacheable`) | *(gestionado por Spring Cache; sin fail-open/closed explícito — un caché vacío solo implica más carga en BD)* | Rendimiento, no seguridad |
| `jti:*` | `JwtAuthenticationFilter`, `SessionRevocationService` | **Fail-closed** (503) | La revocación es una garantía de seguridad: sin poder consultarla, no se puede confiar en ningún access token |
| `rl:*` | `AuthRateLimitFilter`, `IntentosAutenticacionService` | **Fail-open** | Una mitigación de abuso no debe bloquear el login completo del sistema |
| `2fa:ticket:*` | `PreAuth2faTicketService` | **Fail-closed** (503) | Sin ticket no hay prueba de que la contraseña se validó |

## Consecuencias positivas
- TTL externo permite ajustar la política de expiración sin recompilar el backend.
- Al ser compartido, el caché y la blacklist funcionan correctamente si el backend se escala a múltiples instancias.
- La hit ratio del caché de catálogo se puede medir empíricamente y reportarse (Bloque C.1 de esta entrega).

## Consecuencias negativas
- Introduce un componente adicional a operar (contenedor Redis) y un punto de fallo más — mitigado con `healthcheck` en `docker-compose.yml`.
- Requiere invalidación explícita del caché al editar/eliminar un servicio, para evitar servir datos obsoletos.

## Referencias
Guía de la Tercera Entrega, sección A.1 (consolidación funcional); `docker-compose.yml` del proyecto (servicio `redis`).
