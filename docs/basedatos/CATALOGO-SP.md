# Catálogo de procedimientos almacenados y funciones SQL

Documentación funcional completa de las rutinas versionadas en [`db/procs/`](../../db/procs/), exigida
por el apartado **A.2.1** de la Guía de la Entrega Final.

- **Decisión de diseño:** [`docs/adr/adr-006-estrategia-acceso-datos.md`](../adr/adr-006-estrategia-acceso-datos.md)
- **Contrato del directorio:** [`db/procs/README.md`](../../db/procs/README.md)
- **Trazabilidad:** [`docs/trazabilidad/matriz.csv`](../trazabilidad/matriz.csv), columna `tipo_acceso = SP`
- **Auditoría de SQL dinámico:** [`scripts/audit-sql-dynamic.sh`](../../scripts/audit-sql-dynamic.sh)

## Resumen

El sistema declara **veintiocho rutinas activas** en `db/procs/`. De las seis originales de la
Tercera Entrega (una por categoría funcional del apartado A.2.2), **cinco se retiraron del
catálogo** el 01-09-2026 por no tener nunca un consumidor real desde código Java (`fn_catalogo_filtrado`,
`fn_calificacion_promedio_creador`, `fn_cerrar_pedidos_vencidos`, `fn_liberar_fondos_escrow`,
`fn_generar_codigo_pedido` — ver [ADR-006, sección "Rutinas retiradas"](../adr/adr-006-estrategia-acceso-datos.md#rutinas-retiradas)
para la justificación de cada una); la sexta, `fn_reporte_comisiones_creador`, sí está conectada
end-to-end desde el 26-08-2026. A esa rutina se suman: siete de la ampliación del 16 de
agosto de 2026 (ver [ADR-006, sección Ampliación](../adr/adr-006-estrategia-acceso-datos.md#ampliación)),
con prioridad en el módulo de seguridad; cuatro de la **Fase 1 de concurrencia** del 22 de agosto de
2026 (ver [`PLAN-CONCURRENCIA-SP.md`](PLAN-CONCURRENCIA-SP.md), sección 15 de este catálogo),
orientadas a cerrar anomalías concretas de actualización perdida, lectura no repetible y lectura
fantasma bajo `READ COMMITTED`; una de la **Fase 2 de rendimiento**, ese mismo día (sección 16),
que elimina el N+1 de la ruta de autenticación; siete de la **Fase 3 de concurrencia** (sección
17), que cierran las anomalías restantes del plan (estado a medias, acumulación no controlada,
actualización perdida y lectura fantasma en los flujos de 2FA, recuperación de contraseña,
cambio de contraseña, alta de usuarios/roles y países); una de la **Fase 4 de mantenimiento**
(sección 18) — el único `PROCEDURE` de todo `db/procs/` hasta ese momento — que purga por lotes lo
que las tres fases anteriores dejaban crecer sin límite; una de **retención de notificaciones**
(sección 19, H-08), segundo `PROCEDURE` del directorio; y **seis del módulo de seguidores**
(sección 20), incorporadas el 24-08-2026 para la funcionalidad social de seguir creadores
(`fn_seguir_creador`, `fn_dejar_de_seguir_creador`, `fn_es_seguidor`, `fn_conteo_seguidores`,
`fn_listar_creadores_seguidos_novedades`, `fn_actualizar_portada_creador`). Todas las rutinas
activas posteriores a la original se verificaron conectadas end-to-end (repositorio Spring Data +
servicio Java que las invoca), no solo declaradas en SQL.

| # | Rutina | Categoría funcional | Requisito | Tipo | Volatilidad | Escribe |
|---|---|---|---|---|---|---|
| ~~1~~ | ~~`fn_catalogo_filtrado`~~ | — | REQ-F-013 | — | — | **Retirada, ver [§1](#1-fn_catalogo_filtrado-retirada)** |
| ~~2~~ | ~~`fn_calificacion_promedio_creador`~~ | — | REQ-F-009 | — | — | **Retirada, ver [§2](#2-fn_calificacion_promedio_creador-retirada)** |
| 3 | `fn_reporte_comisiones_creador` | Reportes | REQ-NF-013 | `FUNCTION` | `STABLE` | No |
| ~~4~~ | ~~`fn_cerrar_pedidos_vencidos`~~ | — | REQ-F-019 | — | — | **Retirada, ver [§4](#4-fn_cerrar_pedidos_vencidos-retirada)** |
| ~~5~~ | ~~`fn_liberar_fondos_escrow`~~ | — | REQ-F-021 | — | — | **Retirada, ver [§5](#5-fn_liberar_fondos_escrow-retirada)** |
| ~~6~~ | ~~`fn_generar_codigo_pedido`~~ | — | REQ-F-018 | — | — | **Retirada, ver [§6](#6-fn_generar_codigo_pedido-retirada)** |
| 7 | `fn_registrar_usuario` | Validaciones cruzadas + inserción multi-tabla | REQ-F-001 | `FUNCTION` | `VOLATILE` | Sí |
| 8 | `fn_resolver_estado_login` | Consultas multi-tabla | REQ-F-002 | `FUNCTION` | `STABLE` | No |
| 9 | `fn_sincronizar_permisos_rol` | Actualizaciones masivas | REQ-F-003 | `FUNCTION` | `VOLATILE` | Sí |
| 10 | `fn_eliminar_rol` | Validaciones cruzadas | REQ-F-004 | `FUNCTION` | `VOLATILE` | Sí |
| 11 | `fn_restablecer_contrasena` | Validaciones cruzadas + escritura multi-tabla | REQ-F-005 | `FUNCTION` | `VOLATILE` | Sí |
| 12 | `fn_seleccionar_ganadores_sorteo` | Selección aleatoria + actualización masiva | REQ-F-023 | `FUNCTION` | `VOLATILE` | Sí |
| 13 | `fn_registrar_infraccion` | Cálculos agregados + validación cruzada | REQ-F-015 | `FUNCTION` | `VOLATILE` | Sí |

Fuera de `db/procs/`, el módulo de verificación asistida por IA aporta un par adicional ya conectado
desde antes de esta ampliación: `fn_listar_cola_verificacion` (`FUNCTION`, `STABLE`) y
`sp_registrar_decision_verificacion` (`PROCEDURE`), documentadas en la sección
[14](#14-fn_listar_cola_verificacion-y-sp_registrar_decision_verificacion) de este catálogo.

### Nota sobre modos de parámetro y cursores

Veintiséis de las veintiocho rutinas activas (ocho anteriores + las cuatro de la Fase 1 de
concurrencia + la de la Fase 2 de rendimiento + las siete de la Fase 3 de concurrencia + las seis
del módulo de seguidores, sección 20) se declaran como **funciones** de PostgreSQL con valor de
retorno escalar, `JSONB` o `TABLE`. En consecuencia, para esas veintiséis:

- **Todos los parámetros son de modo `IN`.** No hay parámetros `OUT` ni `INOUT` en ninguna rutina:
  el resultado viaja siempre por el valor de retorno.
- **Ninguna rutina devuelve `refcursor`.** Es una decisión deliberada, no una omisión: un
  `refcursor` obliga a consumir el cursor dentro de la misma transacción y no es representable por
  el contrato de `@Procedure` de Spring Data JPA bajo el modo por defecto del driver
  (`escapeSyntaxCallMode=select`). Los conjuntos de resultados se devuelven como documento `JSONB`,
  que el driver materializa en un único valor. El razonamiento completo está en
  `db/procs/README.md`, sección *"Por qué `fn_` y no `sp_`"*.

Las dos rutinas restantes, `sp_purgar_datos_seguridad` (Fase 4, sección 18) y
`sp_purgar_notificaciones` (retención de notificaciones, sección 19), son las únicas
`PROCEDURE` de `db/procs/`: ambas necesitan `COMMIT`/`ROLLBACK` reales por lote, algo que una
`FUNCTION` no puede hacer bajo ninguna circunstancia (§0.1 de `PLAN-CONCURRENCIA-SP.md`). Ninguna
devuelve nada y ambas se invocan con `CALL`, nunca con `SELECT`.

### Postura de seguridad

Ninguna de las veintiocho rutinas activas construye SQL por concatenación. No aparece `EXECUTE
IMMEDIATE`, `sp_executesql`, `EXECUTE format(...)` ni `EXECUTE <variable>` en ningún archivo. Toda
entrada externa llega como **parámetro formal tipado**, y los filtros opcionales se neutralizan con
el patrón `(p_x IS NULL OR columna = p_x)` en lugar de armar el predicado por texto. Esto satisface
la regla transversal 7 de la guía y el *SQL Injection Prevention Cheat Sheet* de OWASP, que reconoce
el procedimiento almacenado correctamente parametrizado como defensa primaria equivalente al ORM
parametrizado.

---

## 1. `fn_catalogo_filtrado` (retirada)

**Categoría:** consultas multi-tabla · **Requisito:** REQ-F-013

**Retirada del catálogo el 01-09-2026.** Nunca tuvo un consumidor real desde código Java —
verificado con `grep -rn "fn_catalogo_filtrado" artisync/Backend/src/main/java` sin resultados. El
listado del catálogo público sigue resolviéndose por la `Specification` dinámica de Java
(`specification/catalogo/ServicioSpecification.java`), que ya cumplía el mismo propósito. Ver
[ADR-006, sección "Rutinas retiradas"](../adr/adr-006-estrategia-acceso-datos.md#rutinas-retiradas)
para la justificación completa. El archivo `db/procs/fn_catalogo_filtrado.sql` se eliminó del
repositorio; este catálogo conserva la sección para no romper referencias externas al número.

---

## 2. `fn_calificacion_promedio_creador` (retirada)

**Categoría:** cálculos agregados · **Requisito:** REQ-F-009

**Retirada del catálogo el 01-09-2026.** Nunca tuvo un consumidor real desde código Java. La
calificación media de un creador sigue resolviéndose por la consulta JPQL con `AVG` de
`repository/social/ResenaServicioRepository.calcularPromedioByCreadorIdPerfil`, que ya cumplía el
mismo propósito. Ver [ADR-006, sección "Rutinas retiradas"](../adr/adr-006-estrategia-acceso-datos.md#rutinas-retiradas).
El archivo `db/procs/fn_calificacion_promedio_creador.sql` se eliminó del repositorio.

---

## 3. `fn_reporte_comisiones_creador`

**Categoría:** reportes · **Requisito:** REQ-NF-013 · **Archivo:** [`db/procs/fn_reporte_comisiones_creador.sql`](../../db/procs/fn_reporte_comisiones_creador.sql)

Produce el reporte financiero de un creador en una ventana temporal: importe bruto liberado,
comisión de la plataforma, importe neto, número de pedidos y desglose por transacción. Recorre la
cadena completa del patrón *escrow*. Sustituye a la consulta JPQL de tres `JOIN` de
`repository/legal/TransaccionPagoRepository.findByCreadorPerfilId`, que devolvía entidades crudas y
obligaba a agregar en Java.

La comisión se **parametriza** en lugar de fijarse en la rutina, para que el reporte pueda
recalcularse históricamente si la tasa cambia sin versionar una función nueva.

### Parámetros

| # | Nombre | Modo | Tipo | Por defecto | Significado |
|---|---|---|---|---|---|
| 1 | `p_id_perfil` | IN | `BIGINT` | — | Perfil del creador. **Obligatorio**: `NULL` lanza `SQLSTATE 22004` |
| 2 | `p_fecha_desde` | IN | `TIMESTAMP` | `NULL` | Inicio de la ventana (inclusivo). `NULL` = sin cota inferior |
| 3 | `p_fecha_hasta` | IN | `TIMESTAMP` | `NULL` | Fin de la ventana (inclusivo). `NULL` = sin cota superior |
| 4 | `p_tasa_comision` | IN | `NUMERIC(5,4)` | `0.1000` | Tasa de comisión. Se acota a `[0, 1]` |

### Retorno

`JSONB` con: `idPerfil`, `fechaDesde`, `fechaHasta`, `tasaComision`, `totalPedidos`,
`totalOperaciones`, `montoBruto`, `comision`, `montoNeto` y `detalle[]`. Cada entrada de `detalle[]`
contiene `idTransaccion`, `idPedido`, `servicio`, `tipo`, `monto` y `fechaEjecucion` (ISO-8601),
ordenada por fecha descendente.

### Excepciones

| Condición | `SQLSTATE` |
|---|---|
| `p_id_perfil IS NULL` | `22004` (*null value not allowed*) |

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `transacciones_pago` | Lectura |
| `pagos_garantia` | Lectura (JOIN) |
| `contratos` | Lectura (JOIN) |
| `pedidos` | Lectura (JOIN) |
| `servicios` | Lectura (JOIN, filtro por `id_perfil`) |

---

## 4. `fn_cerrar_pedidos_vencidos` (retirada)

**Categoría:** actualizaciones masivas · **Requisito:** REQ-F-019

**Retirada del catálogo el 01-09-2026.** Nunca tuvo un consumidor real desde código Java. El
cierre de pedidos vencidos no está implementado por ninguna otra vía todavía — es trabajo futuro
declarado, no una brecha oculta. Ver [ADR-006, sección "Rutinas retiradas"](../adr/adr-006-estrategia-acceso-datos.md#rutinas-retiradas).
Los índices `idx_pedidos_fecha_entrega_estimada` e `idx_historial_pedido_fecha` que la rutina
aprovechaba se conservan porque siguen siendo útiles para cualquier consulta futura por fecha de
entrega. El archivo `db/procs/fn_cerrar_pedidos_vencidos.sql` se eliminó del repositorio.

---

## 5. `fn_liberar_fondos_escrow` (retirada)

**Categoría:** validaciones cruzadas · **Requisito:** REQ-F-021

**Retirada del catálogo el 01-09-2026.** Nunca tuvo un consumidor real desde código Java. La
liberación de fondos en garantía no está implementada por ninguna otra vía todavía — es trabajo
futuro declarado, no una brecha oculta. Ver [ADR-006, sección "Rutinas retiradas"](../adr/adr-006-estrategia-acceso-datos.md#rutinas-retiradas).
El archivo `db/procs/fn_liberar_fondos_escrow.sql` se eliminó del repositorio.

---

## 6. `fn_generar_codigo_pedido` (retirada)

**Categoría:** generación de códigos secuenciales · **Requisito:** REQ-F-018

**Retirada del catálogo el 01-09-2026.** Nunca tuvo un consumidor real desde código Java — la
columna `pedidos.codigo_pedido` y la secuencia `seq_codigo_pedido` existen en el esquema pero
ningún flujo del backend las llena todavía (`grep -rn "codigo_pedido" artisync/Backend/src/main/java`
sin resultados). Es trabajo futuro declarado, no una brecha oculta. Ver
[ADR-006, sección "Rutinas retiradas"](../adr/adr-006-estrategia-acceso-datos.md#rutinas-retiradas).
El archivo `db/procs/fn_generar_codigo_pedido.sql` se eliminó del repositorio.

---

## 7. `fn_registrar_usuario`

**Categoría:** validaciones cruzadas + inserción multi-tabla · **Requisito:** REQ-F-001 · **Archivo:** [`db/procs/fn_registrar_usuario.sql`](../../db/procs/fn_registrar_usuario.sql)

Registra un nuevo usuario en una única transacción atómica: valida correo único, mayoría de edad
(RNF-12, ≥ 18 años) y rol permitido en auto-registro, e inserta `usuarios` + `usuario_roles` + el
`perfiles_creadores` inicial cuando el rol es CREADOR. Sustituye a `AuthServiceImpl.register`, que
encadenaba `existsByCorreo`, `findByNombreRol` y hasta tres `save()` independientes.

El hash de la contraseña se calcula en Java (BCrypt) y llega ya cifrado; la función nunca ve la
contraseña en texto plano.

### Parámetros

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_nombres` | IN | `VARCHAR(100)` | Nombres del usuario |
| 2 | `p_apellidos` | IN | `VARCHAR(100)` | Apellidos del usuario |
| 3 | `p_correo` | IN | `VARCHAR(150)` | Correo, debe ser único |
| 4 | `p_contrasena_hash` | IN | `VARCHAR(255)` | Hash BCrypt, calculado en Java |
| 5 | `p_fecha_nacimiento` | IN | `DATE` | Debe implicar ≥ 18 años (RNF-12) |
| 6 | `p_nombre_rol` | IN | `VARCHAR(50)` | `CLIENTE` o `CREADOR` únicamente |

### Retorno

`BIGINT` — el `id_usuario` generado.

### Excepciones

| Condición | `SQLSTATE` |
|---|---|
| Correo ya registrado | `23505` |
| Menor de 18 años | `23514` |
| Rol distinto de CLIENTE/CREADOR | `23514` |
| Rol inexistente en `roles` | `23503` |

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `usuarios` | Lectura (verificación de correo) y escritura (`INSERT`) |
| `roles` | Lectura |
| `usuario_roles` | Escritura (`INSERT`) |
| `perfiles_creadores` | Escritura condicional (`INSERT` solo si rol = CREADOR) |

---

## 8. `fn_resolver_estado_login`

**Categoría:** consultas multi-tabla · **Requisito:** REQ-F-002 · **Archivo:** [`db/procs/fn_resolver_estado_login.sql`](../../db/procs/fn_resolver_estado_login.sql)

Resuelve, tras una autenticación por contraseña ya exitosa, todo lo que el flujo de login necesita en
una sola llamada: datos básicos de la cuenta, si el 2FA está habilitado y la lista de roles (join
`usuario_roles`-`roles`). Sustituye a las tres consultas independientes que hacían
`AuthServiceImpl.login` y `verify2Fa` (`findByCorreo`, `findByUsuarioIdUsuario` en 2FA,
`findByUsuarioIdUsuario` en roles).

No participa en la validación de la contraseña: eso permanece en `AuthenticationManager` de Spring
Security (BCrypt vive fuera del motor de datos).

### Parámetros

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_correo` | IN | `VARCHAR(150)` | Correo del usuario ya autenticado |

### Retorno

`JSONB` con la forma `{ idUsuario, correo, nombres, apellidos, estadoCuenta, dosFactoresHabilitado, roles[] }`.
Devuelve `NULL` si el correo no existe.

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `usuarios` | Lectura |
| `autenticacion_dos_factores` | Lectura (`LEFT JOIN`) |
| `usuario_roles` | Lectura (subconsulta de agregación) |
| `roles` | Lectura (JOIN dentro de la subconsulta) |

---

## 9. `fn_sincronizar_permisos_rol`

**Categoría:** actualizaciones masivas · **Requisito:** REQ-F-003 · **Archivo:** [`db/procs/fn_sincronizar_permisos_rol.sql`](../../db/procs/fn_sincronizar_permisos_rol.sql)

Reemplaza atómicamente el conjunto completo de permisos de un rol (`DELETE` + `INSERT ... SELECT`
sobre `rol_permisos`). Sustituye a `RolePermissionServiceImpl.syncPermissions`, que dejaba que
Hibernate calculara el diff de la colección `@ManyToMany` `Rol.permisos` al hacer `save()`.

Valida todos los códigos de permiso antes de aplicar cualquier cambio: si uno solo no existe, no se
modifica nada.

### Parámetros

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_nombre_rol` | IN | `VARCHAR(50)` | Rol a sincronizar |
| 2 | `p_codigos_permiso` | IN | `TEXT[]` | Conjunto completo de códigos de permiso deseado |

### Retorno

`INTEGER` — número de permisos finalmente asignados.

### Excepciones

| Condición | `SQLSTATE` |
|---|---|
| Rol no encontrado | `P0002` |
| Algún código de permiso inexistente | `23503` |

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `roles` | Lectura |
| `permisos` | Lectura (validación de códigos) |
| `rol_permisos` | Escritura (`DELETE` + `INSERT`) |

---

## 10. `fn_eliminar_rol`

**Categoría:** validaciones cruzadas · **Requisito:** REQ-F-004 · **Archivo:** [`db/procs/fn_eliminar_rol.sql`](../../db/procs/fn_eliminar_rol.sql)

Elimina un rol personalizado solo tras comprobar, en la misma transacción que el `DELETE`, que no es
uno de los roles base protegidos del sistema y que no tiene usuarios activos asignados. Sustituye a
`RolePermissionServiceImpl.deleteRole`.

### Parámetros

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_rol` | IN | `BIGINT` | Rol a eliminar |

### Retorno

`BOOLEAN` — `TRUE` si eliminó el rol.

### Excepciones

| Condición | `SQLSTATE` |
|---|---|
| Rol no encontrado | `P0002` |
| Rol base protegido (ADMIN, CLIENTE, CREADOR, MODERADOR, SOPORTE, AUDITOR_FINANCIERO) | `23514` |
| Tiene usuarios activos asignados | `23514` |

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `roles` | Lectura y escritura (`DELETE`) |
| `usuario_roles` | Lectura (conteo de asignaciones) |
| `rol_permisos` | Escritura (`DELETE`, limpieza de la tabla puente) |

---

## 11. `fn_restablecer_contrasena`

**Categoría:** validaciones cruzadas + escritura multi-tabla · **Requisito:** REQ-F-005 · **Archivo:** [`db/procs/fn_restablecer_contrasena.sql`](../../db/procs/fn_restablecer_contrasena.sql)

Aplica un restablecimiento de contraseña a partir de un token de recuperación: valida (con
`SELECT ... FOR UPDATE`) que exista, no esté usado y no haya expirado (ventana de 60 minutos), y
actualiza `usuarios` + `tokens_recuperacion` atómicamente. Sustituye a
`AuthServiceImpl.resetPassword`. El bloqueo de fila serializa restablecimientos concurrentes con el
mismo token, cerrando la ventana de doble uso que tenía la versión en dos `save()` secuenciales.

### Parámetros

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_hash_token` | IN | `VARCHAR(255)` | SHA-256 del token, calculado en Java |
| 2 | `p_nueva_contrasena_hash` | IN | `VARCHAR(255)` | Hash BCrypt de la nueva contraseña |

### Retorno

`BIGINT` — el `id_usuario` cuya contraseña se actualizó.

### Excepciones

| Condición | `SQLSTATE` |
|---|---|
| Token inexistente, ya usado o expirado | `23514` |

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `tokens_recuperacion` | Lectura con `FOR UPDATE` y escritura (`UPDATE usado = TRUE`) |
| `usuarios` | Escritura (`UPDATE contrasena_hash`) |

---

## 12. `fn_seleccionar_ganadores_sorteo`

**Categoría:** selección aleatoria + actualización masiva · **Requisito:** REQ-F-023 · **Archivo:** [`db/procs/fn_seleccionar_ganadores_sorteo.sql`](../../db/procs/fn_seleccionar_ganadores_sorteo.sql)

Selecciona aleatoriamente a los ganadores de un sorteo entre los participantes que aún no han
ganado, y marca en bloque tanto a los participantes ganadores como el sorteo. Es el candidato que el
propio ADR-006 ya identificaba por nombre como pendiente de implementación. Sustituye a
`SorteoScheduler.ejecutarSorteo`, que traía todos los participantes a la aplicación,
`Collections.shuffle(new SecureRandom())` en Java y actualizaba fila por fila en un bucle.

La fila del sorteo se toma con `FOR UPDATE` para serializar ejecuciones concurrentes del mismo
sorteo; `ORDER BY random() LIMIT n` resuelve la selección y la actualización de todos los ganadores
en una única sentencia. La notificación en tiempo real (WebSocket) permanece en Java.

### Parámetros

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_sorteo` | IN | `BIGINT` | Sorteo a resolver. **Obligatorio** |

### Retorno

`JSONB` con la forma `{ idSorteo, tituloSorteo, estado, ganadores: [ { idParticipacion, idUsuario } ] }`.
Idempotente: una segunda llamada sobre un sorteo que ya no está `'Activo'` devuelve el estado actual
con `ganadores: []` sin volver a sortear.

### Excepciones

| Condición | `SQLSTATE` |
|---|---|
| `p_id_sorteo IS NULL` | `22004` |
| Sorteo no encontrado | `P0002` |

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `sorteos` | Lectura con `FOR UPDATE` y escritura (`UPDATE estado_sorteo`) |
| `participantes_sorteo` | Lectura y escritura (`UPDATE es_ganador, fecha_notificacion_premio`) |

---

## 13. `fn_registrar_infraccion`

**Categoría:** cálculos agregados + validación cruzada · **Requisito:** REQ-F-015 · **Archivo:** [`db/procs/fn_registrar_infraccion.sql`](../../db/procs/fn_registrar_infraccion.sql)

Registra una infracción de mensaje (RF-15: intento de compartir datos de contacto fuera del chat) y,
en la misma transacción, cuenta cuántas infracciones acumula el usuario en la ventana móvil de 30
días; si alcanza 3 o más, suspende la cuenta. Sustituye a
`InfraccionServiceImpl.registrarInfraccion`/`suspenderCuenta`, que hacía un `INSERT`, un `COUNT` y un
`UPDATE` condicional como tres llamadas independientes.

La fila de `usuarios` se toma con `FOR UPDATE` antes de decidir la suspensión, para serializar la
carrera cuando dos infracciones del mismo usuario llegan casi al mismo tiempo. El filtrado de
patrones (detección de teléfonos/correos/redes en el texto) permanece en Java
(`MensajeFilterService`): es lógica de texto, no de datos.

### Parámetros

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario` | IN | `BIGINT` | Usuario infractor. **Obligatorio** |
| 2 | `p_id_pedido` | IN | `BIGINT` | Pedido asociado (puede ser `NULL`) |
| 3 | `p_mensaje_original` | IN | `TEXT` | Texto del mensaje filtrado |
| 4 | `p_patron_detectado` | IN | `VARCHAR(50)` | Patrón detectado por `MensajeFilterService` |

### Retorno

`JSONB` con la forma `{ idInfraccion, totalInfraccionesPeriodo, cuentaSuspendida }`.

### Excepciones

| Condición | `SQLSTATE` |
|---|---|
| `p_id_usuario IS NULL` | `22004` |
| Usuario no encontrado | `P0002` |

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `infracciones_mensaje` | Escritura (`INSERT`) |
| `usuarios` | Lectura (conteo, `FOR UPDATE` antes de suspender) y escritura condicional (`UPDATE estado_cuenta = FALSE`) |

---

## Estructuras de soporte

El archivo [`db/procs/V8__estructuras_para_procedimientos.sql`](../../db/procs/V8__estructuras_para_procedimientos.sql)
no declara ninguna rutina: aporta el DDL que las rutinas anteriores necesitan.

| Objeto | Tipo | Usado por |
|---|---|---|
| `pedidos.codigo_pedido` | Columna | `fn_generar_codigo_pedido` (retirada, §6) — queda disponible para una futura implementación |
| `uq_pedidos_codigo_pedido` | Restricción `UNIQUE` | `fn_generar_codigo_pedido` (retirada, §6) |
| `seq_codigo_pedido` | Secuencia | `fn_generar_codigo_pedido` (retirada, §6) |
| 4 índices de apoyo | Índices | Predicados de filtrado y JOIN de rutinas 1 y 4 (ambas retiradas) y de la rutina 3 activa |

---

## 14. `fn_listar_cola_verificacion` y `sp_registrar_decision_verificacion`

**Categorías:** consultas multi-tabla (rutina 14a) y validaciones cruzadas (rutina 14b) · **Requisitos:** REQ-F-006 / REQ-F-007 · **Archivo:** [`artisync/Backend/src/main/resources/db/migration/V7__verificacion_asistida_ia.sql`](../../artisync/Backend/src/main/resources/db/migration/V7__verificacion_asistida_ia.sql)

Estas dos rutinas del módulo de verificación asistida por IA nacieron **antes** de que existiera
`db/procs/` como ubicación canónica, por lo que viven en su propia migración versionada en vez de en
un archivo de `db/procs/` concatenado por `sync-procs.sh`. Se documentan aquí porque, a diferencia de
las rutinas 1–6 originales, **sí estaban conectadas end-to-end** desde antes de la ampliación del 16
de agosto de 2026: `repository/perfil/CertificadoIaRepository.java` las invoca con `@Query` nativa
(`fn_listar_cola_verificacion`) y `@Procedure` (`sp_registrar_decision_verificacion`) — el único
ejemplo de `@Procedure` funcionando en todo el repositorio, y el motivo por el que la ampliación usa
`@Query` nativa (no `@Procedure`) para las siete `FUNCTION` nuevas: `@Procedure` solo está verificado
aquí contra un `CREATE PROCEDURE` real, no contra una `FUNCTION` invocada con la sintaxis de escape.

### 14a. `fn_listar_cola_verificacion` — consultas multi-tabla

Devuelve la cola de certificados de identidad/documentos pendientes de revisión moderada, uniendo
`certificados_ia` con `perfiles_creadores`, `usuarios` y `estados_verificacion`, con paginado y
filtro opcional por estado.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_estado` | IN | `VARCHAR` | Filtra por nombre de estado. `NULL` desactiva el filtro |
| 2 | `p_limite` | IN | `INT` | Tamaño de página |
| 3 | `p_offset` | IN | `INT` | Desplazamiento |

**Retorno:** conjunto de filas (`RETURNS TABLE`) con `id_certificado`, `id_perfil`,
`nombre_creador`, `tipo_documento`, `nombre_estado`, `veredicto_ia`, `puntaje_confianza_ia`,
`fecha_analisis`, ordenado por `fecha_analisis ASC`.

**Tablas implicadas:** `certificados_ia` (lectura), `perfiles_creadores` (JOIN),
`usuarios` (JOIN), `estados_verificacion` (JOIN).

### 14b. `sp_registrar_decision_verificacion` — validaciones cruzadas

Único punto de escritura de `id_estado_verificacion`: valida que el certificado exista y esté en
estado `PENDIENTE` (no se puede sobrescribir una decisión ya tomada) antes de registrar la decisión
del moderador.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_certificado` | IN | `BIGINT` | Certificado sobre el que se decide |
| 2 | `p_id_estado` | IN | `BIGINT` | Nuevo estado (`APROBADO`/`RECHAZADO`/`REQUIERE_ACLARACION`) |
| 3 | `p_id_moderador` | IN | `BIGINT` | Moderador que registra la decisión |
| 4 | `p_nota` | IN | `TEXT` | Nota del moderador |

**Retorno:** ninguno (`PROCEDURE`).

**Excepciones:** certificado inexistente, o certificado que no está en estado `PENDIENTE`
(`RAISE EXCEPTION` sin `ERRCODE` explícito — usa el código genérico `P0001`).

**Tablas implicadas:** `certificados_ia` (lectura y escritura de `id_estado_verificacion`),
`estados_verificacion` (lectura).

---

## 15. Fase 1 de concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md)

Cuatro rutinas adicionales, incorporadas para cerrar anomalías de concurrencia concretas del módulo
de seguridad bajo `READ COMMITTED` (nivel de aislamiento efectivo del proyecto — ver
[`PLAN-CONCURRENCIA-SP.md §0.4`](PLAN-CONCURRENCIA-SP.md#04-nivel-de-aislamiento-vigente-en-el-proyecto)).
A diferencia de las 13 anteriores, su motivación primaria no es rendimiento sino **corrección bajo
acceso concurrente**: cada una documenta la anomalía que cierra y la técnica usada (predicado
`UPDATE ... WHERE`, `SELECT ... FOR UPDATE`, o `DELETE ... RETURNING`).

| # | Rutina | Categoría funcional | Anomalía que cierra | Tipo | Volatilidad | Escribe |
|---|---|---|---|---|---|---|
| 14 | `fn_consumir_codigo_respaldo_2fa` | Validaciones cruzadas | Actualización perdida (A1) | `FUNCTION` | `VOLATILE` | Sí |
| 15 | `fn_sincronizar_roles_usuario` | Actualizaciones masivas | Lectura fantasma (A2) | `FUNCTION` | `VOLATILE` | Sí |
| 16 | `fn_revocar_sesiones_usuario` | Actualizaciones masivas | Lectura no repetible (A6) | `FUNCTION` | `VOLATILE` | Sí |
| 17 | `fn_cambiar_estado_cuenta` | Validaciones cruzadas | Actualización perdida / lectura no repetible (A6) | `FUNCTION` | `VOLATILE` | Sí |

### 15a. `fn_consumir_codigo_respaldo_2fa`

**Archivo:** [`db/procs/fn_consumir_codigo_respaldo_2fa.sql`](../../db/procs/fn_consumir_codigo_respaldo_2fa.sql)

Sustituye a `TwoFactorServiceImpl.validarCodigoOBackup` (rama de códigos de respaldo), que leía
todos los códigos no usados del usuario a memoria Java y comparaba en un bucle — dos peticiones
concurrentes con el mismo código lo consumían ambas (actualización perdida, bypass de segundo
factor). La rutina es un único `UPDATE ... WHERE usado = FALSE RETURNING`: bajo READ COMMITTED,
PostgreSQL re-evalúa el predicado sobre la versión confirmada más reciente (*EvalPlanQual*) antes
de aplicar el cambio, de modo que el segundo llamante concurrente nunca encuentra fila.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario` | IN | `BIGINT` | Usuario dueño del código |
| 2 | `p_codigo_hash` | IN | `VARCHAR(255)` | SHA-256 del código de respaldo ingresado |

**Retorno:** `BOOLEAN` — `TRUE` solo para el primer llamante concurrente que consume el código.

**Excepciones:** `p_id_usuario`/`p_codigo_hash` nulos (`22004`).

**Tablas implicadas:** `codigos_respaldo_2fa` (lectura y escritura condicionada).

### 15b. `fn_sincronizar_roles_usuario`

**Archivo:** [`db/procs/fn_sincronizar_roles_usuario.sql`](../../db/procs/fn_sincronizar_roles_usuario.sql)

Gemela de `fn_sincronizar_permisos_rol` (#9) para el lado usuario↔rol. Sustituye a
`AdminUserServiceImpl.actualizarRoles`, que hacía `findByUsuarioIdUsuario` + `deleteAll` + `flush` +
por cada rol nuevo (`findByNombreRol` + `save` + alta de perfil de creador) — unos 10 viajes sin
atomicidad entre ellos. `SELECT ... FOR UPDATE` sobre `usuarios` serializa dos sincronizaciones
concurrentes del mismo usuario; `ON CONFLICT (id_usuario, id_rol) DO NOTHING`, respaldado por la
restricción `uq_usuario_rol` (V14), cierra estructuralmente la lectura fantasma que antes permitía
roles duplicados. Valida todos los roles nuevos antes de borrar los antiguos.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario` | IN | `BIGINT` | Usuario a sincronizar |
| 2 | `p_nombres_rol` | IN | `TEXT[]` | Conjunto completo de roles deseado |

**Retorno:** `INTEGER` — número de filas de `usuario_roles` insertadas.

**Excepciones:**

| Condición | `SQLSTATE` |
|---|---|
| Usuario no encontrado | `P0002` |
| Array de roles nulo o vacío | `22004` |
| Algún nombre de rol inexistente | `23514` |

**Tablas implicadas:** `usuarios` (bloqueo `FOR UPDATE`), `roles` (lectura), `usuario_roles`
(escritura `DELETE`+`INSERT`), `perfiles_creadores` (alta perezosa si se asigna `CREADOR`).

### 15c. `fn_revocar_sesiones_usuario`

**Archivo:** [`db/procs/fn_revocar_sesiones_usuario.sql`](../../db/procs/fn_revocar_sesiones_usuario.sql)

Sustituye la parte SQL de `SessionRevocationService.revocarSesionesUsuario`, que hacía
`findByUsuarioIdUsuario` + revocación en Redis + `deleteByUsuarioIdUsuario` en tres pasos: una
sesión creada entre el primero y el último se borraba de la base sin haberse revocado nunca en
Redis (lectura no repetible). `DELETE ... RETURNING` lee y borra en una sola sentencia sobre un
único snapshot.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario` | IN | `BIGINT` | Usuario cuyas sesiones se revocan |

**Retorno:** `TABLE (jti VARCHAR(36), segundos_restantes INTEGER)` — una fila por sesión borrada.

**Excepciones:** `p_id_usuario` nulo (`22004`).

**Tablas implicadas:** `sesiones_usuario` (lectura y escritura en la misma sentencia).

### 15d. `fn_cambiar_estado_cuenta`

**Archivo:** [`db/procs/fn_cambiar_estado_cuenta.sql`](../../db/procs/fn_cambiar_estado_cuenta.sql)

Unifica el patrón "cambiar `estado_cuenta` + revocar sesiones si hubo transición activa→inactiva"
que se repetía en `AdminUserServiceImpl.changeEstado`/`deleteUser`/`updateUser` y
`UserServiceImpl.deleteOwnAccount`. `SELECT ... FOR UPDATE` sobre `usuarios` serializa dos
administradores concurrentes sobre el mismo usuario, evitando que uno decida no revocar sesiones a
partir de un `estadoAnterior` que el otro ya dejó obsoleto (actualización perdida). Delega en
`fn_revocar_sesiones_usuario` (15c) cuando corresponde.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario` | IN | `BIGINT` | Usuario a modificar |
| 2 | `p_estado` | IN | `BOOLEAN` | Nuevo valor de `estado_cuenta` |

**Retorno:** `TABLE (jti VARCHAR(36), segundos_restantes INTEGER)` — vacío si no hubo transición
activa→inactiva.

**Excepciones:**

| Condición | `SQLSTATE` |
|---|---|
| Usuario no encontrado | `P0002` |
| Parámetros nulos | `22004` |

**Tablas implicadas:** `usuarios` (bloqueo `FOR UPDATE` + escritura), `sesiones_usuario`
(vía `fn_revocar_sesiones_usuario`).

### Objetos de esquema de apoyo (V17__concurrencia_seguridad.sql)

| Objeto | Tabla | Propósito |
|---|---|---|
| `uq_usuario_rol` | `usuario_roles (id_usuario, id_rol)` | Respalda el `ON CONFLICT` de 15b; cierra la lectura fantasma de A2 de forma estructural |
| `uq_codigo_respaldo_usuario_hash` | `codigos_respaldo_2fa (id_usuario, codigo_hash)` | Localiza por índice el código que consume 15a |
| `idx_usuario_roles_id_usuario` | `usuario_roles (id_usuario)` | Ruta caliente: `CustomUserDetailsService`, `UsuarioMapper`, `fn_resolver_estado_login` |
| `idx_usuario_roles_id_rol` | `usuario_roles (id_rol)` | `existsByRolIdRol`, `fn_eliminar_rol`, `findIdsUsuarioByNombreRol` |
| `idx_tokens_recuperacion_hash` | `tokens_recuperacion (hash_token)` | Evita que el `FOR UPDATE` de `fn_restablecer_contrasena` (#11) degrade a *seq scan* |
| `idx_usuarios_id_pais` | `usuarios (id_pais)` | `existsByPaisIdPais` |

### Objetos de esquema de apoyo (V21__indices_fk_alto_trafico.sql)

Índices de clave foránea ausentes desde V1 en las tres tablas de mayor tráfico del sistema
(H-07 de la auditoría de estado del 2026-08-26), siguiendo el mismo patrón de V17.

| Objeto | Tabla | Propósito |
|---|---|---|
| `idx_pedidos_id_usuario_cliente` | `pedidos (id_usuario_cliente)` | `PedidoRepository.findByUsuarioClienteIdUsuario` ("mis pedidos" del cliente) |
| `idx_pedidos_id_servicio` | `pedidos (id_servicio)` | `findByServicioPerfilIdPerfil`, `findByServicioPerfilUsuarioIdUsuario` ("mis pedidos" del creador) |
| `idx_pedidos_id_flujo` | `pedidos (id_flujo)` | FK sin caller de repositorio conocido; evita *seq scan* al modificar `flujos_trabajo` |
| `idx_mensajes_sala_fecha` | `mensajes (id_sala, fecha_hora_envio)` | `MensajeRepository.findBySalaIdSalaOrderByFechaHoraEnvioAsc` — resuelve `WHERE` y `ORDER BY` de una pasada |
| `idx_mensajes_id_remitente` | `mensajes (id_remitente)` | FK sin índice |
| `idx_notificaciones_usuario_fecha` | `notificaciones_sistema (id_usuario, fecha_emision DESC)` | `findByUsuarioIdUsuarioOrderByFechaEmisionDesc` (listado paginado) |
| `idx_notificaciones_no_leidas` | `notificaciones_sistema (id_usuario) WHERE esta_leida = false` | `countByUsuarioIdUsuarioAndEstaLeidaFalse`, `marcarTodasLeidas` — parcial porque la mayoría de filas acaban leídas |
| `idx_notificaciones_id_tipo` | `notificaciones_sistema (id_tipo_notificacion)` | FK sin índice |

---

## 16. Fase 2 de rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8)

Una rutina adicional, orientada a eliminar el N+1 de la ruta de autenticación (la más caliente del
sistema: se ejecuta en cada petición autenticada). El segundo hallazgo de rendimiento de la Fase 2
— el listado paginado de administración de usuarios — **no** se resolvió con una rutina almacenada:
ver la nota al final de esta sección.

| # | Rutina | Categoría funcional | Motivación | Tipo | Volatilidad | Escribe |
|---|---|---|---|---|---|---|
| 18 | `fn_permisos_efectivos_usuario` | Consultas multi-tabla | N+1 en cada petición autenticada | `FUNCTION` | `STABLE` | No |

### 16a. `fn_permisos_efectivos_usuario`

**Archivo:** [`db/procs/fn_permisos_efectivos_usuario.sql`](../../db/procs/fn_permisos_efectivos_usuario.sql)

Sustituye a `CustomUserDetailsService.loadUserByUsername`, invocado por `JwtAuthenticationFilter`
en **cada** petición autenticada. La versión anterior hacía `findByCorreo` + `findByUsuarioIdUsuario`
sobre `usuario_roles` + un `SELECT` adicional por cada rol al acceder a `Rol.permisos`
(`FetchType.EAGER`) — entre 4 y 8 consultas por petición con N+1 clásico. La rutina resuelve
usuario + `authorities` (roles con prefijo `ROLE_` más permisos, deduplicados vía `UNION` dentro de
un `jsonb_agg(DISTINCT ...)`) en una sola sentencia `STABLE`: todas las subconsultas se evalúan
sobre el mismo snapshot, así que roles y permisos quedan garantizados coherentes entre sí, algo que
las consultas independientes no garantizaban bajo `READ COMMITTED` si una sincronización
(`fn_sincronizar_roles_usuario`, `fn_sincronizar_permisos_rol`) se colaba justo entre ellas.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_correo` | IN | `VARCHAR(150)` | Correo del usuario a autenticar |

**Retorno:** `JSONB` con la forma `{ idUsuario, correo, contrasenaHash, estadoCuenta,
authorities: [] }`. `NULL` si el correo no existe — `CustomUserDetailsService` lo traduce a
`UsernameNotFoundException`, igual que antes.

**Tablas implicadas:** `usuarios` (lectura), `usuario_roles` (lectura), `roles` (lectura),
`rol_permisos` (lectura), `permisos` (lectura).

### Nota: el listado de administración de usuarios se resolvió sin rutina almacenada

`AdminUserServiceImpl.getAllUsers` (`GET /api/v1/admin/usuarios`) tenía el mismo síntoma de N+1 que
la ruta de autenticación: `UsuarioMapper.toUserResponse` se invocaba una vez por fila de la página
(~2 consultas por usuario). El plan original proponía una rutina `fn_listar_usuarios_admin` al
estilo de `fn_catalogo_filtrado` (§1, retirada). Al implementarla se identificó un conflicto real con una
funcionalidad ya existente: el endpoint acepta `sortBy` **arbitrario** sobre cualquier campo
paginable (`?sortBy=correo&direction=desc`), resuelto hoy por `Pageable`/`Sort` de Spring Data, que
JPA traduce de forma segura a `ORDER BY` parametrizado. Reproducir un `ORDER BY` por columna
arbitraria dentro de una función SQL exige `EXECUTE format(...)` (SQL dinámico) o una enumeración
manual de columnas soportadas — lo primero viola la regla transversal 7 de la guía (cero SQL
dinámico, verificado por `scripts/audit-sql-dynamic.sh`); lo segundo degradaría o rompería el
contrato de ordenamiento que el frontend ya consume.

La solución adoptada resuelve el N+1 sin tocar ese contrato: `usuarioRepository.findAll(pageable)`
seguimos usándolo (conserva el `Sort` dinámico intacto), pero `UsuarioMapper` gana un método por
lotes, `toUserResponseList`, que sustituye N invocaciones de `findByUsuarioIdUsuario` /
`findByUsuarioIdUsuario` (roles y 2FA) por **dos** consultas `findByUsuarioIdUsuarioIn(ids)` sobre
toda la página a la vez. Para una página de 20 usuarios: de ~42 consultas a 4 (página, conteo,
roles por lote, 2FA por lote). Ver
[`UsuarioMapper.java`](../../artisync/Backend/src/main/java/uteq/edu/ec/artisync/service/shared/UsuarioMapper.java)
y [`PagedResponseBuilder.buildAndMapList`](../../artisync/Backend/src/main/java/uteq/edu/ec/artisync/util/PagedResponseBuilder.java).

Pendiente identificado pero fuera de alcance de esta fase: `UsuarioMapper` accede a
`usuario.getPais()` (`FetchType.LAZY`) por cada fila sin batch, un N+1 menor preexistente (no
introducido por esta fase) sobre una tabla pequeña y de baja cardinalidad. Candidato a un
`JOIN FETCH` en `findAll` si el catálogo de países creciera.

---

## 17. Fase 3 de concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §4, §6, §7)

Siete rutinas que cierran las anomalías restantes del plan: A3 (lectura fantasma en
`createUser`), A4 (estado a medias en `setup2Fa`, código duplicado entre `disable2Fa` y
`updateUser`), A5 (acumulación no controlada en `forgotPassword`), A7 (actualización perdida en
`changePassword`), A8 (lectura fantasma en `createRole`) y A9 (lectura fantasma en `updatePais`).
Dos técnicas se repiten en todas ellas:

- **Captura de `unique_violation`** (`A3`, `A8`, `A9`): PostgreSQL no ofrece bloqueo de rango bajo
  `READ COMMITTED`, así que no existe forma de "bloquear un nombre/correo que aún no existe". La
  única defensa correcta es intentar la escritura y capturar la violación de la restricción
  `UNIQUE` con un bloque `EXCEPTION` — el mismo molde en las tres rutinas.
- **`UPDATE` condicionado** (`A7`): compare-and-swap sobre el propio valor verificado, sin
  necesidad de `SELECT ... FOR UPDATE` previo.

Dos de las siete **componen** con rutinas de fases anteriores en vez de reimplementar su lógica:
`fn_crear_usuario_admin` invoca a `fn_sincronizar_roles_usuario` (Fase 1, §3) y `fn_crear_rol`
invoca a `fn_sincronizar_permisos_rol` (#9, ya existente).

| # | Rutina | Categoría funcional | Anomalía que cierra | Tipo | Volatilidad | Escribe |
|---|---|---|---|---|---|---|
| 19 | `fn_configurar_2fa` | Validaciones cruzadas + escritura multi-tabla | Estado a medias (A4) | `FUNCTION` | `VOLATILE` | Sí |
| 20 | `fn_desactivar_2fa` | Validaciones cruzadas + escritura multi-tabla | Código duplicado (A4) | `FUNCTION` | `VOLATILE` | Sí |
| 21 | `fn_solicitar_recuperacion` | Validaciones cruzadas + escritura multi-tabla | Acumulación no controlada (A5) | `FUNCTION` | `VOLATILE` | Sí |
| 22 | `fn_cambiar_contrasena` | Validaciones cruzadas | Actualización perdida (A7) | `FUNCTION` | `VOLATILE` | Sí |
| 23 | `fn_crear_usuario_admin` | Validaciones cruzadas + inserción multi-tabla | Lectura fantasma (A3) | `FUNCTION` | `VOLATILE` | Sí |
| 24 | `fn_crear_rol` | Validaciones cruzadas | Lectura fantasma (A8) | `FUNCTION` | `VOLATILE` | Sí |
| 25 | `fn_guardar_pais` | Validaciones cruzadas | Lectura fantasma (A9) | `FUNCTION` | `VOLATILE` | Sí |

### 17a. `fn_configurar_2fa` y `fn_desactivar_2fa`

**Archivos:** [`db/procs/fn_configurar_2fa.sql`](../../db/procs/fn_configurar_2fa.sql),
[`db/procs/fn_desactivar_2fa.sql`](../../db/procs/fn_desactivar_2fa.sql)

`fn_configurar_2fa` sustituye la parte de escritura de `TwoFactorServiceImpl.setup2Fa`: upsert
(`ON CONFLICT` sobre `autenticacion_dos_factores.id_usuario`, ya `UNIQUE`) del secreto TOTP +
reemplazo completo de los 8 códigos de respaldo, en una transacción — antes eran 10 pasos no
atómicos (upsert manual + `DELETE` + 8 `save()` individuales) que podían dejar un secreto nuevo
emparejado con un juego de códigos incompleto si el proceso fallaba a mitad del bucle.

`fn_desactivar_2fa` desactiva el flag y purga los códigos de respaldo atómicamente, y es
**idempotente** (devuelve `FALSE`, no lanza, si el usuario no tenía 2FA configurado). Unifica el
código que antes estaba **duplicado** entre `TwoFactorServiceImpl.disable2Fa` (con código validado)
y `AdminUserServiceImpl.updateUser` (forzado por un administrador, sin validar código).

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario` | IN | `BIGINT` | Usuario a configurar/desactivar |
| 2 | `p_llave_secreta` *(solo configurar)* | IN | `VARCHAR(255)` | Secreto TOTP nuevo |
| 3 | `p_hashes` *(solo configurar)* | IN | `TEXT[]` | SHA-256 de los códigos de respaldo nuevos |

**Retorno:** `fn_configurar_2fa` → `INTEGER` (códigos insertados). `fn_desactivar_2fa` → `BOOLEAN`
(`TRUE` si había 2FA configurado).

**Tablas implicadas:** `autenticacion_dos_factores` (upsert / `UPDATE`), `codigos_respaldo_2fa`
(`DELETE` + `INSERT`).

### 17b. `fn_solicitar_recuperacion`

**Archivo:** [`db/procs/fn_solicitar_recuperacion.sql`](../../db/procs/fn_solicitar_recuperacion.sql)

Sustituye la parte de escritura de `AuthServiceImpl.forgotPassword`, que insertaba un
`TokenRecuperacion` sin invalidar los anteriores: cada solicitud de recuperación dejaba un token
más válido durante 60 minutos (ventana de `fn_restablecer_contrasena`, #11), acumulando N tokens
utilizables simultáneamente. La rutina invalida los tokens previos no usados e inserta el nuevo en
la misma transacción, bajo `SELECT ... FOR UPDATE` sobre `usuarios` (serializa dobles solicitudes
del mismo correo). Preserva la respuesta indistinguible: devuelve `NULL` (no lanza) si la cuenta no
existe o está inactiva.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_correo` | IN | `VARCHAR(150)` | Correo que solicita la recuperación |
| 2 | `p_hash_token` | IN | `VARCHAR(255)` | SHA-256 del nuevo token (plano generado y enviado por Java) |

**Retorno:** `JSONB` `{ idUsuario, nombres }`, o `NULL` si la cuenta no existe/está inactiva.

**Tablas implicadas:** `usuarios` (bloqueo `FOR UPDATE`), `tokens_recuperacion` (`UPDATE` + `INSERT`).

### 17c. `fn_cambiar_contrasena`

**Archivo:** [`db/procs/fn_cambiar_contrasena.sql`](../../db/procs/fn_cambiar_contrasena.sql)

Sustituye la parte de escritura de `UserServiceImpl.changePassword`. BCrypt permanece fuera del
motor (la comparación de la contraseña actual se sigue haciendo en Java); lo que se traslada es la
**escritura condicionada**: `UPDATE ... WHERE contrasena_hash = p_hash_esperado`, un
compare-and-swap con el propio hash verificado como testigo de versión. Si otra sesión cambió la
contraseña entre la verificación en Java y este `UPDATE`, el predicado no coincide (0 filas) y la
función lanza en vez de pisar en silencio ese cambio.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario` | IN | `BIGINT` | Usuario que cambia su contraseña |
| 2 | `p_hash_esperado` | IN | `VARCHAR(255)` | Hash BCrypt ya verificado en Java |
| 3 | `p_hash_nuevo` | IN | `VARCHAR(255)` | Hash BCrypt de la nueva contraseña |

**Retorno:** `BOOLEAN` (`TRUE` si se aplicó). Lanza `SQLSTATE 40001` (`serialization_failure`) si el
hash ya no coincidía — la capa Java lo traduce a `409 CONFLICT`.

**Tablas implicadas:** `usuarios` (`UPDATE` condicionado).

### 17d. `fn_crear_usuario_admin` y `fn_crear_rol`

**Archivos:** [`db/procs/fn_crear_usuario_admin.sql`](../../db/procs/fn_crear_usuario_admin.sql),
[`db/procs/fn_crear_rol.sql`](../../db/procs/fn_crear_rol.sql)

Sustituyen `AdminUserServiceImpl.createUser` y `RolePermissionServiceImpl.createRole`, que
comprobaban `existsByCorreo`/`findByNombreRol` y luego hacían `save()` en sentencias separadas —
lectura fantasma no atómica, mitigada en la práctica por las restricciones `UNIQUE` pero sin
traducción de error (500 crudo en vez de 409). Ambas capturan `unique_violation` con un bloque
`EXCEPTION` (mismo molde que `fn_crear_rol` describe en su cabecera) y delegan el resto en rutinas
ya existentes: `fn_crear_usuario_admin` invoca `fn_sincronizar_roles_usuario` (Fase 1, §15b) para
los roles y el alta perezosa de `perfiles_creadores`; `fn_crear_rol` invoca
`fn_sincronizar_permisos_rol` (#9) para los permisos iniciales.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| — | *(`fn_crear_usuario_admin`)* | | | `p_nombres, p_apellidos, p_correo, p_contrasena_hash, p_fecha_nacimiento, p_id_pais, p_estado_cuenta, p_nombres_rol` |
| — | *(`fn_crear_rol`)* | | | `p_nombre_rol, p_descripcion_rol, p_codigos_permiso` |

**Retorno:** `BIGINT` (id generado) en ambas.

**Tablas implicadas:** `usuarios` / `roles` (`INSERT` capturando `unique_violation`), más las de la
rutina delegada (`usuario_roles`+`perfiles_creadores`, o `rol_permisos`, respectivamente).

### 17e. `fn_guardar_pais`

**Archivo:** [`db/procs/fn_guardar_pais.sql`](../../db/procs/fn_guardar_pais.sql)

Sustituye la parte de escritura de `PaisServiceImpl.createPais` y `.updatePais`. Una sola rutina
cubre ambos casos (`p_id_pais NULL` = crear, con valor = renombrar) porque comparten la misma
técnica: ni crear ni renombrar pueden usar `SELECT ... FOR UPDATE` para "bloquear" el nombre en
conflicto (esa fila, si existe, pertenece a *otro* país); la única defensa correcta en ambos casos
es la restricción `UNIQUE` como predicado. Renombrar un país a su propio nombre actual no dispara
la restricción (misma fila, mismo valor), preservando el comportamiento previo de permitir ese
"no-op". La capa Java (`PaisServiceImpl`) traduce el resultado de vuelta al vocabulario de
excepciones ya establecido en ese servicio (`ExcepcionRecursoDuplicado`/`ExcepcionRecursoNoEncontrado`)
en vez de adoptar `ResponseStatusException`, para no romper su contrato con la capa de presentación.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_pais` | IN | `BIGINT` | `NULL` para crear; id del país a renombrar en caso contrario |
| 2 | `p_nombre_pais` | IN | `VARCHAR(100)` | Nombre nuevo |

**Retorno:** `BIGINT` (id del país afectado).

**Tablas implicadas:** `pais` (`INSERT`/`UPDATE` capturando `unique_violation`).

---

## 18. Fase 4 de mantenimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §7)

Una rutina, y la única `PROCEDURE` (no `FUNCTION`) de todo `db/procs/`: corrige la anomalía A10
— `sesiones_usuario`, `tokens_recuperacion` y `codigos_respaldo_2fa` crecían sin ningún proceso de
purga; el índice `idx_sesiones_usuario_fecha_expiracion` que ya crea V8 no lo usaba nadie.

| # | Rutina | Categoría funcional | Anomalía que cierra | Tipo | Volatilidad | Escribe |
|---|---|---|---|---|---|---|
| 26 | `sp_purgar_datos_seguridad` | Actualizaciones masivas (mantenimiento) | Crecimiento sin límite (A10) | `PROCEDURE` | `VOLATILE` | Sí |

### 18a. `sp_purgar_datos_seguridad`

**Archivo:** [`db/procs/sp_purgar_datos_seguridad.sql`](../../db/procs/sp_purgar_datos_seguridad.sql)

Es la única rutina de todo el módulo que hace `COMMIT`/`ROLLBACK` **reales** (§0.3 del plan): un
`PROCEDURE`, invocado con `CALL`, fuera de cualquier transacción abierta. Borrar en una sola
transacción un historial completo de sesiones/tokens mantendría una transacción de larga duración
que bloquearía a `VACUUM` en toda la base mientras dura, así que confirma **un lote a la vez**
(`FOR UPDATE SKIP LOCKED` + `COMMIT`), compatible con tráfico de login/logout concurrente: en vez
de esperar una fila bloqueada, la salta y la recoge en la siguiente ejecución diaria.

Alcance deliberadamente acotado en `codigos_respaldo_2fa`: **solo** purga los ya consumidos
(`usado = TRUE`). Los códigos sin usar de un usuario con 2FA deshabilitado **no** se tocan —
la tabla no tiene columna de fecha que distinga un código huérfano (2FA desactivado hace tiempo) de
uno recién generado por `fn_configurar_2fa` a la espera de `confirm2Fa`; purgar por
`esta_habilitado = FALSE` borraría códigos de una configuración de 2FA en curso, todavía sin
confirmar. `tokens_recuperacion` sí se purga por antigüedad (usados, o generados hace más de 24h —
la ventana de validez real es 60 minutos, ver `fn_restablecer_contrasena` #11).

| # | Nombre | Modo | Tipo | Por defecto | Significado |
|---|---|---|---|---|---|
| 1 | `p_tamano_lote` | IN | `INTEGER` | `1000` | Filas por lote antes de cada `COMMIT` |

**Retorno:** ninguno (`PROCEDURE`). Cualquier fallo hace `ROLLBACK` del lote en curso (los lotes ya
confirmados permanecen) y relanza la excepción.

**Tablas implicadas:** `sesiones_usuario` (`DELETE` de expiradas), `tokens_recuperacion` (`DELETE`
de usados/>24h), `codigos_respaldo_2fa` (`DELETE` solo de usados).

**Invocación:** `SeguridadPurgaScheduler` (`@Scheduled(cron = "0 30 3 * * *")`), vía `JdbcTemplate`
bajo `@Transactional(propagation = Propagation.NOT_SUPPORTED)` — obligatorio, no cosmético: un
`PROCEDURE` con `COMMIT` interno falla con `2D000 invalid_transaction_termination` si Spring ya
abrió una transacción antes de invocarlo.

**Privilegios:** a diferencia de las `FUNCTION` de `db/procs/` (cubiertas por
`ALTER DEFAULT PRIVILEGES ... GRANT EXECUTE ON FUNCTIONS` en `seed_privilegios.sh`), un
`PROCEDURE` requiere su propio `GRANT EXECUTE ON PROCEDURE` explícito — mismo patrón que
`sp_registrar_decision_verificacion` (§14b), el único otro `PROCEDURE` del proyecto.

---

## 19. Política de retención de notificaciones (H-08, auditoría de estado 2026-08-26)

Una rutina más, siguiendo exactamente el mismo patrón de la sección 18: `notificaciones_sistema`
crecía sin ningún proceso de purga. Es la única de las tres tablas de alto volumen de escritura
candidatas que se puede purgar sin riesgo — ver
[`docs/basedatos/POLITICA-RETENCION.md`](POLITICA-RETENCION.md) para por qué `auditoria_eventos` y
`mensajes` quedan explícitamente fuera de alcance.

| # | Rutina | Categoría funcional | Anomalía que cierra | Tipo | Volatilidad | Escribe |
|---|---|---|---|---|---|---|
| 27 | `sp_purgar_notificaciones` | Actualizaciones masivas (mantenimiento) | Crecimiento sin límite (H-08) | `PROCEDURE` | `VOLATILE` | Sí |

### 19a. `sp_purgar_notificaciones`

**Archivo:** [`db/procs/sp_purgar_notificaciones.sql`](../../db/procs/sp_purgar_notificaciones.sql)

Mismo mecanismo que `sp_purgar_datos_seguridad` (§18a): `PROCEDURE` con `COMMIT` real por lote
(`FOR UPDATE SKIP LOCKED`, `ORDER BY id_notificacion`), sin bloque `EXCEPTION` (PostgreSQL prohíbe
`COMMIT` con un `SAVEPOINT` implícito abierto). Purga las notificaciones **ya leídas** con más de
`p_dias_retencion` días de antigüedad. Las no leídas **nunca** se tocan, sin importar su
antigüedad: el usuario todavía no las vio.

| # | Nombre | Modo | Tipo | Por defecto | Significado |
|---|---|---|---|---|---|
| 1 | `p_tamano_lote` | IN | `INTEGER` | `1000` | Filas por lote antes de cada `COMMIT` |
| 2 | `p_dias_retencion` | IN | `INTEGER` | `90` | Días desde `fecha_emision` tras los que una notificación leída se purga |

**Retorno:** ninguno (`PROCEDURE`).

**Tablas implicadas:** `notificaciones_sistema` (`DELETE` de leídas con más de `p_dias_retencion`
días). Sin FKs entrantes ni triggers — segura de purgar sin efectos colaterales.

**Invocación:** `NotificacionesPurgaScheduler` (`@Scheduled(cron = "0 0 4 * * *")`), vía
`JdbcTemplate` bajo `@Transactional(propagation = Propagation.NOT_SUPPORTED)`, mismo motivo que
§18a.

**Privilegios:** `GRANT EXECUTE ON PROCEDURE` propio, guardado tras la existencia del rol
`artisync_app` — mismo patrón que §18a.

---

## 20. Módulo de seguidores (feature social, 24-08-2026)

Seis rutinas incorporadas el 24-08-2026 para la funcionalidad social de seguir creadores. A
diferencia de las secciones 15/17 (concurrencia) y 16 (rendimiento), su motivación primaria es
funcional: modelan el ciclo completo de la relación "seguir" (crear, verificar, contar, listar con
novedades) más un ajuste de perfil asociado a la vista de un creador. Todas están conectadas
end-to-end desde `service/comunicacion/impl/SeguidorServicioImpl.java`, vía
`repository/comunicacion/SeguidorRepository.java`.

| # | Rutina | Categoría funcional | Tipo | Volatilidad | Escribe |
|---|---|---|---|---|---|
| 28 | `fn_seguir_creador` | Validaciones cruzadas + escritura multi-tabla | `FUNCTION` | `VOLATILE` | Sí |
| 29 | `fn_dejar_de_seguir_creador` | Actualizaciones masivas / eliminación | `FUNCTION` | `VOLATILE` | Sí |
| 30 | `fn_es_seguidor` | Consultas multi-tabla / validaciones | `FUNCTION` | `STABLE` | No |
| 31 | `fn_conteo_seguidores` | Cálculos agregados | `FUNCTION` | `STABLE` | No |
| 32 | `fn_listar_creadores_seguidos_novedades` | Consultas multi-tabla / reportes | `FUNCTION` | `STABLE` | No |
| 33 | `fn_actualizar_portada_creador` | Actualizaciones | `FUNCTION` | `VOLATILE` | Sí |

### 20a. `fn_seguir_creador`

**Archivo:** [`db/procs/fn_seguir_creador.sql`](../../db/procs/fn_seguir_creador.sql)

Registra que un usuario sigue a un perfil de creador. Valida que el perfil exista y que el
usuario no intente seguirse a sí mismo, e inserta en `seguidores` con `ON CONFLICT
(id_usuario_seguidor, id_perfil_creador) DO NOTHING` — idempotente ante doble clic o reintento.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario_seguidor` | IN | `BIGINT` | Usuario que sigue |
| 2 | `p_id_perfil_creador` | IN | `BIGINT` | Perfil de creador a seguir |

**Retorno:** `BOOLEAN` (`TRUE` si la operación se completó, incluso si ya existía el seguimiento).

**Excepciones:** `RAISE EXCEPTION` sin `SQLSTATE` explícito (código genérico `P0001`, mismo caso
que `sp_registrar_decision_verificacion`, §14b) para: parámetros nulos, perfil de creador
inexistente, o auto-seguimiento.

**Tablas implicadas:** `perfiles_creadores` (lectura, resolución del dueño del perfil),
`seguidores` (escritura, `INSERT`).

### 20b. `fn_dejar_de_seguir_creador`

**Archivo:** [`db/procs/fn_dejar_de_seguir_creador.sql`](../../db/procs/fn_dejar_de_seguir_creador.sql)

Elimina la relación de seguimiento entre un usuario y un perfil de creador. No valida existencia
previa: un `DELETE` sobre una relación inexistente es un no-op silencioso.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario_seguidor` | IN | `BIGINT` | Usuario que deja de seguir |
| 2 | `p_id_perfil_creador` | IN | `BIGINT` | Perfil de creador dejado de seguir |

**Retorno:** `BOOLEAN` — `FALSE` solo si algún parámetro llega `NULL`; `TRUE` en cualquier otro
caso (exista o no la relación).

**Tablas implicadas:** `seguidores` (escritura, `DELETE`).

### 20c. `fn_es_seguidor`

**Archivo:** [`db/procs/fn_es_seguidor.sql`](../../db/procs/fn_es_seguidor.sql)

Verifica si un usuario sigue a un perfil de creador determinado, para pintar el estado del botón
"Seguir"/"Siguiendo" en el frontend.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario_seguidor` | IN | `BIGINT` | Usuario a verificar |
| 2 | `p_id_perfil_creador` | IN | `BIGINT` | Perfil de creador a verificar |

**Retorno:** `BOOLEAN` — `TRUE` si existe la relación; `FALSE` si no existe o si algún parámetro es
`NULL`.

**Tablas implicadas:** `seguidores` (lectura, `EXISTS`).

### 20d. `fn_conteo_seguidores`

**Archivo:** [`db/procs/fn_conteo_seguidores.sql`](../../db/procs/fn_conteo_seguidores.sql)

Calcula el número total de seguidores de un perfil de creador, para el contador visible en su
página pública.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_perfil_creador` | IN | `BIGINT` | Perfil de creador a contar |

**Retorno:** `BIGINT` — total de filas en `seguidores` para ese perfil; `0` si el parámetro es
`NULL`.

**Tablas implicadas:** `seguidores` (lectura, `COUNT(*)`).

### 20e. `fn_listar_creadores_seguidos_novedades`

**Archivo:** [`db/procs/fn_listar_creadores_seguidos_novedades.sql`](../../db/procs/fn_listar_creadores_seguidos_novedades.sql)

Devuelve, para un usuario, los creadores que sigue junto con un resumen de novedades por creador,
ordenado por fecha de seguimiento descendente. Une `seguidores` con `perfiles_creadores` y
`usuarios` para componer el handle y los datos de presentación de cada creador seguido.

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_usuario_seguidor` | IN | `BIGINT` | Usuario cuyos creadores seguidos se listan |

**Retorno:** `TABLE (id_perfil, id_usuario, nombres_usuario, apellidos_usuario, handle,
url_foto_perfil, titulo_profesional, resumen_novedad, tipo_novedad, fecha_novedad)`. El resumen y
el tipo de novedad son actualmente valores fijos (`'Actividad reciente en su perfil'`,
`'GENERAL'`) — la rutina deja el contrato listo para un feed de novedades real sin romper
consumidores si esa lógica se implementa después.

**Tablas implicadas:** `seguidores` (lectura), `perfiles_creadores` (JOIN), `usuarios` (JOIN).

### 20f. `fn_actualizar_portada_creador`

**Archivo:** [`db/procs/fn_actualizar_portada_creador.sql`](../../db/procs/fn_actualizar_portada_creador.sql)

Actualiza la URL de portada y/o el título profesional de un perfil de creador. Usa `COALESCE`
sobre cada campo para permitir actualizaciones parciales (pasar `NULL` conserva el valor actual).

| # | Nombre | Modo | Tipo | Significado |
|---|---|---|---|---|
| 1 | `p_id_perfil` | IN | `BIGINT` | Perfil de creador a actualizar. **Obligatorio** |
| 2 | `p_url_portada` | IN | `VARCHAR(500)` | Nueva URL de portada; `NULL` conserva la actual |
| 3 | `p_titulo_profesional` | IN | `VARCHAR(150)` | Nuevo título profesional; `NULL` conserva el actual |

**Retorno:** `BOOLEAN` — `FOUND` (`TRUE` si el perfil existía y se actualizó).

**Excepciones:** `p_id_perfil IS NULL` → `RAISE EXCEPTION` sin `SQLSTATE` explícito (`P0001`).

**Tablas implicadas:** `perfiles_creadores` (escritura, `UPDATE`).
