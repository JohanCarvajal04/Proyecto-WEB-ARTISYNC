# ADR-006: Estrategia híbrida de acceso a datos (ORM + procedimientos almacenados)

**Estado:** Aceptado — ampliado el 16 de agosto de 2026 (ver [Ampliación](#ampliación)); cinco
rutinas originales retiradas del catálogo el 01-09-2026 (ver [Rutinas retiradas](#rutinas-retiradas))
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
(`ResenaServicioRepository.findByCreadorIdPerfil`, que además duplica en JPQL lo que calculaba
`fn_calificacion_promedio_creador`, hoy retirada — ver más abajo).

### Rutinas retiradas

**Actualización 01-09-2026.** De las seis rutinas originales de la Tercera Entrega, una
(`fn_reporte_comisiones_creador`) se conectó end-to-end el 26-08-2026, vía `@Query(nativeQuery=true)`
en `TransaccionPagoRepository.java:26`. Las otras cinco nunca tuvieron un consumidor real desde
código Java y se retiraron del catálogo (`db/procs/`, `docs/basedatos/CATALOGO-SP.md`) en lugar de
mantenerlas indefinidamente como documentación de un comportamiento que el backend en ejecución no
reproducía:

- **`fn_catalogo_filtrado`** — el listado del catálogo público sigue resolviéndose por la
  `Specification` dinámica de Java (`specification/catalogo/ServicioSpecification.java`), que ya
  cumple el mismo propósito y no tiene el conflicto de SQL dinámico que sí tendría una función
  equivalente para `sortBy` arbitrario (ver la nota sobre `fn_listar_usuarios_admin` en
  `CATALOGO-SP.md`).
- **`fn_calificacion_promedio_creador`** — la calificación media de un creador sigue
  resolviéndose por la consulta JPQL con `AVG` de
  `ResenaServicioRepository.calcularPromedioByCreadorIdPerfil`, que ya cumple el mismo propósito.
- **`fn_cerrar_pedidos_vencidos`**, **`fn_liberar_fondos_escrow`**, **`fn_generar_codigo_pedido`**
  — ninguna de estas tres operaciones (cierre automático de pedidos vencidos, liberación de fondos
  en garantía, asignación de código público de pedido) está implementada todavía por ninguna vía,
  ni ORM ni procedimiento almacenado. Es trabajo futuro declarado del dominio, no una brecha oculta
  ni una regresión: las columnas/secuencias de soporte (`pedidos.codigo_pedido`,
  `seq_codigo_pedido`) quedan en el esquema para cuando se implemente.

Documentación completa de cada retiro, con la evidencia de `grep` que confirma la ausencia de
consumidor, en [`docs/basedatos/CATALOGO-SP.md`](../basedatos/CATALOGO-SP.md), secciones 1, 2, 4, 5
y 6.

## Mecanismo de invocación: `@Query(nativeQuery=true)` frente a `@Procedure` (OBS-AUTO-11)

El apartado A.2.1 de la guía pide invocar las rutinas «mediante los mecanismos formales de la
especificación JPA 2.1 (`@Procedure` sobre método de repositorio Spring Data o
`@NamedStoredProcedureQuery` sobre entidad)» y **prohíbe expresamente** invocarlas «mediante
concatenación de cadenas en `createNativeQuery(...)`».

**Estado real, verificado el 20-08-2026.** De las rutinas conectadas, siete se invocan con
`@Query(value = "SELECT fn_...(:param)", nativeQuery = true)` y parámetros nombrados
(`UsuarioRepository`, `RolRepository`, `InfraccionRepository`, `SorteoRepository`), y una con el
mecanismo formal `@Procedure`
(`repository/perfil/CertificadoIaRepository.java:24`, `sp_registrar_decision_verificacion`).
No existe **ninguna** ocurrencia de `createNativeQuery` en el backend
(`grep -rn "createNativeQuery" artisync/Backend/src` → sin resultados), ni ninguna concatenación de
entrada de usuario en SQL: la regla que el script `scripts/audit-sql-dynamic.sh` verifica en cada
ejecución del CI.

**Decisión: no refactorizar las siete invocaciones existentes**, y declarar la divergencia
abiertamente. Las razones:

1. **Son funciones, no procedimientos.** Las rutinas son `fn_*` (`CREATE FUNCTION`), y `@Procedure`
   está definido en JPA sobre `StoredProcedureQuery`, que emite la sintaxis de escape JDBC
   `{call ...}`. Forzar ese camino sobre funciones de PostgreSQL que devuelven `JSONB` o escalares
   es frágil y aporta poco.
2. **La prohibición explícita de la norma se cumple con holgura.** Lo que A.2.1 prohíbe es la
   concatenación de cadenas; aquí todos los argumentos viajan como parámetros nombrados vinculados
   por el driver. La *Cheat Sheet* de prevención de inyección SQL de OWASP reconoce las consultas
   parametrizadas y los procedimientos almacenados parametrizados como defensas primarias
   **equivalentes**.
3. **El riesgo supera el beneficio.** Reescribir siete puntos de acceso a datos en el tramo final de
   la entrega, con 522 pruebas dependiendo de ellos, cambia comportamiento verificado a cambio de
   una diferencia interpretativa de forma, no de seguridad.

**Compromiso hacia adelante:** las rutinas que se conecten a partir de ahora usarán `@Procedure`
siempre que el tipo de retorno lo permita, para elevar el número de invocaciones que satisfacen la
letra del requisito además de su espíritu.
