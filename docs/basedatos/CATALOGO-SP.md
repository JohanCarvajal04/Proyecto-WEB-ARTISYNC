# Catálogo de procedimientos almacenados y funciones SQL

Documentación funcional completa de las rutinas versionadas en [`db/procs/`](../../db/procs/), exigida
por el apartado **A.2.1** de la Guía de la Entrega Final.

- **Decisión de diseño:** [`docs/adr/adr-006-estrategia-acceso-datos.md`](../adr/adr-006-estrategia-acceso-datos.md)
- **Contrato del directorio:** [`db/procs/README.md`](../../db/procs/README.md)
- **Trazabilidad:** [`docs/trazabilidad/matriz.csv`](../trazabilidad/matriz.csv), columna `tipo_acceso = SP`
- **Auditoría de SQL dinámico:** [`scripts/audit-sql-dynamic.sh`](../../scripts/audit-sql-dynamic.sh)

## Resumen

El sistema declara **catorce rutinas** en `db/procs/` — las seis originales de la Tercera Entrega,
una por cada categoría funcional del apartado A.2.2, más ocho de la ampliación del 16 de agosto de
2026 (ver [ADR-006, sección Ampliación](../adr/adr-006-estrategia-acceso-datos.md#ampliación)), con
prioridad en el módulo de seguridad. A diferencia de las seis originales, las ocho nuevas se
verificaron conectadas end-to-end (repositorio Spring Data + servicio Java que las invoca), no solo
declaradas en SQL.

| # | Rutina | Categoría funcional | Requisito | Tipo | Volatilidad | Escribe |
|---|---|---|---|---|---|---|
| 1 | `fn_catalogo_filtrado` | Consultas multi-tabla | REQ-F-013 | `FUNCTION` | `STABLE` | No |
| 2 | `fn_calificacion_promedio_creador` | Cálculos agregados | REQ-F-009 | `FUNCTION` | `STABLE` | No |
| 3 | `fn_reporte_comisiones_creador` | Reportes | REQ-NF-013 | `FUNCTION` | `STABLE` | No |
| 4 | `fn_cerrar_pedidos_vencidos` | Actualizaciones masivas | REQ-F-019 | `FUNCTION` | `VOLATILE` | Sí |
| 5 | `fn_liberar_fondos_escrow` | Validaciones cruzadas | REQ-F-021 | `FUNCTION` | `VOLATILE` | Sí |
| 6 | `fn_generar_codigo_pedido` | Generación de códigos secuenciales | REQ-F-018 | `FUNCTION` | `VOLATILE` | Sí |
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

Las seis rutinas se declaran como **funciones** de PostgreSQL con valor de retorno escalar o
`JSONB`. En consecuencia:

- **Todos los parámetros son de modo `IN`.** No hay parámetros `OUT` ni `INOUT` en ninguna rutina:
  el resultado viaja siempre por el valor de retorno.
- **Ninguna rutina devuelve `refcursor`.** Es una decisión deliberada, no una omisión: un
  `refcursor` obliga a consumir el cursor dentro de la misma transacción y no es representable por
  el contrato de `@Procedure` de Spring Data JPA bajo el modo por defecto del driver
  (`escapeSyntaxCallMode=select`). Los conjuntos de resultados se devuelven como documento `JSONB`,
  que el driver materializa en un único valor. El razonamiento completo está en
  `db/procs/README.md`, sección *"Por qué `fn_` y no `sp_`"*.

### Postura de seguridad

Ninguna de las seis rutinas construye SQL por concatenación. No aparece `EXECUTE IMMEDIATE`,
`sp_executesql`, `EXECUTE format(...)` ni `EXECUTE <variable>` en ningún archivo. Toda entrada
externa llega como **parámetro formal tipado**, y los filtros opcionales se neutralizan con el
patrón `(p_x IS NULL OR columna = p_x)` en lugar de armar el predicado por texto.

El único uso del operador `||` en todo el directorio es la construcción de **texto de datos**, no de
SQL: los comodines de un `ILIKE` sobre un parámetro ya tipado como `VARCHAR`
(`fn_catalogo_filtrado`), la observación de auditoría (`fn_cerrar_pedidos_vencidos`) y el código de
pedido (`fn_generar_codigo_pedido`). Esto satisface la regla transversal 7 de la guía y el
*SQL Injection Prevention Cheat Sheet* de OWASP, que reconoce el procedimiento almacenado
correctamente parametrizado como defensa primaria equivalente al ORM parametrizado.

---

## 1. `fn_catalogo_filtrado`

**Categoría:** consultas multi-tabla · **Requisito:** REQ-F-013 · **Archivo:** [`db/procs/fn_catalogo_filtrado.sql`](../../db/procs/fn_catalogo_filtrado.sql)

Devuelve la página del catálogo público que satisface una combinación de filtros que cruza cinco
tablas más la agregación de reseñas del creador. Sustituye a la `Specification` dinámica de Java
(`specification/catalogo/ServicioSpecification.java`), que construía el predicado en tiempo de
ejecución desde la capa de servicio.

El predicado se evalúa **una sola vez**: el total de coincidencias se obtiene con la función ventana
`COUNT(*) OVER ()` sobre el mismo conjunto que se pagina, de modo que `total` y `elementos` no
pueden divergir.

### Parámetros

| # | Nombre | Modo | Tipo | Por defecto | Significado |
|---|---|---|---|---|---|
| 1 | `p_id_categoria` | IN | `BIGINT` | `NULL` | Filtra por categoría. `NULL` desactiva el filtro |
| 2 | `p_id_subcategoria` | IN | `BIGINT` | `NULL` | Filtra por subcategoría. `NULL` desactiva el filtro |
| 3 | `p_precio_min` | IN | `NUMERIC(10,2)` | `NULL` | Cota inferior de `precio_base` (inclusiva) |
| 4 | `p_precio_max` | IN | `NUMERIC(10,2)` | `NULL` | Cota superior de `precio_base` (inclusiva) |
| 5 | `p_etiqueta` | IN | `VARCHAR(50)` | `NULL` | Nombre exacto de etiqueta; se resuelve con `EXISTS` |
| 6 | `p_texto` | IN | `VARCHAR(150)` | `NULL` | Búsqueda `ILIKE` sobre `titulo_servicio` |
| 7 | `p_limite` | IN | `INTEGER` | `20` | Tamaño de página. Se acota a `[1, 100]` (REQ-NF-004) |
| 8 | `p_desplazamiento` | IN | `INTEGER` | `0` | Desplazamiento. Se acota a `>= 0` |

### Retorno

`JSONB` con la forma `{ total, limite, offset, elementos[] }`. Cada elemento de `elementos[]`
contiene: `idServicio`, `titulo`, `descripcion`, `precioBase`, `urlMiniatura`, `tipoItem`,
`idPerfil`, `idSubcategoria`, `subcategoria`, `idCategoria`, `categoria`, `calificacionMedia` y
`etiquetas[]`. Devuelve `elementos: []` y `total: 0` cuando no hay coincidencias, nunca `NULL`.

### Tablas y rutinas implicadas

| Objeto | Acceso |
|---|---|
| `servicios` | Lectura |
| `subcategorias` | Lectura (JOIN) |
| `categorias` | Lectura (JOIN) |
| `servicio_etiquetas` | Lectura (`EXISTS` y subconsulta de agregación) |
| `etiquetas` | Lectura (JOIN) |
| `fn_calificacion_promedio_creador` | Invocación anidada (rutina 2) |

**Filtros fijos:** `estado_publicacion = 'ACTIVO'` y `categorias.estado_activa IS TRUE`.

---

## 2. `fn_calificacion_promedio_creador`

**Categoría:** cálculos agregados · **Requisito:** REQ-F-009 · **Archivo:** [`db/procs/fn_calificacion_promedio_creador.sql`](../../db/procs/fn_calificacion_promedio_creador.sql)

Calcula la calificación media (1..5) de un creador agregando las reseñas de todos los pedidos
servidos por sus servicios. Sustituye a la consulta JPQL con `AVG` y dos `JOIN` de
`repository/social/ResenaServicioRepository.calcularPromedioByCreadorIdPerfil`.

El redondeo a dos decimales se hace **en el motor**, para que todos los consumidores (API REST,
`fn_catalogo_filtrado`, reportes) reciban exactamente el mismo valor y no se introduzcan
discrepancias de redondeo entre capas.

### Parámetros

| # | Nombre | Modo | Tipo | Por defecto | Significado |
|---|---|---|---|---|---|
| 1 | `p_id_perfil` | IN | `BIGINT` | — | Perfil del creador. `NULL` devuelve `NULL` |

### Retorno

`NUMERIC(3,2)` — media redondeada a dos decimales.

**`NULL` cuando el creador aún no acumula reseñas.** La ausencia de calificación es semánticamente
distinta de una calificación de `0.0`, y el frontend las presenta de forma distinta
("Sin valoraciones" vs. "0 estrellas").

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `resenas_servicios` | Lectura |
| `pedidos` | Lectura (JOIN) |
| `servicios` | Lectura (JOIN, filtro por `id_perfil`) |

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

## 4. `fn_cerrar_pedidos_vencidos`

**Categoría:** actualizaciones masivas · **Requisito:** REQ-F-019 · **Archivo:** [`db/procs/fn_cerrar_pedidos_vencidos.sql`](../../db/procs/fn_cerrar_pedidos_vencidos.sql)

Cierra en bloque los pedidos cuya fecha de entrega estimada ya venció y que todavía no alcanzaron
una etapa final de su flujo de trabajo.

**Por qué es un `INSERT` y no un `UPDATE`:** el modelo de datos de Artisync no guarda el estado en
la tabla `pedidos` — el estado vigente es la última fila de `historial_estados_pedido`. El cierre se
materializa por tanto como una transición nueva hacia la etapa final configurada para el flujo del
pedido (`flujo_etapas_config` con `es_etapa_final = TRUE` y el `numero_orden` más alto).

Ejecutarlo como una sola sentencia en el motor, en lugar de iterar en Java pedido por pedido, evita
el patrón N+1 que tenía `PedidoServicioImpl` y garantiza que todas las transiciones compartan la
misma transacción y la misma marca de tiempo.

**Idempotente:** un pedido que ya está en etapa final queda excluido por el predicado.

### Parámetros

| # | Nombre | Modo | Tipo | Por defecto | Significado |
|---|---|---|---|---|---|
| 1 | `p_dias_gracia` | IN | `INTEGER` | `0` | Días de gracia tras el vencimiento. Se acota a `>= 0` (un valor negativo cerraría pedidos aún vigentes) |

### Retorno

`INTEGER` — número de pedidos cerrados (`ROW_COUNT` del `INSERT`). `0` si no había ninguno vencido.

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `flujo_etapas_config` | Lectura (`DISTINCT ON` por flujo, mayor `numero_orden`) |
| `historial_estados_pedido` | Lectura (último estado por pedido) **y escritura (`INSERT`)** |
| `pedidos` | Lectura (JOIN, filtro por `fecha_entrega_estimada`) |

---

## 5. `fn_liberar_fondos_escrow`

**Categoría:** validaciones cruzadas · **Requisito:** REQ-F-021 · **Archivo:** [`db/procs/fn_liberar_fondos_escrow.sql`](../../db/procs/fn_liberar_fondos_escrow.sql)

Libera los fondos retenidos de un pedido bajo el patrón *escrow*, pero solo después de comprobar,
dentro de la misma transacción y con el registro bloqueado, **cinco condiciones de negocio**:

1. El pedido tiene contrato formalizado y pago de garantía asociado.
2. Los fondos están en estado `'Retenido'` (no liberados ni reembolsados).
3. El contrato está firmado por ambas partes (cliente y creador).
4. Existe al menos un entregable final cargado para el pedido.
5. No queda ningún ticket de revisión abierto.

**Por qué en el motor y no en `PagoServicioImpl`:** las cinco lecturas y las tres escrituras deben
ser atómicas frente a otra liberación concurrente del mismo pedido. La fila de `pagos_garantia` se
toma con `SELECT ... FOR UPDATE OF pg`, de modo que dos llamadas simultáneas se serializan y la
segunda observa el estado ya cambiado por la primera. Resolverlo en Java exigiría un bloqueo
pesimista explícito y varias idas y vueltas a la base.

### Parámetros

| # | Nombre | Modo | Tipo | Por defecto | Significado |
|---|---|---|---|---|---|
| 1 | `p_id_pedido` | IN | `BIGINT` | — | Pedido cuyos fondos se liberan. **Obligatorio** |

### Retorno

`BOOLEAN` — `TRUE` si liberó los fondos; `FALSE` si no había nada que liberar.

El contrato distingue deliberadamente **"ya estaba liberado"** (devuelve `FALSE`, idempotencia) de
**"no se puede liberar"** (lanza excepción), para que la capa de servicio pueda tratar cada caso de
forma distinta.

### Excepciones

| Condición | Mensaje | `SQLSTATE` |
|---|---|---|
| `p_id_pedido IS NULL` | `p_id_pedido es obligatorio` | `22004` |
| Sin contrato con pago de garantía | `El pedido % no tiene contrato con pago de garantia asociado` | `23503` |
| Contrato sin ambas firmas | `El contrato del pedido % no esta firmado por ambas partes` | `23514` |
| Sin entregables finales | `El pedido % no tiene entregables finales cargados` | `23514` |
| Tickets de revisión abiertos | `El pedido % tiene % ticket(s) de revision abiertos` | `23514` |

### Tablas implicadas

| Objeto | Acceso |
|---|---|
| `pagos_garantia` | Lectura con `FOR UPDATE` **y escritura (`UPDATE estado_fondos = 'Liberado'`)** |
| `contratos` | Lectura (JOIN, verificación de firmas) |
| `entregables_finales` | Lectura (conteo) **y escritura (`UPDATE esta_liberado = TRUE`)** |
| `tickets_revision` | Lectura (conteo de estado `'Abierto'`) |
| `transacciones_pago` | **Escritura (`INSERT` de tipo `'LIBERACION'`)** |

---

## 6. `fn_generar_codigo_pedido`

**Categoría:** generación de códigos secuenciales · **Requisito:** REQ-F-018 · **Archivo:** [`db/procs/fn_generar_codigo_pedido.sql`](../../db/procs/fn_generar_codigo_pedido.sql)

Genera y asigna el código público de un pedido con el formato `ART-<AAAA>-<NNNNNN>`
(p. ej. `ART-2026-000042`), donde `<AAAA>` es el año de creación del pedido y `<NNNNNN>` un
correlativo de seis dígitos tomado de la secuencia `seq_codigo_pedido`.

**Por qué una secuencia y no `MAX(codigo)+1`:** la secuencia entrega valores únicos sin bloquear la
tabla y sin sufrir condiciones de carrera cuando dos pedidos se crean a la vez. Es la razón por la
que esta operación no puede resolverse con un contador en Java: dos instancias del backend
generarían el mismo número. La contrapartida aceptada es que los huecos en la secuencia son posibles
tras un *rollback*; el código es un identificador, no un contador de pedidos, y su unicidad la
garantiza además la restricción `UNIQUE` de la tabla.

**Idempotente:** si el pedido ya tiene código asignado lo devuelve sin consumir un valor nuevo de la
secuencia.

### Parámetros

| # | Nombre | Modo | Tipo | Por defecto | Significado |
|---|---|---|---|---|---|
| 1 | `p_id_pedido` | IN | `BIGINT` | — | Pedido al que se asigna el código. **Obligatorio** |

### Retorno

`VARCHAR(20)` — el código asignado, o el ya existente si la rutina se invoca dos veces.

### Excepciones

| Condición | Mensaje | `SQLSTATE` |
|---|---|---|
| `p_id_pedido IS NULL` | `p_id_pedido es obligatorio` | `22004` |
| Pedido inexistente | `El pedido % no existe` | `23503` |

### Objetos implicados

| Objeto | Acceso |
|---|---|
| `pedidos` | Lectura con `FOR UPDATE` **y escritura (`UPDATE codigo_pedido`)** |
| `seq_codigo_pedido` | `nextval()` — secuencia definida en `db/procs/V8__estructuras_para_procedimientos.sql` |
| `uq_pedidos_codigo_pedido` | Restricción `UNIQUE` que respalda la unicidad del código |

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
| `pedidos.codigo_pedido` | Columna | `fn_generar_codigo_pedido` |
| `uq_pedidos_codigo_pedido` | Restricción `UNIQUE` | `fn_generar_codigo_pedido` |
| `seq_codigo_pedido` | Secuencia | `fn_generar_codigo_pedido` |
| 4 índices de apoyo | Índices | Predicados de filtrado y JOIN de las rutinas 1, 3 y 4 |

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
