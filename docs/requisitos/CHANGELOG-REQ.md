# Changelog de Requisitos — Artisync

Formato basado en [Keep a Changelog](https://keepachangelog.com), adaptado a requisitos de software.

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
