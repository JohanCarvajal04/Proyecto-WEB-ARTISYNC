# Changelog de Requisitos — Artisync

Formato basado en [Keep a Changelog](https://keepachangelog.com), adaptado a requisitos de software.

## [v1.2.0] - 2026-09-07 — 11 requisitos adicionales: funcionalidad implementada que no tenía especificación

### Added

Auditoría del SRS contra el código real del backend (44 controladores, ~217 combinaciones método+ruta) encontró módulos completos, varios con implicación directa de dinero, implementados y probados sin ningún requisito que los especificara. Se agregan 8 REQ-F y 3 REQ-NF; ninguno representa alcance nuevo del sistema, sino especificación que le faltaba a alcance ya construido. Detalle completo en `docs/requisitos/SRS.md` §3.1 y §4.1.

| Requisito | Módulo | Prioridad | Motivo de la adición |
| --- | --- | --- | --- |
| REQ-F-024 | Retiro de fondos por el Creador (payouts) | Must | Cierre del ciclo de pago: el escrow de REQ-F-021 retiene fondos, pero no había requisito para el flujo de retiro (`SolicitudRetiroControlador`, `SolicitudRetiroAdminControlador`, `DatosPagoControlador`). |
| REQ-F-025 | Supervisión de pagos en garantía (escrow) | Must | Sin requisito, nadie podía auditar cuánto dinero de clientes estaba retenido ni en qué estado (`PagoGarantiaAuditoriaControlador`). |
| REQ-F-026 | Reportes administrativos exportables | Should | Exponen datos financieros y de contratos de terceros sin que ningún requisito declarara quién puede generarlos (`ReporteFinancieroControlador`, `ReporteContratoControlador`). |
| REQ-F-027 | Centro de notificaciones internas | Should | Mecanismo transversal ya usado por varios módulos, sin requisito propio (`NotificacionControlador`). |
| REQ-F-028 | Gestión y moderación de catálogos auxiliares | Must | REQ-F-011/012/013 dan por hecho categorías/subcategorías/etiquetas ya existentes, sin especificar su CRUD ni su flujo de revisión (`CategoriaControlador`, `SubcategoriaControlador`, `EtiquetaControlador`, `ServicioModeracionControlador`). |
| REQ-F-029 | Reseñas y calificaciones de servicios | Must | REQ-F-009 solo cubre la exhibición del promedio, no el flujo de creación/edición/eliminación que lo alimenta (`ResenaControlador`). |
| REQ-F-030 | Administración de cuentas de usuario | Should | Ciclo de vida completo de la cuenta (activar/suspender/roles/sesiones), distinto del RBAC operativo de REQ-F-002 (`AdminUserController`). |
| REQ-F-031 | Catálogo de países y "me gusta" en portafolio | Could | Dos capacidades menores sin impacto financiero ni legal (`PaisController`, `LikePortafolioControlador`). |
| REQ-NF-015 | Comportamiento ante indisponibilidad de Redis | Must | Política fail-open ya implementada (`IntentosAutenticacionService`) y documentada solo en código/ADR-004, sin requisito. |
| REQ-NF-016 | Cobertura de código (JaCoCo) | Should | El equipo ya perseguía un umbral de 70-75% (6 mediciones versionadas) sin que ningún requisito lo formalizara. |
| REQ-NF-017 | Usabilidad medible (SUS) | Should | El SUS medido (61,25/100) está por debajo del umbral propio del proyecto (68) y ningún requisito lo capturaba como brecha conocida. |

Se agregaron las historias de usuario HU-24 a HU-31 (`docs/requisitos/historias/HU-24-a-HU-31-adicionales.md`) y los casos de uso CU-24 a CU-31 (`docs/requisitos/casos-de-uso/CU-24-a-CU-31-adicionales.md`), y se actualizaron §7.1-§7.3 de `SRS.md` con los nuevos totales (48 requisitos en total).

## [v1.1.2] - 2026-09-07 — Corrección de estados inflados y excepción de despliegue desactualizada

### Changed — estado corregido

| Requisito | Antes | Ahora | Motivo |
| --- | --- | --- | --- |
| REQ-F-022 | verificado | implementado | Solo el cargo configurable por revisión adicional está implementado y probado (`TicketRevisionServicioImplTest`). El nuevo enlace de pago al superar el límite y el rechazo automático tras 48h sin pago no tienen servicio de pago ni scheduler en el código: ninguno de los 4 `@Scheduled` existentes cubre tickets de revisión. Mismo criterio de honestidad ya aplicado a REQ-F-010 en v1.0.1. |
| REQ-NF-011 | verificado | implementado | `AlmacenamientoAzure` existe y está probado, pero `documentos.proveedor` tiene `local` como valor por defecto en `application.properties` y `render.yaml` no fija `DOCUMENTOS_PROVEEDOR=azure` en el despliegue. Sin verificar esa variable en producción, el criterio "sin archivos locales en el servidor" no está acreditado. |

### Fixed

- `docs/trazabilidad/excepciones-estado.txt`: la excepción de REQ-NF-001 y REQ-NF-009 decía "el sistema aún no está desplegado", lo cual ya no es cierto (`render.yaml` y `docs/mediciones/lighthouse/REPORTE-LIGHTHOUSE.md` documentan el despliegue real en Render desde principios de septiembre). Se corrigió el motivo de ambas excepciones para reflejar que el sistema está desplegado y lo que falta es específicamente el análisis SSL Labs archivado (REQ-NF-001) y la demostración de caída/recuperación archivada (REQ-NF-009).
- Se agregaron a `excepciones-estado.txt` las entradas de REQ-F-022 y REQ-NF-011 corregidas arriba.

## [v1.1.1] - 2026-09-07 — Sincronización de versión del documento SRS.md

### Fixed

- El header de `SRS.md` (versión, fecha, "Actualiza a") declaraba `v1.0.0 — 2026-08-18`, pero el cuerpo del documento (REQ-F-016, REQ-F-017) ya reflejaba el enunciado reescrito en `[v1.1.0] - 2026-09-06` (ver entrada siguiente). El documento nunca fue falso en su contenido técnico, pero su propio número de versión estaba desincronizado consigo mismo.
- §7.4 (Estabilidad de requisitos) afirmaba "Modificados en enunciado o alcance: 0" y una tasa de estabilidad de 100%, lo cual contradecía la entrada `[v1.1.0]` de este mismo archivo, que documenta la reescritura de enunciado de REQ-F-016 y REQ-F-017. Se recalculó la tasa a `1 − 2/37 = 0,946 (94,6%)`.
- §6 (Evolución de requisitos) no mencionaba el cambio de v1.0.0 a v1.1.0; se agregó la fila correspondiente.

### Motivo

Auditoría del SRS contra el propio historial de cambios del proyecto: un documento no puede declararse una versión mientras describe el comportamiento de la siguiente sin decirlo.

## [v1.1.0] - 2026-09-06 — Contratos personalizados por servicio y cuestionario obligatorio al crear el pedido

### Changed

| Requisito | Antes | Ahora | Motivo |
| --- | --- | --- | --- |
| REQ-F-016 | Formulario de briefing genérico del Creador (`briefing_plantillas.id_perfil`), enviado manualmente por el Creador desde el chat *después* de creado el pedido; el Cliente podía no llegar a responderlo nunca. | El cuestionario se asigna a un servicio concreto (`Servicio.briefingPlantilla`, nullable). Si el servicio tiene uno asignado, el Cliente lo responde dentro del mismo formulario de creación del pedido; el pedido no se crea si falta alguna respuesta. Un servicio sin cuestionario no pide nada extra (sin cambio de comportamiento respecto a hoy). Se retiraron los endpoints `POST /api/v1/pedidos/{idPedido}/briefing` y `POST /api/v1/pedidos/{idPedido}/briefing/responder`; `GET /api/v1/pedidos/{idPedido}/briefing` se mantiene, ahora de solo lectura. | El envío manual dependía de que el Creador se acordara de dispararlo, y el Cliente podía aceptar un pedido sin dar ninguna información del proyecto. Ligarlo al servicio y exigirlo en la creación garantiza que el Creador reciba el contexto que pidió, siempre. |
| REQ-F-017 | Una única plantilla de contrato global (`plantillas_contrato`), sembrada por migración (`V13`), sin ningún endpoint de administración; `ContratoServicioImpl` tomaba siempre "la de mayor id". Todo servicio firmaba el mismo texto legal. | Catálogo de plantillas de contrato curado por ADMIN (`plantillas_contrato.nombre_plantilla/es_predeterminada/activa`, nuevo permiso `CONTRATO_PLANTILLA_GESTIONAR`, `PlantillaContratoAdminControlador`). El Creador elige, al crear/editar su servicio, cuál plantilla del catálogo aplica (`Servicio.plantillaContrato`, nullable); sin elegir ninguna, el contrato usa la marcada como predeterminada. El creador no escribe texto legal libre. | Un contrato de diseño gráfico y uno de desarrollo de software no deberían firmar exactamente las mismas cláusulas. Un catálogo curado por ADMIN permite personalizar el texto legal por tipo de servicio sin exponer a la plataforma a cláusulas no revisadas escritas por cualquier creador. |

Ninguno de los dos requisitos cambió de prioridad (siguen Must) ni de estado (siguen `verificado`): las pruebas automatizadas se ampliaron junto con el código (`ContratoServicioImplTest`, nuevo `PlantillaContratoAdminServicioImplTest`, `PedidoServicioImplTest`, `BriefingServiceImplTest`), así que el estado sigue siendo cierto.

### Added

- Migraciones `V39__catalogo_plantillas_contrato.sql` y `V40__cuestionario_por_servicio.sql`.
- `PlantillaContratoAdminControlador` (CRUD del catálogo, ADMIN) y `PlantillaContratoControlador` (lectura de plantillas activas, para el selector del creador).
- Caso de uso `CU-17b: Administrar el catálogo de plantillas de contrato`.

### Removed

- Flujo manual de briefing: `enviarBriefing`/`responderBriefing` en `BriefingService`/`BriefingServiceImpl`/`BriefingControlador`, y el DTO `PeticionEnviarBriefing`. El botón "Enviar briefing" del panel del creador (`comision-detalle.component`) se retiró junto con él.

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
- Primera especificación de requisitos (RF-01 a RF-23, RNF-01 a RNF-14) en `docs/requisitos/historico/entrega-1a.pdf`, incluyendo roles de usuario, alcance, arquitectura C4 nivel 1-2 y modelo de datos.

---

> **Nota para el equipo:** cada vez que se modifique, agregue o elimine un requisito Must después de esta entrega, se debe añadir una fila aquí (fecha, autor, requisito afectado, tipo de cambio: added/modified/deprecated/removed, motivo) **y** crear un ADR si el cambio es arquitectónicamente significativo.
