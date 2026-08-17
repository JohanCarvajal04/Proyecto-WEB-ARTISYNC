# ADR-006: Estrategia híbrida de acceso a datos (ORM + procedimientos almacenados)

**Estado:** Aceptado — ampliado el 16 de agosto de 2026 (ver [Ampliación](#ampliación))
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

## Ampliación

**Fecha:** 16 de agosto de 2026.

### Hallazgo que motiva esta ampliación

Al auditar el estado real de las seis rutinas listadas como implementadas en la sección Decisión, se
encontró que **ninguna estaba conectada al código Java en ejecución**: no aparecía `@Procedure`,
`createNativeQuery` ni `JdbcTemplate` referenciando sus nombres en `src/main/java`, y la migración
repetible que las aplica (`R__procedimientos.sql`) ni siquiera existía todavía en
`artisync/Backend/src/main/resources/db/migration/` — `scripts/sync-procs.sh` nunca se había
ejecutado. Los seis archivos `.sql` y su documentación en `CATALOGO-SP.md` eran correctos, pero la
aplicación seguía ejecutando la lógica original en Java (Specification dinámica, JPQL con `AVG`,
bucles de actualización). El único par de rutinas realmente conectado end-to-end en todo el
repositorio era `fn_listar_cola_verificacion`/`sp_registrar_decision_verificacion`, del módulo de
verificación asistida por IA, que además no estaba documentado en el catálogo.

Esta ampliación resuelve ambos problemas: genera y versiona `R__procedimientos.sql`, y añade siete
rutinas nuevas —con prioridad explícita en el módulo de **seguridad**, ausente por completo de la
Decisión original— verificando en cada caso que el repositorio Spring Data y el servicio Java que la
invoca queden realmente conectados, no solo el archivo `.sql`.

### Candidatos añadidos e implementados

- **`fn_registrar_usuario`** (REQ-F-001) — inserción multi-tabla: `usuarios` + `usuario_roles` +
  `perfiles_creadores` opcional, con validación de correo único, mayoría de edad (RNF-12) y rol
  permitido. Sustituye a `AuthServiceImpl.register`.
- **`fn_resolver_estado_login`** (REQ-F-002) — consulta multi-tabla: resuelve estado de cuenta, 2FA
  y roles (join `usuario_roles`-`roles`) en una sola llamada. Sustituye a las consultas separadas de
  `AuthServiceImpl.login`/`verify2Fa`.
- **`fn_sincronizar_permisos_rol`** (REQ-F-003) — actualización masiva: reemplaza atómicamente
  (DELETE+INSERT) el conjunto de permisos de un rol en `rol_permisos`. Sustituye a
  `RolePermissionServiceImpl.syncPermissions`.
- **`fn_eliminar_rol`** (REQ-F-004) — validación cruzada: borra un rol solo si no es uno de los
  roles base protegidos y no tiene usuarios asignados. Sustituye a
  `RolePermissionServiceImpl.deleteRole`.
- **`fn_restablecer_contrasena`** (REQ-F-005) — validación cruzada + escritura multi-tabla: valida
  (con `FOR UPDATE`) que el token de recuperación no esté usado ni expirado, y actualiza `usuarios` +
  `tokens_recuperacion` atómicamente. Sustituye a `AuthServiceImpl.resetPassword`.
- **`fn_seleccionar_ganadores_sorteo`** (REQ-F-023) — selección aleatoria + actualización masiva: el
  candidato que esta misma Decisión ya identificaba por nombre como pendiente. Sustituye al
  `Collections.shuffle` en Java de `SorteoScheduler.ejecutarSorteo`.
- **`fn_registrar_infraccion`** (REQ-F-015) — cálculo agregado + validación cruzada: inserta la
  infracción, cuenta el total del usuario en 30 días y suspende la cuenta al llegar a 3, en una
  transacción atómica (`FOR UPDATE` sobre `usuarios` antes de suspender). Sustituye a
  `InfraccionServiceImpl.registrarInfraccion`/`suspenderCuenta`.

Documentación funcional completa (parámetros, retorno, tablas implicadas, excepciones) en
[`docs/basedatos/CATALOGO-SP.md`](../basedatos/CATALOGO-SP.md).

### Candidatos identificados pero no implementados en esta ampliación

Quedan documentados como trabajo futuro, sin código ni SQL asociado todavía: `fn_crear_ticket_revision`
(validación de límite de revisiones del contrato en `TicketRevisionServicioImpl`), la resolución de
flujo y avance de etapa de `PedidoServicioImpl` (`crearPedido`/`avanzarEtapa`), el reporte de
auditoría de transacciones por creador, y el listado de reseñas por creador
(`ResenaServicioRepository.findByCreadorIdPerfil`, que además duplica en JPQL lo que
`fn_calificacion_promedio_creador` ya calcula).

### Consecuencia adicional

Las seis rutinas originales de la Tercera Entrega permanecen sin conectar al código Java: esta
ampliación no las reconecta, por estar fuera del alcance solicitado (sumar rutinas nuevas, no reparar
las existentes). Reconectarlas queda pendiente como una brecha aparte, ya no de C1/C6 (la ampliación
suma rutinas realmente en uso) sino de deuda técnica: seis archivos `.sql` y su documentación
describen un comportamiento que el backend en ejecución no reproduce.
