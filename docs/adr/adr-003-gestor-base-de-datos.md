# ADR-003: Selección del gestor de base de datos

**Estado:** Aceptado
**Fecha:** Entrega 1A, ratificado en Tercera Entrega

## Contexto
El esquema de Artisync tiene más de 40 tablas con relaciones complejas (RBAC, catálogo dinámico, flujo de pedidos, contratos, pagos con patrón *escrow*, mensajería, funciones sociales). Se requieren transacciones ACID estrictas, tipos de dato precisos para montos financieros y soporte maduro de constraints.

## Opciones consideradas
- **A — PostgreSQL:** ACID completo, tipos avanzados (`DECIMAL`, `ENUM` vía `CHECK`), soporte robusto de funciones/procedimientos en `PL/pgSQL`, JSONB para datos semiestructurados si se necesitara.
- **B — MySQL/MariaDB:** ampliamente soportado, pero históricamente con soporte más limitado de tipos avanzados y de `CHECK` constraints en versiones anteriores a 8.0.16.
- **C — MongoDB (NoSQL):** flexible pero sin transacciones ACID multi-documento tan maduras para el patrón financiero *escrow* que requiere consistencia estricta.

## Decisión
Se adopta **PostgreSQL 16** (imagen de contenedor `postgres:16`, a fijar por digest sha256 en esta entrega) como motor relacional único, con **Flyway** para versionado de esquema y **procedimientos almacenados en PL/pgSQL** para las operaciones no triviales (joins múltiples, agregaciones, reportes) según la estrategia híbrida de acceso a datos (ver ADR-006).

## Consecuencias positivas
- Constraints `DECIMAL(10,2)` garantizan precisión en montos de pagos y comisiones.
- `PL/pgSQL` permite encapsular lógica de reportes y validaciones cruzadas cerca de los datos, reduciendo *round-trips* y superficie de inyección SQL.
- Flyway asegura reproducibilidad del esquema entre entornos (Bloque B de la guía de esta entrega).

## Consecuencias negativas
- Menor portabilidad si en el futuro se requiere migrar a otro motor (mitigado: JPA abstrae parcialmente el acceso a datos para los CRUD elementales).
- Los procedimientos almacenados en PL/pgSQL no son portables a otro motor sin reescritura — se acepta como costo del beneficio de rendimiento y seguridad.

## Referencias
Corpus Entrega 1A (tabla de tecnología, sección 4.2); Jakarta Persistence 2.1 (uso de `@Procedure`).
