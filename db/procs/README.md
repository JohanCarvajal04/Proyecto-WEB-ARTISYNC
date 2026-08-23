# `db/procs/` — Procedimientos almacenados y funciones SQL

Ubicación **canónica** de la lógica de acceso a datos que no es un CRUD elemental, según el
apartado A.2.1 de la Guía de la Entrega Final y la decisión registrada en
[`docs/adr/adr-006-estrategia-acceso-datos.md`](../../docs/adr/adr-006-estrategia-acceso-datos.md).

## Contrato de este directorio

1. **Un archivo por rutina.** El nombre del archivo coincide con el nombre de la rutina.
2. **Idempotencia.** Toda rutina se declara con `CREATE OR REPLACE`, de modo que aplicar el
   directorio dos veces sobre la misma base es inocuo.
3. **Cero SQL dinámico.** Ninguna rutina construye sentencias por concatenación
   (`EXECUTE IMMEDIATE`, `EXECUTE format(...)`, `||` sobre variables dentro de un `EXECUTE`).
   `scripts/audit-sql-dynamic.sh` lo verifica en cada ejecución de CI y falla el build si aparece.
   Es la defensa que exige la regla transversal 7 de la guía y el *SQL Injection Prevention Cheat
   Sheet* de OWASP.
4. **Solo parámetros nombrados y tipados.** Toda entrada externa llega como parámetro formal de la
   rutina; nunca interpolada en el texto de la consulta.

## Aplicación sobre la base de datos

Los archivos de este directorio **no se aplican solos**. `scripts/sync-procs.sh` los concatena en
la migración *repeatable* de Flyway:

    artisync/Backend/src/main/resources/db/migration/R__procedimientos.sql

Flyway reaplica las migraciones repetibles cada vez que su checksum cambia, por lo que editar un
archivo aquí y ejecutar `make sync-procs` basta para propagar el cambio. CI ejecuta
`scripts/sync-procs.sh --check` y falla si el archivo generado no coincide con el commiteado,
de modo que las dos copias no pueden divergir en silencio.

## Catálogo

La documentación funcional completa —categoría, propósito, parámetros con tipo y modo, valor
devuelto y tablas afectadas— está en
[`docs/basedatos/CATALOGO-SP.md`](../../docs/basedatos/CATALOGO-SP.md).

| Archivo | Categoría funcional (A.2.2 Entrega 3) | Requisito |
| --- | --- | --- |
| `fn_catalogo_filtrado.sql` | Consultas multi-tabla | REQ-F-013 |
| `fn_calificacion_promedio_creador.sql` | Cálculos agregados | REQ-F-009 |
| `fn_reporte_comisiones_creador.sql` | Reportes | REQ-NF-013 |
| `fn_cerrar_pedidos_vencidos.sql` | Actualizaciones masivas | REQ-F-019 |
| `fn_liberar_fondos_escrow.sql` | Validaciones cruzadas | REQ-F-021 |
| `fn_generar_codigo_pedido.sql` | Generación de códigos secuenciales | REQ-F-018 |
| `fn_registrar_usuario.sql` | Validaciones cruzadas + inserción multi-tabla | REQ-F-001 |
| `fn_resolver_estado_login.sql` | Consultas multi-tabla | REQ-F-002 |
| `fn_sincronizar_permisos_rol.sql` | Actualizaciones masivas | REQ-F-003 |
| `fn_eliminar_rol.sql` | Validaciones cruzadas | REQ-F-004 |
| `fn_restablecer_contrasena.sql` | Validaciones cruzadas + escritura multi-tabla | REQ-F-005 |
| `fn_seleccionar_ganadores_sorteo.sql` | Selección aleatoria + actualización masiva | REQ-F-023 |
| `fn_registrar_infraccion.sql` | Cálculos agregados + validación cruzada | REQ-F-015 |
| `fn_consumir_codigo_respaldo_2fa.sql` | Validaciones cruzadas (Fase 1 concurrencia) | actualización perdida |
| `fn_sincronizar_roles_usuario.sql` | Actualizaciones masivas (Fase 1 concurrencia) | lectura fantasma |
| `fn_revocar_sesiones_usuario.sql` | Actualizaciones masivas (Fase 1 concurrencia) | lectura no repetible |
| `fn_cambiar_estado_cuenta.sql` | Validaciones cruzadas (Fase 1 concurrencia) | actualización perdida |
| `fn_permisos_efectivos_usuario.sql` | Consultas multi-tabla (Fase 2 rendimiento) | N+1 en cada petición autenticada |
| `fn_configurar_2fa.sql` | Validaciones cruzadas + escritura multi-tabla (Fase 3 concurrencia) | estado a medias |
| `fn_desactivar_2fa.sql` | Validaciones cruzadas + escritura multi-tabla (Fase 3 concurrencia) | código duplicado |
| `fn_solicitar_recuperacion.sql` | Validaciones cruzadas + escritura multi-tabla (Fase 3 concurrencia) | acumulación no controlada |
| `fn_cambiar_contrasena.sql` | Validaciones cruzadas (Fase 3 concurrencia) | actualización perdida |
| `fn_crear_usuario_admin.sql` | Validaciones cruzadas + inserción multi-tabla (Fase 3 concurrencia) | lectura fantasma |
| `fn_crear_rol.sql` | Validaciones cruzadas (Fase 3 concurrencia) | lectura fantasma |
| `fn_guardar_pais.sql` | Validaciones cruzadas (Fase 3 concurrencia) | lectura fantasma |
| `sp_purgar_datos_seguridad.sql` | Actualizaciones masivas (Fase 4 mantenimiento) | crecimiento sin límite |

