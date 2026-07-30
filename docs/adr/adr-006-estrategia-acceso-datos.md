# ADR-006: Estrategia híbrida de acceso a datos (ORM + procedimientos almacenados)

**Estado:** Aceptado (implementación pendiente — ver Consecuencias negativas)
**Fecha:** Tercera Entrega, 24 de julio de 2026

## Contexto
La guía de la Tercera Entrega exige, sin excepción, que toda operación de base de datos que no sea un CRUD elemental (joins, agregaciones, reportes, actualizaciones masivas, validaciones cruzadas entre tablas, generación de folios, proyecciones DTO) se encapsule en procedimientos almacenados o funciones SQL, invocados desde Spring Data mediante `@Procedure` o `@NamedStoredProcedureQuery`. A la fecha de este ADR, el backend de Artisync implementa el 100% del acceso a datos vía JPA/Hibernate (48 entidades, sin uso de `@Procedure` en el código revisado).

## Opciones consideradas
- **A — Mantener 100% ORM:** más simple y homogéneo, pero no cumple el requisito obligatorio A.2.2 de la guía y no ofrece las mismas garantías de atomicidad para operaciones multi-tabla complejas (ej. liberación de fondos en el patrón *escrow*, reportes de comisiones del Creador).
- **B — Migrar 100% a procedimientos almacenados:** máximo control sobre SQL, pero renuncia a las ventajas de mapeo automático de JPA para los CRUD simples, aumentando el esfuerzo de mantenimiento sin necesidad.
- **C — Estrategia híbrida:** CRUD elementales (alta, lectura por PK, listado/paginado con filtros triviales, actualización de atributos propios, baja lógica) permanecen en JPA/Spring Data; toda operación con joins, agregaciones, reportes, actualizaciones masivas o validaciones cruzadas se mueve a procedimientos/funciones PL/pgSQL versionados en `db/procs/`.

## Decisión
Se adopta la **Opción C**. Se identifican como candidatos obligatorios a procedimiento almacenado, entre otros:
- Reporte de comisiones y transacciones por Creador (agregación + filtro de fechas) — soporta REQ-NF-013.
- Liberación de fondos del patrón *escrow* (validación cruzada entre `pedidos`, `pagos_garantia` y `transacciones_pago` antes de aceptar la escritura) — soporta REQ-F-021.
- Cálculo de calificación promedio del Creador a partir de reseñas (agregación) — soporta REQ-F-009.
- Selección aleatoria de ganadores de sorteo entre participantes que cumplen el requisito de seguidor (join + regla de negocio) — soporta REQ-F-023.
- Listado del catálogo con filtros combinados de categoría, subcategoría, rango de precio y etiquetas cuando el filtro cruza más de una tabla — soporta REQ-F-013.

Cada procedimiento se documentará en `docs/basedatos/CATALOGO-SP.md` con nombre, propósito, parámetros y tablas afectadas, y usará exclusivamente parámetros nombrados (sin SQL dinámico por concatenación).

## Consecuencias positivas
- Cumplimiento del requisito A.2.2, condición necesaria para no calificar automáticamente Insuficiente en los criterios C1 y C6.
- Mayor garantía de atomicidad e integridad en las operaciones financieras críticas del patrón *escrow*.
- Reducción de lógica de agregación en la capa de servicio Java, delegándola al motor de datos.

## Consecuencias negativas — pendiente de implementación
- **A la fecha de este ADR, ningún procedimiento almacenado de negocio existe en el repositorio** (solo un trigger de auditoría `set_actualizado_en`). Esta es la brecha de mayor riesgo para los criterios C1 y C6 de la rúbrica y debe resolverse antes del 24 de julio.
- Introduce un segundo lenguaje (PL/pgSQL) que el equipo debe mantener con la misma disciplina de versionado que el código Java.

## Referencias
Guía de la Tercera Entrega, secciones A.2.1–A.2.3; Jakarta Persistence 2.1 (JSR 338); OWASP SQL Injection Prevention Cheat Sheet.
