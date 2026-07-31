# Versionado Semántico (Semantic Versioning 2.0.0)

Este proyecto adopta de manera estricta las reglas de [Semantic Versioning 2.0.0 (SemVer)](https://semver.org/lang/es/).

Dada un número de versión `MAJOR.MINOR.PATCH`, se debe incrementar:
1. La versión **MAJOR** (mayor) cuando se realizan cambios incompatibles en la API.
2. La versión **MINOR** (menor) cuando se añade funcionalidad de manera compatible con versiones anteriores.
3. La versión **PATCH** (parche) cuando se corrigen errores de manera compatible con versiones anteriores.

## Reglas adicionales para el PFC

- La **Tercera Entrega** se etiqueta obligatoriamente como un Release Candidate: `v0.9.0-rc`.
- La **Entrega Final** se considerará la primera versión estable de producción y se etiquetará como `v1.0.0`.
- El historial de cambios se mantiene en `CHANGELOG.md` siguiendo el formato "Keep a Changelog".
- Todos los commits deben seguir las convenciones de **Conventional Commits** (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`, `perf:`).