Las siete primeras filas son las rutinas originales de la Tercera Entrega (módulos
catálogo/pedido/legal/social). Las siete siguientes son la ampliación descrita en
[`docs/adr/adr-006-estrategia-acceso-datos.md`](../../docs/adr/adr-006-estrategia-acceso-datos.md#ampliación),
con prioridad en el módulo de seguridad, más `fn_seleccionar_ganadores_sorteo` (el candidato que el
propio ADR-006 ya identificaba por nombre como pendiente) y `fn_registrar_infraccion`.

Las cuatro filas siguientes son la Fase 1 de
[`docs/basedatos/PLAN-CONCURRENCIA-SP.md`](../docs/basedatos/PLAN-CONCURRENCIA-SP.md): no responden
a una categoría A.2.2 de rendimiento sino a una anomalía de concurrencia concreta bajo `READ
COMMITTED` (el nivel de aislamiento efectivo del proyecto), que ese documento detalla junto con la
técnica usada en cada una (`UPDATE ... WHERE <predicado>`, `SELECT ... FOR UPDATE`,
`DELETE ... RETURNING`). Dependen del esquema que crea
`artisync/Backend/src/main/resources/db/migration/V17__concurrencia_seguridad.sql`
(`uq_usuario_rol`, `uq_codigo_respaldo_usuario_hash` y los índices de clave foránea que la ruta de
autenticación necesitaba).

La fila siguiente es la Fase 2 (rendimiento) del mismo plan: elimina el N+1 de
`CustomUserDetailsService.loadUserByUsername`, invocado en cada petición autenticada. El otro
hallazgo de rendimiento de esa fase — el listado paginado de administración de usuarios — se
resolvió **sin** una rutina almacenada nueva, por un conflicto real con el `sortBy` arbitrario que
ya soporta ese endpoint (ver la nota en `docs/basedatos/CATALOGO-SP.md`, sección 16).

Las siete filas siguientes son la Fase 3 del mismo plan: cierran las anomalías A3–A9 (lectura
fantasma en `createUser`/`createRole`/`updatePais`, estado a medias en `setup2Fa`, acumulación no
controlada en `forgotPassword`, actualización perdida en `changePassword`) capturando
`unique_violation` con `EXCEPTION WHEN` en vez de comprobaciones "existe?" no atómicas, o con
`UPDATE ... WHERE <predicado>` como compare-and-swap. `fn_crear_usuario_admin` y `fn_crear_rol`
**componen** con rutinas de fases anteriores (`fn_sincronizar_roles_usuario`,
`fn_sincronizar_permisos_rol`) en vez de reimplementar esa lógica.

La última fila es la Fase 4 (mantenimiento) del plan, y la **única `PROCEDURE`** —no `FUNCTION`—
de todo `db/procs/`: `sp_purgar_datos_seguridad` purga por lotes (`COMMIT` real por lote,
`FOR UPDATE SKIP LOCKED`) las tres tablas que las fases anteriores dejaban crecer sin límite
(A10). Se invoca desde `SeguridadPurgaScheduler` (`@Scheduled` diario) vía `JdbcTemplate` bajo
`Propagation.NOT_SUPPORTED` — un `PROCEDURE` con `COMMIT` interno no puede correr dentro de una
transacción Spring ya abierta. A diferencia de las `FUNCTION`, no queda cubierta por el
`ALTER DEFAULT PRIVILEGES ... GRANT EXECUTE ON FUNCTIONS` de `seed_privilegios.sh`: lleva su
propio `GRANT EXECUTE ON PROCEDURE` guardado al final del archivo, mismo patrón que
`sp_registrar_decision_verificacion` (el único otro `PROCEDURE` del proyecto).

Aparte del catálogo de `db/procs/`, el módulo de verificación asistida por IA ya tenía, desde antes
de esta ampliación, un par de rutinas conectadas end-to-end:
`fn_listar_cola_verificacion` (FUNCTION) y `sp_registrar_decision_verificacion` (PROCEDURE), ambas
definidas en `artisync/Backend/src/main/resources/db/migration/V7__verificacion_asistida_ia.sql` e
invocadas desde `CertificadoIaRepository`. No viven en `db/procs/` (nacieron antes de que existiera
este directorio canónico) y por eso no aparecen en la tabla anterior, pero están documentadas en
`docs/basedatos/CATALOGO-SP.md`.

## Por qué `fn_` y no `sp_`

Las seis rutinas se declaran como **funciones** de PostgreSQL (`CREATE FUNCTION`), no como
procedimientos (`CREATE PROCEDURE`), incluidas las dos que escriben. El motivo es de
interoperabilidad con el mecanismo de invocación que la guía exige: el driver JDBC de PostgreSQL
traduce la sintaxis de escape `{call rutina(?)}` que genera Hibernate a `SELECT * FROM rutina(?)`
bajo su modo por defecto (`escapeSyntaxCallMode=select`), lo que resuelve funciones pero no
procedimientos. Declararlas como funciones permite invocarlas con `@Procedure` de Spring Data JPA
sin alterar la cadena de conexión ni recurrir a `createNativeQuery`, que la guía prohíbe.

El apartado A.2.1 admite explícitamente ambas formas ("al menos seis procedimientos almacenados
**o funciones SQL**"). La decisión y sus alternativas quedan registradas en el ADR-006.
