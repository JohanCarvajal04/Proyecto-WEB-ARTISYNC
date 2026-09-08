# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [v1.1.0] - 2026-09-01
Versión posterior al cierre académico de la Entrega Final (`v1.0.0`, commit `d07656b`,
archivado en Zenodo). Consolida el trabajo real de las dos semanas siguientes; no
sustituye ni altera el artefacto evaluado como Entrega Final.

### Security
- Refactor de autorización backend: reemplazo de `hasAnyRole` por permisos explícitos
  (`hasAuthority`) en ~20 endpoints (`RolePermissionController` y otros).
- Refactor del sistema de permisos del frontend: navegación derivada dinámicamente de
  `NAV_CATALOG` en vez de listas de permisos por panel codificadas a mano, cerrando
  rutas de escalamiento de privilegios entre paneles.
- Ajustes en 2FA y `SecurityConfig`; restricción de endpoints públicos de catálogo a
  rutas con ID explícito; exigencia de verificación de identidad para publicar
  servicios o crear pedidos.

### Added
- Flujos de trabajo por creador, permiso `FLUJO_MODERAR`, validación de duplicados y
  reordenamiento atómico.
- Directorio público de creadores, seguimiento de creadores, reseñas y calificación
  promedio, comentarios y likes en obras de portafolio con moderación.
- Auditoría de pagos en garantía (escrow); exportación de reportes en pedidos.
- Reintentos de fallos transitorios en los proveedores de IA (Gemini/NVIDIA) y mejora
  de logs de diagnóstico; visualización legible de datos extraídos por IA en el panel
  de moderación.

### Changed
- Optimización N+1 al cargar etiquetas en el listado paginado de servicios.
- Actualización de procedimientos almacenados (`R__procedimientos.sql`) y trazabilidad
  de requisitos.

### Fixed
- Corrección de precios de pedidos modificados sin consentimiento; corrección de
  infracciones y validación del flujo de trabajo.
- Guards del panel creador y exportación en "Mis Pedidos" del cliente; bug de rutas
  multi-segmento en el sidebar.
- No retroceder un pedido cuando su etapa actual fue eliminada del flujo.

## [v1.0.0] - 2026-08-17
### Added
- Análisis estático SpotBugs + find-sec-bugs sobre concatenación SQL, y escaneo OWASP ZAP baseline
  archivado (`docs/mediciones/sec/`).
- 7 procedimientos almacenados nuevos conectados al código real (ADR-006): `fn_registrar_usuario`,
  `fn_resolver_estado_login`, `fn_sincronizar_permisos_rol`, `fn_eliminar_rol`,
  `fn_registrar_infraccion`, `fn_restablecer_contrasena`, `fn_seleccionar_ganadores_sorteo`.
- Perfil Lighthouse desktop, con 3 corridas por perfil (mobile y desktop).
- `docs/despliegue/` (`DEPLOYMENT.md`, `RUNBOOK.md`, `BACKUP.md`).
- Checklists FAIR, INCOSE, PRISMA 2020 y Ralph 2021.
- Servicio `azurite` (perfil `azure`) en `docker-compose.yml` para emular Azure Blob Storage.
- `AlmacenamientoRouter`: decide por prefijo si un archivo va a Azure Blob Storage o al volumen local, con
  suite de pruebas propia (`AlmacenamientoRouterTest`, `AlmacenamientoCableadoTest`).
- Despliegue en Render: `render.yaml` (Blueprint con `artisync-backend` como Private Service en red interna,
  `artisync-frontend` como Web Service público y `artisync-redis`), `Dockerfile.render`, `nginx.render.conf`
  y `docker-entrypoint-render.sh`.

### Changed
- Cobertura (JaCoCo) y mediciones SUS actualizadas.
- Evidencia OWASP reorganizada en `docs/mediciones/sec/owasp/` + `DATA-PROVENANCE.md`.
- Checklists y matriz de trazabilidad sincronizados con el estado real del código.
- Colección Postman ampliada de 10 a 26 peticiones (casos 400/401/403/404).
- `ChatControlador` y creación de sala de chat (`ContratoServicioImpl`, `EntregableServicioImpl`) con
  ajustes finales y cobertura de pruebas nueva (`ContratoServicioImplSalaChatTest`).

