# Changelog de Requisitos — Artisync

Formato basado en [Keep a Changelog](https://keepachangelog.com), adaptado a requisitos de software.

## [v1.3.6] - 2026-09-11 — Cierre de la regresión de `[v1.3.5]`: REQ-NF-016 sube a 82,93 % / 71,50 %; OBS-P1-01 vuelve a cumplirse

### Changed — REQ-NF-016: la regresión de Controladores declarada en `[v1.3.5]` se cerró el mismo día

Commits `a6746df` (*"test(cobertura): cubrir 5 controladores y RespaldoServicioImpl sin
pruebas propias"*) y `0cd7d50` (*"docs(jacoco): actualizar medicion de cobertura tras cerrar
OBS-P1-01"*), ambos 2026-09-11, horas después de `[v1.3.5]`. Se añadió una clase de test por
controlador (`RespaldoControlador`, `SubcategoriaControlador`, `PagoControlador`,
`PlantillaAcuerdoCreadorControlador`, `EtiquetaControlador` — los cinco que quedaron en 0% de
cobertura tras V45-V48) y una suite completa para `RespaldoServicioImpl` (0/14 ramas cubiertas
hasta entonces), sin tocar ningún código de producción. Nueva medición (1230 pruebas, 129
clases, 0 fallos):

| Capa | Lines | Branches |
|---|---|---|
| Servicios | 85,89 % (4778/5563) | 76,09 % (1273/1673) |
| Controladores | **86,18 % (393/456)** | **78,05 % (64/82)** |
| Global | **82,93 % (6023/7263)** | **71,50 % (1548/2165)** |

**`OBS-P1-01` vuelve a cumplirse**: las tres capas superan el 70% en líneas y ramas, con margen
razonable en las tres. La entrada `[v1.3.5]` queda como registro histórico de una regresión que
duró horas (de la mañana a la tarde del mismo 2026-09-11), no de un problema sin resolver —
siguiendo la misma convención de no reescribir entradas pasadas que ya usó `[v1.1.1]` para
corregir `[v1.0.0]`.

Estado de `REQ-NF-016` en `matriz.csv`/`SRS.md`: sigue `verificado` (no cambió durante la
regresión, porque el requisito exige solo líneas globales ≥70%, cumplidas en todo momento); esta
entrada documenta la evidencia empírica actualizada que respalda ese estado, y el cierre del
hallazgo de `OBS-P1-01` que la nota de `REQ-NF-016` en `SRS.md` declaraba como brecha activa.

`bash scripts/validate-traceability.sh`: 0 errores, 62/62.

## [v1.3.5] - 2026-09-11 — REQ-NF-016: nueva medición JaCoCo (81,69 % / 70,44 %); regresión declarada en cobertura de Controladores

### Changed — REQ-NF-016: re-medición tras las funcionalidades V45-V48

Nueva corrida de `./mvnw.cmd -B clean test` (1187 pruebas, 0 fallos, 0 errores; 148 pruebas
nuevas desde la medición del 2026-09-05, que incorporaron plantillas de acuerdo por creador
(V45), reconciliación PayPal, cierre de tickets de revisión e integridad de contratos
(V46-V48)). Cobertura global: **81,69 % líneas (5933/7263) / 70,44 % ramas (1525/2165)** —
el requisito solo exige líneas ≥70 % según JaCoCo, y se cumple con margen. Ver
`docs/mediciones/jacoco/REPORTE-JACOCO.md`.

> **Nota:** al escribir esta entrada, la medición del 2026-09-11 seguía archivada en el
> working tree (`docs/mediciones/jacoco/REPORTE-JACOCO.md` modificado, no comiteado sobre
> `e84ab50`). Si el archivo cambia de nuevo antes de comitearse, esta entrada debe
> releerse contra el archivo real antes de darse por definitiva — mismo estándar que exige
> el resto de este documento.

### Regresión declarada — cobertura de ramas de Controladores cae bajo el 70 % (OBS-P1-01)

El corpus de la capa `controller` casi se duplicó desde el 2026-09-05 (377→456 líneas,
50→82 ramas) por las funcionalidades V45-V48, y las pruebas nuevas de esta ronda se
concentraron en `service` sin cubrir los controladores nuevos en la misma proporción:

| Capa | Antes (2026-09-05, ronda 2) | Ahora (2026-09-11) |
|---|---|---|
| Servicios | 89,96 % líneas / 79,30 % ramas | 85,12 % líneas / **75,25 % ramas** |
| Controladores | 86,47 % líneas / 86,00 % ramas | 76,10 % líneas / **67,07 % ramas ⚠** |
| Global | 86,75 % líneas / 75,03 % ramas | 81,69 % líneas / 70,44 % ramas |

Controladores queda por debajo del 70 % de ramas que exige `OBS-P1-01` (líneas Y ramas en
las tres capas), arrastrado por `RespaldoControlador`, `SubcategoriaControlador`,
`PagoControlador`, `PlantillaAcuerdoCreadorControlador` y `EtiquetaControlador` — los cinco
en 0 % de cobertura de línea hoy. **No cambia el estado de `REQ-NF-016`** (que mide líneas
globales, ya cumplido con margen), pero es la brecha activa que sustenta `OBS-P1-01` en
`docs/observaciones/`. Pendiente: `@WebMvcTest` para esos cinco controladores antes de dar
por cerrado `OBS-P1-01` de nuevo — no se aborda en esta entrada, que solo registra el
estado real medido, sin ajustar el umbral ni excluir paquetes.

`bash scripts/validate-traceability.sh`: 0 errores, 62/62.

## [v1.3.4] - 2026-09-10 — REQ-NF-020 y REQ-NF-024 suben a `verificado`; REQ-F-022b/REQ-F-022c suben a `implementado`

### Changed — REQ-NF-020 pasa de `pendiente` a `verificado`

Commit `a218ac4` (*"feat(legal,pedido): reconciliacion PayPal, cierre de tickets de revision
e integridad de contratos (V46-V48)"*, 2026-09-10). Se introdujo un hash de **contenido**
nuevo y separado del hash de firma existente (este último sigue intacto: es una huella
evento/quién/cuándo con `Instant.now()`, nunca reproducible por diseño, y no debía
confundirse con la integridad del contenido). El nuevo hash se calcula una sola vez sobre el
HTML ya renderizado y congelado al completarse la segunda firma. `ContratoIntegridadScheduler`
reverifica todos los contratos firmados cada noche; `POST
/api/v1/admin/contratos/{id}/verificar-integridad` permite una verificación puntual bajo
demanda. Probado con `ContratoServicioImplTest` (congelar al firmar, íntegro, discrepancia),
`ContratoIntegridadSchedulerTest` y `ContratoIntegridadControladorTest`.

### Changed — REQ-F-022b y REQ-F-022c pasan de `pendiente` a `implementado`

Mismo commit `a218ac4`. **Corrección de una desincronización previa de este mismo
documento**: la entrada `[v1.3.0]` (tabla "división de 2 requisitos compuestos") seguía
citando ambos como `(pendiente)` — ese estado ya no era cierto desde este commit. Mismo caso
que `[v1.1.1]` corrigió antes para el documento en su conjunto: el contenido técnico no era
falso, pero una tabla concreta había quedado desincronizada con un cambio posterior.

- **REQ-F-022b**: `PagoTicketRevisionServicioImpl` (nuevo) genera automáticamente la orden de
  pago PayPal al crear un ticket de revisión que supera el límite configurado, con una
  entidad propia `pagos_ticket_revision` para no competir con el escrow principal. Probado
  con `PagoTicketRevisionServicioImplTest` y
  `TicketRevisionServicioImplTest#crearTicketRevision_superaLimite_disparaCreacionDeOrdenDePago`.
- **REQ-F-022c**: `TicketRevisionExpiracionScheduler`/`TicketRevisionExpiracionServicio`
  (nuevos) rechazan automáticamente un ticket de revisión sin pago confirmado tras 48 horas
  (umbral configurable vía `ticketrevision.expiracion-horas`). Probado con
  `TicketRevisionExpiracionSchedulerTest`, `TicketRevisionExpiracionServicioTest` y
  `TicketRevisionRepositoryIT` (esta última ejercita la query JPQL con `LEFT JOIN ON`
  explícito contra un motor JPA real, no solo con mocks).

Ninguno de los dos sube a `verificado`: ambos son `Should` y `implementado` ya cumple su
mínimo formal, pero falta ejercitar el flujo completo contra el sandbox real de PayPal para
subir el último tramo — excepción declarada por honestidad en
`docs/trazabilidad/excepciones-estado.txt`.

### Changed — REQ-NF-024 pasa de `implementado` a `verificado`

Commit `804ed2d` (módulo de respaldos, panel admin) seguido de `c640c21` (*"docs: verificar y
cerrar REQ-NF-024 tras restauración exitosa de prueba"*, 2026-09-10). El mecanismo de
respaldo automatizado FULL/INCREMENTAL con frecuencia y retención declaradas (programación
cron o disparo manual desde el panel de administración) ya estaba implementado; lo que
faltaba era la restauración de prueba. Se ejecutó y documentó una real: respaldo FULL id=1
generado por el panel admin, `pg_restore` exit 0 sin errores, validación post-restauración
pedidos=4/usuarios=10/contratos=3 — ver `docs/despliegue/BACKUP.md §Registro de
restauraciones de prueba`.

`bash scripts/validate-traceability.sh`: 0 errores, 62/62. §7.1-§7.3 de `SRS.md` y
`docs/trazabilidad/matriz.csv` ya reflejaban estos tres estados; esta entrada solo cierra el
historial que había quedado atrás.

## [v1.3.3] - 2026-09-10 — Evidencia real para REQ-NF-001a/b/c, REQ-NF-009, REQ-NF-005, REQ-NF-006; REQ-NF-005/006/011 suben a `verificado`; defecto real corregido en WebSocket

### Added — Exclusión explícita del reporte manual de contenido (A6)

Se declara explícitamente fuera de alcance el reporte manual de contenido por usuarios (observación A6); la moderación cubierta hoy es automática (REQ-F-015) y administrativa reactiva (REQ-F-033). No se añade requisito nuevo; la brecha ya se documentó en §2.3 en v1.3.0 ("se retiró 'contenido reportado' de la descripción del rol Administrador"). Esta entrada formaliza la exclusión en §1.2 Alcance.

### Added — Rationale completado para 8 requisitos heredados

Se completó el rationale de los 8 requisitos heredados que aún carecían de él: REQ-F-004, REQ-F-005, REQ-F-006, REQ-F-007, REQ-F-008, REQ-F-009, REQ-F-020 y REQ-F-021 — cierra la observación residual de M4. No cuenta como modificación de enunciado para §7.4 (la regla del documento es "cambio de enunciado, prioridad o alcance"; el rationale no es ninguna de las tres). No afecta tasa de estabilidad. Igual que se hizo para REQ-F-010 a REQ-F-019, REQ-F-022a/b/c y REQ-F-023 (ver CHANGELOG-REQ.md línea 164).

### Changed — REQ-F-003: enunciado ampliado para declarar el refresh token

REQ-F-003: enunciado ampliado para declarar el refresh token de 7 días (antes solo mencionado en el criterio de aceptación) — corrige observación M5 de la revisión externa del 2026-09-08. No afecta la implementación (ambos tokens existen desde v1.0.0); el campo Aceptación (línea 204 de SRS.md) ya los mencionaba correctamente. Con este cambio, los modificados en enunciado del corpus heredado pasan de 4 a 5; tasa de estabilidad: 1 − 5/37 = 0,865 (86,5 %).

### Changed — REQ-NF-005, REQ-NF-006 y REQ-NF-011 pasan de `implementado` a `verificado`

Nuevas pruebas automatizadas re-ejecutables, no mediciones manuales de una sola vez:

- **REQ-NF-005**: `ChatWebSocketLoadIT` (`@SpringBootTest` de contexto completo, 10 conexiones STOMP reales concurrentes × 5 rondas) mide la latencia extremo-a-extremo real del chat: p95=346ms, máximo=346ms, 0 conexiones fallidas — bajo el umbral de 500ms. Ver `docs/mediciones/ws/REPORTE-WS.md`.
- **REQ-NF-006**: `ContratoPdfTimingIT` (`@DataJpaTest` contra Postgres real) cronometra 5 generaciones reales de PDF de contrato: 1218/29/30/20/20 ms — muy bajo el umbral de 5000ms. Ver `docs/mediciones/perf/REPORTE-PDF-CONTRATO.md`.
- **REQ-NF-011**: Se fijó el secreto `AZURE_STORAGE_CONNECTION_STRING` en el entorno productivo de Render, cerrando el incumplimiento activo documentado en la versión anterior y verificando descargas reales de Azure Blob Storage.

### Fixed — defecto real de producción: el envío de chat por WebSocket estaba roto

Al construir `ChatWebSocketLoadIT`, la primera corrida no completó ninguna ronda: el
servidor rechazaba todo mensaje STOMP real con `MessageConversionException`.
`ChatControlador.enviarMensajeWs` (`@MessageMapping("/chat.enviar")`) declara
`@AuthenticationPrincipal CustomUserDetails userDetails`, pero el proyecto nunca
registraba un `HandlerMethodArgumentResolver` para resolverlo en mensajería — Spring
trataba ese parámetro como si fuera el `@Payload` implícito y fallaba al intentar
deserializar el cuerpo JSON del mensaje dentro de `CustomUserDetails`. Invisible a
`ChatControladorTest` porque invoca el método del controlador directamente en Java, sin
pasar por el pipeline real de despacho STOMP.

Corregido en `WebSocketConfig`:

- `addArgumentResolvers(...)` con `AuthenticationPrincipalArgumentResolver`.
- `SecurityContextChannelInterceptor` agregado en `configureClientInboundChannel`
  (después de `webSocketAuthInterceptor`), para que la `Authentication` llegue al
  `SecurityContextHolder` que el resolver anterior consulta.
- Nueva dependencia `org.springframework.security:spring-security-messaging`.

Confirmado sin regresiones: `./mvnw test` completo, 1133/1133.

### Fixed — dos gaps preexistentes de configuración de pruebas, descubiertos al validar `./mvnw test` de punta a punta

- `app.frontend.url` faltaba en `src/test/resources/application.properties` y
  `application-postgres-it.properties`: `EmailService` (vía `AuthServiceImpl`) la
  requiere incondicionalmente, y ningún `@DataJpaTest` anterior levantaba el contexto
  completo bajo estos perfiles para exponerlo — `ArtisyncApplicationTests` y
  `SecurityConfigTest` son los únicos que sí lo hacen.
- `spring.flyway.user`/`spring.flyway.password` faltaban en el perfil H2 por defecto:
  `RespaldoBdServicioImpl` (REQ-NF-024) los inyecta directamente vía `@Value`
  independientemente de si Flyway está habilitado.

### Changed — evidencia real archivada para REQ-NF-001a/b/c y REQ-NF-009 (permanecen `implementado`, no `pendiente`)

- **REQ-NF-001a/b/c**: análisis SSL Labs ejecutado contra `artisync-frontend.onrender.com`
  (grade A+, solo TLS 1.2/1.3 aceptados, redirección HTTPS forzada confirmada). No suben
  a `verificado` por un motivo estructural: el validador exige `prueba_automatizada` para
  todo Must verificado, y un análisis externo de un tercero no lo es. Ver
  `docs/mediciones/sec/ssl-labs/REPORTE-SSL-LABS.md` y script reejecutable
  `scripts/verificar-tls-ssllabs.sh`.
- **REQ-NF-009**: demostración real de caída/recuperación ejecutada (`docker kill` sobre
  `pfc_backend` y `pfc_postgres`, Docker Desktop/WSL2 local). Resultado: el reinicio
  automático **no se disparó** en ninguno de los dos casos — hallazgo activo, no evidencia
  pendiente de algo que funciona. Ver `docs/mediciones/resiliencia/REPORTE-RECUPERACION.md`
  y script reejecutable `scripts/demo-recuperacion-docker.sh`.

§7.1-§7.3 de `SRS.md` recalculados: `verificado` 45→47, `implementado` 12→10,
`pendiente` sin cambio (5). `bash scripts/validate-traceability.sh`: 0 errores, 62/62.

## [v1.3.2] - 2026-09-09 — 4 requisitos suben de `implementado`/`pendiente` a `verificado` (evidencia de prueba completada)

### Changed — REQ-F-010, REQ-F-033, REQ-NF-018, REQ-NF-022 pasan a `verificado`

Los cuatro ya estaban funcionalmente implementados; faltaba exclusivamente el nivel de evidencia que exige `verificado`. Se cerró cada brecha sin tocar código de producción salvo donde se indica:

| Requisito  | Qué faltaba                                                                                                                                         | Qué se agregó                                                                                                                                                                                                                                                                                                                               |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-F-010  | `AdminComentarioControlador` no tenía ninguna prueba (el servicio/controlador público ya estaban bien cubiertos).                                   | `AdminComentarioControladorTest` (nuevo): los 4 endpoints (`listarParaModeracion`, `ocultarComentario`, `reactivarComentario`, `eliminarComentario`), incluida la propagación de `ExcepcionRecursoNoEncontrado`.                                                                                                                            |
| REQ-F-033  | `InfraccionServiceImpl.listarInfracciones`/`revertirSuspension` sin prueba (`historialPorUsuario` sí la tenía).                                     | 3 casos nuevos en `InfraccionServiceImplTest`: listado sin filtrar por usuario, reversión exitosa (mensaje exacto con el correo), reversión sobre usuario inexistente.                                                                                                                                                                      |
| REQ-NF-018 | `PrivacidadServiceImplTest` era unitario con Mockito; no ejercitaba las consultas JPA derivadas reales ni el bloqueo pesimista.                     | `PrivacidadServiceImplIT` (nueva, `@DataJpaTest` + `postgres-it`, mismo patrón que `AprobarEntregaConcurrenciaIT`): 4 casos contra PostgreSQL real — anonimización completa, rechazo por pedido sin transición registrada, excepción legal por fondos retenidos, idempotencia contra el bloqueo pesimista real de `findByIdParaAnonimizar`. |
| REQ-NF-022 | `AuthRateLimitFilterTest` solo probaba el límite de `login`; los otros 4 (2fa, forgot-password, reset-password, registro) no tenían caso de prueba. | 8 casos nuevos: cada uno de los 5 límites reales en su valor exacto (pasa) y en límite+1 (bloquea con el `Retry-After` correcto).                                                                                                                                                                                                           |

Ningún cambio de enunciado ni de alcance — solo de estado, respaldado por prueba automatizada verde. Suite completa (`./mvnw test`): 1133/1133. `bash scripts/validate-traceability.sh`: 0 errores, 62/62. §7.1-§7.3 de `SRS.md` recalculados.

## [v1.3.1] - 2026-09-09 — Cierre de REQ-NF-018 (protección de datos personales)

### Changed — REQ-NF-018 pasa de `pendiente` a `implementado`

Los tres criterios de aceptación quedan cubiertos:

- `docs/basedatos/POLITICA-RETENCION.md` se extiende con la sección "Qué se retiene con plazo
  declarado (datos personales sensibles)", cubriendo `certificados_ia`, `datos_pago_creador` y
  `contratos` (este último sin campos personales propios — se documenta por qué queda fuera de
  la anonimización y se remite a REQ-NF-020 para la integridad del hash de firma).
- Nuevo mecanismo real de supresión de datos personales por anonimización:
  `PrivacidadService`/`PrivacidadServiceImpl`, expuesto vía
  `POST /api/v1/usuarios/me/solicitud-supresion` (autoservicio, idempotente) y
  `POST /api/v1/admin/usuarios/{id}/supresion` (administrador, rechaza la operación si el
  usuario ya la tiene ejecutada). Declara la excepción legal para `datos_pago_creador` cuando el
  usuario tiene un contrato con fondos aún retenidos en garantía (`pagos_garantia.estado_fondos
= 'Retenido'`).
- Minimización del payload hacia servicios de IA: sin cambios de código — se reconfirma que
  `GeminiIaService`/`NvidiaIaService` solo envían prompt + imagen.
- Frontend: botón "Suprimir mis datos" en `configuracion-cuenta.component` (autoservicio, con
  advertencia explícita de irreversibilidad antes de confirmar) y acción "Suprimir datos
  personales" en `users.component` del panel admin.

### Added — tres ajustes de robustez sobre el mecanismo de supresión

- **Bloqueo pesimista de fila** (`UsuarioRepository.findByIdParaAnonimizar`, mismo patrón que
  `ContratoRepository.findByIdParaFirmar`): dos solicitudes casi simultáneas para el mismo
  usuario (doble clic, autoservicio + admin a la vez) ya no pueden ambas pasar el chequeo de
  idempotencia antes de que la primera confirme su cambio.
- **Idempotencia basada en el propio correo anonimizado** en vez de una consulta a
  `auditoria_eventos`: al estar bajo el mismo bloqueo de fila que el resto de la operación,
  elimina la ventana de carrera que tenía la versión anterior (el commit de la bitácora ocurre en
  una transacción `REQUIRES_NEW` posterior, fuera del bloqueo).
- **Bloqueo por pedido en curso**: si el usuario (como cliente o como creador) tiene un pedido
  cuya etapa actual no es la etapa final de su flujo, la supresión completa se rechaza (declarada
  como excepción en autoservicio, como error en el panel admin) hasta que el pedido termine o se
  cancele — evita que la contraparte de una transacción activa vea el nombre del usuario cambiar
  a "Usuario eliminado" en medio de mensajería, entregables o reseñas.

No alcanza `verificado`: la prueba nueva (`PrivacidadServiceImplTest`, 16 casos) es unitaria con
Mockito, no una prueba de integración contra base de datos real que ejercite las consultas JPA
derivadas (`ContratoRepository.findByPedidoUsuarioClienteIdUsuario`, etc.) ni el bloqueo pesimista
real de PostgreSQL. Excepción declarada en `docs/trazabilidad/excepciones-estado.txt`.

## [v1.3.0] - 2026-09-08 — Segunda revisión externa: defecto del validador, 10 requisitos adicionales, división de 2 requisitos compuestos, secciones normativas nuevas

### Added — portada institucional (recuperada tras conflicto de fusión)

Un `git stash pop` conflictivo entre esta rama y una versión previa del mismo trabajo omitió la identificación del documento (M1 de la segunda auditoría): portada con universidad/facultad/carrera/período, tabla de los 4 integrantes con ORCID y correo institucional (uno queda honestamente pendiente de confirmar en vez de inventado), DOI del software y del dataset (Zenodo), enlace al repositorio, y "Representante del equipo" en §8. Se restauró tomando los datos de `CONTRIBUTORS.md`, `CITATION.cff` y `README.md`, sin inventar información.

### Fixed — defecto en el validador de trazabilidad

`scripts/validate-traceability.sh` extraía el estado de cada requisito en `SRS.md` con una regex que solo capturaba la primera palabra tras "Estado:" (`[a-zA-Z]+`). REQ-NF-005 y REQ-NF-006 declaraban `Estado: implementado de validación` — fuera del vocabulario cerrado `{pendiente, implementado, verificado}` que el propio documento exige — y la regex leía solo "implementado", coincidiendo por casualidad con `matriz.csv` sin que el validador lo detectara. Se corrigió la regex para capturar hasta el primer paréntesis o fin de línea, se normalizaron dos entradas preexistentes (REQ-NF-013, REQ-NF-017) que usaban punto o coma en vez de paréntesis para separar el estado de su nota, y se añadió una comprobación explícita del enum cerrado para que una regresión similar haga fallar el build en vez de colarse. Confirmado con una prueba de regresión (reintroducir `implementado de X` y verificar que el validador ahora falla).

### Added — 2 requisitos funcionales y 8 no funcionales

| Requisito  | Módulo                                          | Prioridad | Motivo de la adición                                                                                                                                                                                      |
| ---------- | ----------------------------------------------- | --------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-F-032  | Gestión de obras de portafolio                  | Must      | `PortafolioItemControlador` (límite de 50 ítems, descarga forzada anti-XSS) no tenía requisito propio.                                                                                                    |
| REQ-F-033  | Administración de infracciones y suspensiones   | Should    | REQ-F-015 cubre la detección automática, pero `AdminInfraccionControlador` (listar infracciones, revertir suspensión) no tenía requisito ni prueba propios.                                               |
| REQ-NF-018 | Protección de datos personales sensibles        | Must      | Falta extender `POLITICA-RETENCION.md` (hoy solo cubre datos técnicos de sesión) a documentos de identidad, certificados, pagos, contratos y mensajería, y falta un mecanismo real de supresión de datos. |
| REQ-NF-019 | Conciliación y recuperación de pagos PayPal     | Must      | Con patrón escrow, no había requisito sobre webhooks duplicados, reconciliación activa, ni cancelación con fondos retenidos (esto último no existe en código).                                            |
| REQ-NF-020 | Conservación e integridad de contratos firmados | Should    | El hash de firma se calcula una vez y nunca se re-verifica; no hay retención declarada.                                                                                                                   |
| REQ-NF-021 | Política de contraseñas                         | Should    | Ya implementada (longitud + composición, revocación de sesiones al cambiarla) pero no especificada ni declarada `verificado`.                                                                             |
| REQ-NF-022 | Límite de tasa en autenticación y registro      | Must      | Ya implementado con valores concretos (`AuthRateLimitFilter`) pero sin requisito propio que los declare.                                                                                                  |
| REQ-NF-023 | Accesibilidad (Lighthouse)                      | Should    | Ya medido (100/100) pero sin umbral declarado en ningún requisito.                                                                                                                                        |
| REQ-NF-024 | Respaldo y recuperación de base de datos        | Should    | No existe automatización; solo dumps SQL manuales ad hoc.                                                                                                                                                 |
| REQ-NF-025 | Auto-revocación de sesiones propias             | Should    | Ya existe (`DELETE /api/v1/usuarios/me/sesiones`) pero sin requisito propio, distinto de la revocación administrativa de REQ-F-030.                                                                       |

Se agregaron las historias HU-32/HU-33 y los casos de uso CU-32/CU-33 (`docs/requisitos/historias/HU-32-a-HU-33-adicionales.md`, `docs/requisitos/casos-de-uso/CU-32-a-CU-33-adicionales.md`).

### Changed — división de 2 requisitos compuestos en sub-requisitos atómicos

| Requisito original | Dividido en                                                                      | Motivo                                                                                                                                                                                   |
| ------------------ | -------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-F-022          | REQ-F-022a (verificado), REQ-F-022b (pendiente), REQ-F-022c (pendiente)          | Agrupaba tres capacidades con estados de verificación distintos bajo un único identificador; el estado compuesto ocultaba que 2 de las 3 no están construidas.                           |
| REQ-NF-001         | REQ-NF-001a, REQ-NF-001b, REQ-NF-001c (los tres `implementado`, misma excepción) | Agrupaba tres aserciones técnicas independientes (redirección HTTPS, rechazo TLS<1.2, preferencia TLS 1.3) bajo un único enunciado, contra el criterio de atomicidad de INCOSE (C1-C15). |

`scripts/validate-traceability.sh` se actualizó para aceptar un sufijo de letra opcional en los identificadores (`REQ-F-NNNa`) en sus tres puntos de validación de formato/extracción.

### Changed — correcciones de precisión sobre hallazgos ya documentados

- §2.3: se retiró "contenido reportado" de la descripción del rol Administrador — no existe ningún mecanismo de reporte/denuncia de contenido por usuarios, ni backend ni frontend; el alcance estaba prometido en prosa sin construir.
- §7.1-§7.3: recalculados para 62 requisitos totales (43 CRUD-ORM · 8 SP · 11 sin acceso a datos); se distingue explícitamente, dentro de los Must no verificados, entre "evidencia pendiente de algo que ya funciona" (REQ-NF-001a/b/c, REQ-NF-009) e "incumplimiento activo en producción" (REQ-NF-011: mientras `DOCUMENTOS_PROVEEDOR` no se fije en `render.yaml`, el sistema desplegado guarda archivos localmente, que es justo lo que el requisito prohíbe).
- §7.4: la tasa de estabilidad del corpus heredado se recalcula sobre 4 modificaciones (antes 2), incorporando la división de REQ-F-022 y REQ-NF-001 como cambio de enunciado; nueva tasa 89,2% (antes 94,6%).
- Excepción de REQ-NF-017 en `excepciones-estado.txt`: la ronda anterior citaba un "flujo de UX problemático" sin fuente verificable. Se corrigió con la cita real: los ítems peor puntuados son Q10, Q6 y Q8 (carga de aprendizaje inicial), y el plan de cierre real ya existe en `docs/etica/INFORME-SITUACION-ESTUDIO-USABILIDAD.md` §3 (ronda adicional de SUS después de un cambio real de UX, no una remedición del mismo build).
- Se completó la plantilla (rationale + criterio de aceptación) de los 12 requisitos heredados que carecían de ella: REQ-F-010 a REQ-F-019, REQ-F-022a/b/c, REQ-F-023.
- **REQ-F-014**: se corrigió el enunciado ("se cierra al llegar a Entregado o Cancelado" → "se cierra cuando el Cliente aprueba el entregable"). Verificado contra `ChatServiceImpl.cerrarSala`: su único invocador es la aprobación del entregable; no existe ninguna función de cancelar un pedido, así que la cláusula "o Cancelado" no tenía sustento en código.

### Added — secciones normativas nuevas

- **§1.6 Estados del dominio y transiciones**: Pedido (sin catálogo fijo de etapas — 100% configurable por flujo, contra lo asumido inicialmente), Solicitud de retiro (5 estados reales), Pago en garantía (3 estados, confirmado sin cancelación/reembolso), Entregable (booleano, no enum), Certificado/Verificación (4 estados reales en mayúsculas — "verificado" no existe como valor). Construida por lectura directa de enums/constantes reales, no por nombres plausibles.
- **§1.7 Correspondencia con el Anexo C de ISO/IEC/IEEE 29148:2018**: tabla de mapeo sección-a-sección, con 2 desviaciones conscientes declaradas (requisitos lógicos de BD remitidos a `db/schema.sql`; detalle de usabilidad remitido a `docs/mediciones/`).
- **§2.3**: se agregaron Moderador, Auditor Financiero y Soporte a la tabla de roles — existían como roles reales en `artisync/db/seed.sql` con permisos propios, pero no figuraban en la descripción de usuarios del SRS. `SOPORTE` es un sexto rol descubierto en esta ronda, no mencionado en ninguna revisión anterior.
- **§2.6 Matriz de permisos por rol**: construida por extracción de `artisync/db/seed.sql` + migraciones `V10/V19/V20/V32/V33/V36/V39` y las anotaciones `@PreAuthorize` reales del backend. Documenta la distinción entre "permiso concedido en la base de datos" y "acceso real al endpoint" (18 de 20 permisos revisados tienen un comodín `hasRole('ADMIN')` a nivel de controlador aunque la fila de permiso se le haya retirado a ADMIN en la base de datos).
- **§2.7 Interfaces externas**: PayPal Orders v2, servicio de IA de verificación, almacenamiento S3/Azure y canal WebSocket (STOMP + SockJS), con protocolo, autenticación y comportamiento ante indisponibilidad de cada una.
- **§3.0-§4**: criterio de suficiencia de verificación añadido a REQ-NF-007, REQ-NF-008 y REQ-NF-010 (qué observar para dar el requisito por cumplido).

## [v1.2.0] - 2026-09-07 — 11 requisitos adicionales: funcionalidad implementada que no tenía especificación

### Added

Auditoría del SRS contra el código real del backend (44 controladores, ~217 combinaciones método+ruta) encontró módulos completos, varios con implicación directa de dinero, implementados y probados sin ningún requisito que los especificara. Se agregan 8 REQ-F y 3 REQ-NF; ninguno representa alcance nuevo del sistema, sino especificación que le faltaba a alcance ya construido. Detalle completo en `docs/requisitos/SRS.md` §3.1 y §4.1.

| Requisito  | Módulo                                        | Prioridad | Motivo de la adición                                                                                                                                                                                                                           |
| ---------- | --------------------------------------------- | --------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-F-024  | Retiro de fondos por el Creador (payouts)     | Must      | Cierre del ciclo de pago: el escrow de REQ-F-021 retiene fondos, pero no había requisito para el flujo de retiro (`SolicitudRetiroControlador`, `SolicitudRetiroAdminControlador`, `DatosPagoControlador`).                                    |
| REQ-F-025  | Supervisión de pagos en garantía (escrow)     | Must      | Sin requisito, nadie podía auditar cuánto dinero de clientes estaba retenido ni en qué estado (`PagoGarantiaAuditoriaControlador`).                                                                                                            |
| REQ-F-026  | Reportes administrativos exportables          | Should    | Exponen datos financieros y de contratos de terceros sin que ningún requisito declarara quién puede generarlos (`ReporteFinancieroControlador`, `ReporteContratoControlador`).                                                                 |
| REQ-F-027  | Centro de notificaciones internas             | Should    | Mecanismo transversal ya usado por varios módulos, sin requisito propio (`NotificacionControlador`).                                                                                                                                           |
| REQ-F-028  | Gestión y moderación de catálogos auxiliares  | Must      | REQ-F-011/012/013 dan por hecho categorías/subcategorías/etiquetas ya existentes, sin especificar su CRUD ni su flujo de revisión (`CategoriaControlador`, `SubcategoriaControlador`, `EtiquetaControlador`, `ServicioModeracionControlador`). |
| REQ-F-029  | Reseñas y calificaciones de servicios         | Must      | REQ-F-009 solo cubre la exhibición del promedio, no el flujo de creación/edición/eliminación que lo alimenta (`ResenaControlador`).                                                                                                            |
| REQ-F-030  | Administración de cuentas de usuario          | Should    | Ciclo de vida completo de la cuenta (activar/suspender/roles/sesiones), distinto del RBAC operativo de REQ-F-002 (`AdminUserController`).                                                                                                      |
| REQ-F-031  | Catálogo de países y "me gusta" en portafolio | Could     | Dos capacidades menores sin impacto financiero ni legal (`PaisController`, `LikePortafolioControlador`).                                                                                                                                       |
| REQ-NF-015 | Comportamiento ante indisponibilidad de Redis | Must      | Política fail-open ya implementada (`IntentosAutenticacionService`) y documentada solo en código/ADR-004, sin requisito.                                                                                                                       |
| REQ-NF-016 | Cobertura de código (JaCoCo)                  | Should    | El equipo ya perseguía un umbral de 70-75% (6 mediciones versionadas) sin que ningún requisito lo formalizara.                                                                                                                                 |
| REQ-NF-017 | Usabilidad medible (SUS)                      | Should    | El SUS medido (61,25/100) está por debajo del umbral propio del proyecto (68) y ningún requisito lo capturaba como brecha conocida.                                                                                                            |

Se agregaron las historias de usuario HU-24 a HU-31 (`docs/requisitos/historias/HU-24-a-HU-31-adicionales.md`) y los casos de uso CU-24 a CU-31 (`docs/requisitos/casos-de-uso/CU-24-a-CU-31-adicionales.md`), y se actualizaron §7.1-§7.3 de `SRS.md` con los nuevos totales (48 requisitos en total).

## [v1.1.2] - 2026-09-07 — Corrección de estados inflados y excepción de despliegue desactualizada

### Changed — estado corregido

| Requisito  | Antes      | Ahora        | Motivo                                                                                                                                                                                                                                                                                                                                                                                             |
| ---------- | ---------- | ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-F-022  | verificado | implementado | Solo el cargo configurable por revisión adicional está implementado y probado (`TicketRevisionServicioImplTest`). El nuevo enlace de pago al superar el límite y el rechazo automático tras 48h sin pago no tienen servicio de pago ni scheduler en el código: ninguno de los 4 `@Scheduled` existentes cubre tickets de revisión. Mismo criterio de honestidad ya aplicado a REQ-F-010 en v1.0.1. |
| REQ-NF-011 | verificado | implementado | `AlmacenamientoAzure` existe y está probado, pero `documentos.proveedor` tiene `local` como valor por defecto en `application.properties` y `render.yaml` no fija `DOCUMENTOS_PROVEEDOR=azure` en el despliegue. Sin verificar esa variable en producción, el criterio "sin archivos locales en el servidor" no está acreditado.                                                                   |

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

| Requisito | Antes                                                                                                                                                                                                                                      | Ahora                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Motivo                                                                                                                                                                                                                                                                                        |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-F-016 | Formulario de briefing genérico del Creador (`briefing_plantillas.id_perfil`), enviado manualmente por el Creador desde el chat _después_ de creado el pedido; el Cliente podía no llegar a responderlo nunca.                             | El cuestionario se asigna a un servicio concreto (`Servicio.briefingPlantilla`, nullable). Si el servicio tiene uno asignado, el Cliente lo responde dentro del mismo formulario de creación del pedido; el pedido no se crea si falta alguna respuesta. Un servicio sin cuestionario no pide nada extra (sin cambio de comportamiento respecto a hoy). Se retiraron los endpoints `POST /api/v1/pedidos/{idPedido}/briefing` y `POST /api/v1/pedidos/{idPedido}/briefing/responder`; `GET /api/v1/pedidos/{idPedido}/briefing` se mantiene, ahora de solo lectura. | El envío manual dependía de que el Creador se acordara de dispararlo, y el Cliente podía aceptar un pedido sin dar ninguna información del proyecto. Ligarlo al servicio y exigirlo en la creación garantiza que el Creador reciba el contexto que pidió, siempre.                            |
| REQ-F-017 | Una única plantilla de contrato global (`plantillas_contrato`), sembrada por migración (`V13`), sin ningún endpoint de administración; `ContratoServicioImpl` tomaba siempre "la de mayor id". Todo servicio firmaba el mismo texto legal. | Catálogo de plantillas de contrato curado por ADMIN (`plantillas_contrato.nombre_plantilla/es_predeterminada/activa`, nuevo permiso `CONTRATO_PLANTILLA_GESTIONAR`, `PlantillaContratoAdminControlador`). El Creador elige, al crear/editar su servicio, cuál plantilla del catálogo aplica (`Servicio.plantillaContrato`, nullable); sin elegir ninguna, el contrato usa la marcada como predeterminada. El creador no escribe texto legal libre.                                                                                                                  | Un contrato de diseño gráfico y uno de desarrollo de software no deberían firmar exactamente las mismas cláusulas. Un catálogo curado por ADMIN permite personalizar el texto legal por tipo de servicio sin exponer a la plataforma a cláusulas no revisadas escritas por cualquier creador. |

Ninguno de los dos requisitos cambió de prioridad (siguen Must) ni de estado (siguen `verificado`): las pruebas automatizadas se ampliaron junto con el código (`ContratoServicioImplTest`, nuevo `PlantillaContratoAdminServicioImplTest`, `PedidoServicioImplTest`, `BriefingServiceImplTest`), así que el estado sigue siendo cierto.

### Added

- Migraciones `V39__catalogo_plantillas_contrato.sql` y `V40__cuestionario_por_servicio.sql`.
- `PlantillaContratoAdminControlador` (CRUD del catálogo, ADMIN) y `PlantillaContratoControlador` (lectura de plantillas activas, para el selector del creador).
- Caso de uso `CU-17b: Administrar el catálogo de plantillas de contrato`.

### Removed

- Flujo manual de briefing: `enviarBriefing`/`responderBriefing` en `BriefingService`/`BriefingServiceImpl`/`BriefingControlador`, y el DTO `PeticionEnviarBriefing`. El botón "Enviar briefing" del panel del creador (`comision-detalle.component`) se retiró junto con él.

## [v1.0.1] - 2026-08-29 — REQ-F-010 implementado

### Changed

| Requisito | Antes     | Ahora        | Motivo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| --------- | --------- | ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-F-010 | pendiente | implementado | Se construyó la rodaja vertical completa: `ComentarioPortafolioService`/`Impl`, `ComentarioPortafolioControlador` (crear/listar/contar/eliminar) y `AdminComentarioControlador` (listar/ocultar/reactivar/purgar). El borrado por el autor o el dueño del portafolio es lógico (`estado_moderacion = 'Eliminado'`), no visible en la vista pública pero consultable por el administrador vía `GET /api/v1/admin/comentarios`, tal como exige el enunciado. Verificado manualmente end-to-end en navegador; falta la prueba automatizada (unitaria/IT) para subirlo a `verificado`. |

### Removed

- La excepción de REQ-F-010 en `docs/trazabilidad/excepciones-estado.txt`: un Should en estado `implementado` ya cumple su mínimo sin necesidad de excepción declarada.

## [v1.0.0] - 2026-08-21 — Reconciliación SRS ↔ matriz

Ningún requisito cambió de enunciado, prioridad ni alcance: esta entrada registra
únicamente cambios de **estado** y la corrección de estados que estaban mal
declarados. No afecta a la tasa de estabilidad.

### Changed — estado sincronizado con `docs/trazabilidad/matriz.csv` (fuente de verdad)

| Requisito                                                                                             | Antes (SRS)  | Ahora        | Motivo                                                                                                                                                                            |
| ----------------------------------------------------------------------------------------------------- | ------------ | ------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-F-001, 002, 003, 004, 005, 008, 011, 012, 013, 017, 018, 019, 020, 021, 022, REQ-NF-002, 003, 014 | implementado | verificado   | Ya contaban con prueba automatizada en la matriz; el SRS iba por detrás.                                                                                                          |
| REQ-F-006, REQ-F-007                                                                                  | pendiente    | verificado   | Cubiertos por `VerificacionServicioImplTest`, `VerificacionControladorTest` y `CertificadoIaRepositoryIT`.                                                                        |
| REQ-F-009                                                                                             | verificado   | verificado   | El SRS lo daba por verificado sin implementación. Se implementó la rodaja vertical (servicio + controlador + 9 pruebas unitarias + 3 de integración) y ahora el estado es cierto. |
| REQ-F-010                                                                                             | verificado   | pendiente    | **Corrección de estado inflado**: no existe servicio ni controlador. Excepción declarada en `excepciones-estado.txt`.                                                             |
| REQ-NF-001                                                                                            | pendiente    | implementado | La configuración está acreditada; falta el análisis externo, que depende del despliegue público.                                                                                  |
| REQ-NF-005, REQ-NF-006                                                                                | pendiente    | implementado | La funcionalidad está construida y probada; lo que falta es la medición que verifica el umbral.                                                                                   |
| REQ-NF-009                                                                                            | parcial      | implementado | `parcial` no pertenece al enum de A.3.3. Los cinco servicios declaran `restart: unless-stopped`.                                                                                  |

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
