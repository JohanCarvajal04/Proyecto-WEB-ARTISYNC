# ADR-004: Estrategia de caché

**Estado:** Aceptado
**Fecha:** Tercera Entrega, 24 de julio de 2026

## Contexto
El catálogo de servicios es el endpoint de mayor frecuencia de lectura (REQ-NF-004: LCP ≤2s) y candidato natural a caché. Adicionalmente, la blacklist de JTI revocados (ADR-002) requiere un almacén rápido de expiración automática.

## Opciones consideradas
- **A — Caché en memoria de la propia JVM (Caffeine/EhCache):** rápida, pero no compartida entre réplicas del backend si se escala horizontalmente.
- **B — Redis:** almacén clave-valor externo, compartido entre instancias, con soporte nativo de TTL, adecuado tanto para caché de catálogo como para blacklist de tokens.

## Decisión
Se adopta **Redis 7** para dos usos concretos: (1) caché del endpoint de listado de servicios del catálogo, con TTL declarado en configuración externa (no en código), y (2) blacklist de JTI de tokens JWT revocados por logout.

## Consecuencias positivas
- TTL externo permite ajustar la política de expiración sin recompilar el backend.
- Al ser compartido, el caché y la blacklist funcionan correctamente si el backend se escala a múltiples instancias.
- La hit ratio del caché de catálogo se puede medir empíricamente y reportarse (Bloque C.1 de esta entrega).

## Consecuencias negativas
- Introduce un componente adicional a operar (contenedor Redis) y un punto de fallo más — mitigado con `healthcheck` en `docker-compose.yml`.
- Requiere invalidación explícita del caché al editar/eliminar un servicio, para evitar servir datos obsoletos.

## Referencias
Guía de la Tercera Entrega, sección A.1 (consolidación funcional); `docker-compose.yml` del proyecto (servicio `redis`).