### Fixed
- Marcadores de merge sin resolver en `artisync/.env.example`.
- Subida de documentos de verificación: ahora pasa el prefijo `VERIFICACION` al guardar, para que el
  router de almacenamiento los envíe al volumen local en vez de a Azure.

## [v0.9.0-rc] - 2026-07-30

> **Nota sobre `v0.9.0-rc` == `v0.7.1` (OBS-P0-03, verificado 2026-09-07).** Los tags `v0.7.1` y `v0.9.0-rc`
> apuntan al mismo commit (`d292f7b`, "docs: consolidación de artefactos") porque ambos se crearon el mismo
> día (30-07-2026) sobre el mismo cierre de trabajo: `v0.7.1` cerraba la aplicación de observaciones de las
> Entregas 1A/1B y, en el mismo commit, se inauguró la rama de trabajo de la Tercera Entrega etiquetándola de
> inmediato como `v0.9.0-rc` según exige `docs/VERSIONING.md`. El resto de los ítems `### Added`/`### Changed`
> listados bajo este encabezado (Makefile, `scripts/validate-traceability.sh`, `SRS.pdf`, evidencias de
> mediciones) se añadió en los días siguientes (31-07 a 06-08-2026) **sin re-etiquetar**, por lo que el tag
> `v0.9.0-rc` no refleja el commit final del release candidate, solo su punto de partida.
>
> Se documenta así, en vez de reasignar el tag, siguiendo el mismo criterio ya aplicado al caso análogo
> `v1.0.0` / `d07656b` (ver `docs/observaciones/OBSERVACIONES.md`, sección "Etiquetado"): mover un tag ya
> empujado a `origin` es una operación destructiva sobre una referencia pública, y el DOI de Zenodo
> (`10.5281/zenodo.21730559`, ver `CITATION.cff`) fue emitido a partir del estado actual de `v0.9.0-rc` — sin
> confirmar si el snapshot de Zenodo es independiente del puntero del tag, reasignarlo arriesga introducir una
> segunda discrepancia (tag vs. DOI) en vez de resolver la primera (tag vs. tag). Queda como limitación
> conocida y trazada, no como error sin diagnosticar.

### Added
- Evidencias de mediciones empíricas cuantitativas (rendimiento, seguridad, cobertura).
- Archivos de gestión y publicabilidad (`LICENSE`, `CITATION.cff`, `CONTRIBUTORS.md`, `CHANGELOG.md`).
- Documentación de control de versiones y reglas de versionado semántico (`docs/VERSIONING.md`).
- `Makefile` con los objetivos `up`, `down`, `test`, `bench`, `audit`, `clean` (Bloque B.1).
- `scripts/validate-traceability.sh`, ejecutado en CI para validar la matriz de trazabilidad (A.3.3).
- `docs/requisitos/SRS.pdf`.

### Changed
- Estructura y formato del README para soportar insignias de Zenodo y CI/CD.
- Imágenes de `postgres` y `redis` en `docker-compose.yml` ancladas por digest `sha256` (Bloque B.1).
- `.github/workflows/ci.yml` movido a la raíz real del repositorio (antes en `artisync/.github/workflows/`, donde GitHub Actions nunca lo descubría) y disparo agregado en la rama `main`.

## [v0.7.1] - 2026-07-30
### Fixed
- Cierre parcial de la aplicación de observaciones de las Entregas 1A y 1B (3 de 15, 20% — ver
  `docs/observaciones/OBSERVACIONES.md` para el detalle de resueltas y pendientes). El equipo
  no llegó a un cierre del 100% antes de iniciar el trabajo de la Tercera Entrega; el tag marca
  el estado real alcanzado, no un cierre completo (revisión original del docente: 29-06-2026).

## [v0.7.0] - 2026-06-20
### Added
- Módulo de autenticación (JWT) y CRUDs iniciales.
- Documentación de arquitectura y casos de uso.
- Snapshot correspondiente a la Entrega 1B (revisado por el docente el 29-06-2026).
