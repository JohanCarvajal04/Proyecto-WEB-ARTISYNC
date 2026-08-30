# Changelog de Requisitos — Artisync

Formato basado en [Keep a Changelog](https://keepachangelog.com), adaptado a requisitos de software.

## [v1.0.1] - 2026-08-29 — REQ-F-010 implementado

### Changed

| Requisito | Antes | Ahora | Motivo |
| --- | --- | --- | --- |
| REQ-F-010 | pendiente | implementado | Se construyó la rodaja vertical completa: `ComentarioPortafolioService`/`Impl`, `ComentarioPortafolioControlador` (crear/listar/contar/eliminar) y `AdminComentarioControlador` (listar/ocultar/reactivar/purgar). El borrado por el autor o el dueño del portafolio es lógico (`estado_moderacion = 'Eliminado'`), no visible en la vista pública pero consultable por el administrador vía `GET /api/v1/admin/comentarios`, tal como exige el enunciado. Verificado manualmente end-to-end en navegador; falta la prueba automatizada (unitaria/IT) para subirlo a `verificado`. |

### Removed

- La excepción de REQ-F-010 en `docs/trazabilidad/excepciones-estado.txt`: un Should en estado `implementado` ya cumple su mínimo sin necesidad de excepción declarada.

## [v1.0.0] - 2026-08-21 — Reconciliación SRS ↔ matriz

Ningún requisito cambió de enunciado, prioridad ni alcance: esta entrada registra
únicamente cambios de **estado** y la corrección de estados que estaban mal
declarados. No afecta a la tasa de estabilidad.

### Changed — estado sincronizado con `docs/trazabilidad/matriz.csv` (fuente de verdad)

| Requisito                                                                                              | Antes (SRS)  | Ahora        | Motivo                                                                             |
| ------------------------------------------------------------------------------------------------------ | ------------ | ------------ | ---------------------------------------------------------------------------------- |
| REQ-F-001, 002, 003, 004, 005, 008, 011, 012, 013, 017, 018, 019, 020, 021, 022, REQ-NF-002, 003, 014 | implementado | verificado   | Ya contaban con prueba automatizada en la matriz; el SRS iba por detrás.            |
| REQ-F-006, REQ-F-007                                                                                   | pendiente    | verificado   | Cubiertos por `VerificacionServicioImplTest`, `VerificacionControladorTest` y `CertificadoIaRepositoryIT`. |
| REQ-F-009                                                                                              | verificado   | verificado   | El SRS lo daba por verificado sin implementación. Se implementó la rodaja vertical (servicio + controlador + 9 pruebas unitarias + 3 de integración) y ahora el estado es cierto. |
| REQ-F-010                                                                                              | verificado   | pendiente    | **Corrección de estado inflado**: no existe servicio ni controlador. Excepción declarada en `excepciones-estado.txt`. |
| REQ-NF-001                                                                                             | pendiente    | implementado | La configuración está acreditada; falta el análisis externo, que depende del despliegue público. |
| REQ-NF-005, REQ-NF-006                                                                                 | pendiente    | implementado | La funcionalidad está construida y probada; lo que falta es la medición que verifica el umbral. |
| REQ-NF-009                                                                                             | parcial      | implementado | `parcial` no pertenece al enum de A.3.3. Los cinco servicios declaran `restart: unless-stopped`. |

### Added

- `docs/trazabilidad/excepciones-estado.txt`: registro explícito de los requisitos que no alcanzan el estado exigido por su prioridad (REQ-NF-001, REQ-NF-009, REQ-F-010), con motivo y condición de cierre.
- Secciones §7 (métricas de calidad del corpus) y §8 (aprobación del docente-director) en `SRS.md`.
- Cinco reglas nuevas en `scripts/validate-traceability.sh` que impiden que estas incoherencias se repitan: Must ⇒ verificado, verificado ⇒ con prueba, Should ≠ pendiente, estado dentro del enum, y estado idéntico entre SRS y matriz.

## [v1.0.0] - 2026-08-20

### Added
- Se documenta la implementación completa del módulo de auditoría bajo el requisito REQ-NF-013, soportado por rutinas PL/pgSQL, esquema V12 y un aspecto AOP (`@Auditable`).
- Se verifican 13 requisitos funcionales adicionales correspondientes a los módulos de catálogo, pedido, comunicación, legal y social.

### Changed
- El estado de 13 requisitos pasó de "pendiente" a "verificado", reflejando el progreso de la implementación de la Entrega Final.
- REQ-NF-004, 007, 008, 011 resueltos con el frontend Angular 22 finalizado y la integración de Azure Blob Storage.

## [v0.9.0-rc] - 2026-07-24

### Added
- Se formaliza el SRS conforme a ISO/IEC/IEEE 29148:2018 en `docs/requisitos/SRS.md`, consolidando los 23 requisitos funcionales (REQ-F-001 a REQ-F-023) y 14 no funcionales (REQ-NF-001 a REQ-NF-014) originados en la Entrega 1A.
- Se añade a cada requisito: rationale, prioridad MoSCoW, criterio de aceptación medible, método de verificación y estado de implementación.
- Se crea `docs/trazabilidad/matriz.csv` con trazabilidad end-to-end requisito → historia → caso de uso → módulo → endpoint → prueba → evidencia.

### Changed
- Identificadores renombrados de `RF-NN`/`RNF-NN` (Entrega 1A) a `REQ-F-0NN`/`REQ-NF-0NN` (ISO 29148), sin alterar el contenido semántico de los requisitos.

### Deprecated
- Ninguno en esta entrega.

### Removed
- Ninguno en esta entrega.

## [v0.3.0] - 2026-06 (Entrega 1A, referencia histórica)

### Added
- Primera especificación de requisitos (RF-01 a RF-23, RNF-01 a RNF-14) en `Entrega 1A.docx`, incluyendo roles de usuario, alcance, arquitectura C4 nivel 1-2 y modelo de datos.

---

> **Nota para el equipo:** cada vez que se modifique, agregue o elimine un requisito Must después de esta entrega, se debe añadir una fila aquí (fecha, autor, requisito afectado, tipo de cambio: added/modified/deprecated/removed, motivo) **y** crear un ADR si el cambio es arquitectónicamente significativo.
