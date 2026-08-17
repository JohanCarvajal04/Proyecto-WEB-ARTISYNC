# ADR-005: Estrategia de despliegue y reproducibilidad

**Estado:** Aceptado — acciones pendientes de la Tercera Entrega resueltas (ver actualización de la
Entrega Final al final de este documento). El requisito A.4.2 de la guía de la Entrega Final pide
además un **ADR-007** dedicado a la estrategia de despliegue; en este repositorio el número 007 ya
está tomado por `adr-007-almacenamiento-de-archivos.md` (decisión no relacionada). Este ADR-005
sigue siendo la fuente de verdad de la estrategia de despliegue; el ambiente de producción concreto
de la Entrega Final (proveedor, dominio, HTTPS) se documentará en `docs/despliegue/DEPLOYMENT.md`
una vez el equipo decida dónde desplegar — ver brecha declarada en
`docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`.
**Fecha:** Tercera Entrega, 24 de julio de 2026

## Contexto
La Tercera Entrega exige que un tercero pueda reconstruir el sistema de forma idéntica desde una clonación limpia, en un solo comando, con paridad exacta entre entornos (Bloque B de la guía). El proyecto ya usa Docker Compose con cuatro servicios: `postgres`, `redis`, `backend`, `frontend`.

## Opciones consideradas
- **A — Despliegue manual (instalación local de JDK, Node, PostgreSQL, Redis):** replicable con documentación extensa, pero propenso a divergencias de versión entre máquinas de los evaluadores.
- **B — Docker Compose con imágenes por tag variable (`postgres:16`, `redis:7-alpine`):** reproducible en la mayoría de los casos, pero un tag puede apuntar a una imagen distinta si el mantenedor la actualiza (deriva silenciosa).
- **C — Docker Compose con imágenes ancladas por digest sha256 + Makefile de un solo comando:** máxima reproducibilidad, exigida explícitamente por el badge ACM *Artifacts Evaluated — Reusable*.

## Decisión
Se adopta la **Opción C**: Docker Compose con las cuatro imágenes ancladas por digest sha256, orquestadas mediante un `Makefile` con los objetivos `up`, `down`, `test`, `bench`, `audit`, `clean`. El esquema de base de datos se aplica exclusivamente desde `db/schema.sql` y `db/seed.sql` montados en `/docker-entrypoint-initdb.d/` (ya se cumple parcialmente: `spring.jpa.hibernate.ddl-auto=validate` en `application.properties`, con Flyway como única fuente de verdad del esquema).

## Consecuencias positivas
- Un revisor externo puede levantar el sistema completo con `make up` sin conocimiento previo del proyecto.
- El anclaje por digest elimina derivas silenciosas entre reconstrucciones, mejorando la validez de las mediciones de rendimiento (Bloque C.1).

## Consecuencias negativas — acciones pendientes (estado a la Tercera Entrega)
- **El directorio `Frontend/` referenciado en `docker-compose.yml` no existe aún en el repositorio** (confirmado en la revisión de esta entrega); `make up` fallará hasta que se agregue al menos un frontend mínimo funcional o se ajuste el `docker-compose.yml` para no bloquear el arranque del backend.
- Las imágenes `postgres:16` y `redis:7-alpine` aún están fijadas por tag, no por digest — pendiente antes del cierre de la Tercera Entrega.
- No existe todavía un `Makefile`/`justfile` en el repositorio — pendiente de creación.

## Referencias
Guía de la Tercera Entrega, Bloque B; ACM Artifact Review and Badging v1.1.

## Actualización — Entrega Final (17 de agosto de 2026)

Las tres acciones pendientes de arriba están resueltas y verificadas:
- `artisync/Frontend/` existe con `Dockerfile` propio; el stack completo levanta con `make up`.
- `artisync/docker-compose.yml:7,39` fija `postgres:16` y `redis:7-alpine` por digest `sha256`, no por tag.
- `Makefile` (raíz) implementa `up`, `down`, `test`, `bench`, `audit`, `audit-zap`, `clean`, `sus`, `lighthouse`.

Pendiente todavía, fuera del alcance de este ADR: la imagen propia del proyecto (backend/frontend)
no se publica en GitHub Container Registry con tag `v1.0.0` (exigido por el Bloque A.1 de la guía de
la Entrega Final), y no existe objetivo `make all` de un solo comando (exigido por D.1) — ver
`docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`.
