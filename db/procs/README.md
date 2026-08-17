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

Las siete primeras filas son las rutinas originales de la Tercera Entrega (módulos
catálogo/pedido/legal/social). Las siete siguientes son la ampliación descrita en
[`docs/adr/adr-006-estrategia-acceso-datos.md`](../../docs/adr/adr-006-estrategia-acceso-datos.md#ampliación),
con prioridad en el módulo de seguridad, más `fn_seleccionar_ganadores_sorteo` (el candidato que el
propio ADR-006 ya identificaba por nombre como pendiente) y `fn_registrar_infraccion`.

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
