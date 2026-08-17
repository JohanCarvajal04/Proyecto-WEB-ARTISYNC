# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
