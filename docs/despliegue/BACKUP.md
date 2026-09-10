# Estrategia de respaldo — Artisync (Entrega Final, v1.0.0)

Cumple el Bloque A.4.2 de la guía de la Entrega Final. Describe frecuencia, destino, retención,
procedimiento de restauración y prueba periódica de restauración documentada.

## Relación con el plan diseñado en la asignatura de Administración de Base de Datos

El equipo diseñó y documentó un plan de respaldos completo (RPO 15 min, RTO 2 horas, política
3-2-1, `pg_basebackup` + archivado continuo de WAL) como entregable de la asignatura de
Administración de Base de Datos: [`Documentacion sobre respaldos.pdf`](../../Documentacion%20sobre%20respaldos.pdf)
(28 de julio de 2026). Ese documento es la referencia teórica y de diseño; **describe la
arquitectura objetivo para un ambiente de producción real en la nube (AWS S3, servidor NFS
externo, streaming replication)**, que hoy no está desplegado — el sistema corre en Docker Compose
local/Azure (ver `docs/despliegue/DEPLOYMENT.md`). Este documento (`BACKUP.md`) describe lo que
está **realmente implementado y verificable** a la fecha de la Entrega Final, y remite al PDF para
el diseño completo que el equipo aplicará al desplegar en el proveedor final elegido.

## Qué está implementado hoy

- **Persistencia de datos:** `postgres` escribe sobre el volumen Docker con nombre
  `pfc_postgres_data` (`artisync/docker-compose.yml`), que sobrevive a `docker compose down`
  (sin `-v`) y a reinicios del host.
- **Respaldo lógico manual:** `pg_dump`/`pg_restore` (formato `custom`, comprimido), el mismo
  mecanismo demostrado en el PDF de la asignatura (Sección III), ejecutable en cualquier momento
  contra el contenedor `postgres` sin detener el servicio:
  ```bash
  docker exec pfc_postgres pg_dump -U "$DB_USER" -Fc artisyncbd > backup-$(date +%Y%m%d-%H%M).dump
  ```
  Sigue siendo válido como procedimiento de emergencia si el backend está caído.
- **Respaldo automatizado desde el panel de administración (REQ-NF-024):** un ADMIN puede disparar
  un respaldo bajo demanda o programarlo de forma recurrente, eligiendo tipo **FULL** o
  **INCREMENTAL**, desde `/admin/respaldos`. Implementado en `RespaldoControlador` +
  `RespaldoServicioImpl`/`RespaldoProgramacionServicioImpl` (disparo), `RespaldoProgramacionScheduler`
  (poller cada 60s que revisa `respaldo_programaciones`) y `RespaldoRetencionScheduler` (purga diaria
  de respaldos vencidos). Ambos tipos corren **dentro del propio proceso backend**, conectando por
  red al host de Postgres con el rol de solo lectura `artisync_backup` — nunca vía `docker exec` al
  contenedor `postgres` (eso requeriría exponer el socket de Docker al contenedor del backend, una
  escalada de privilegios que el proyecto evita a propósito):
  - **FULL:** invoca el binario `pg_dump` (`-Fc`) vía `ProcessBuilder` (`PgDumpEjecutor`).
  - **INCREMENTAL:** *no* es "un `pg_dump` filtrado" — `pg_dump` no admite filtrar filas por
    `WHERE`. En su lugar, `IncrementalRespaldoExportador` corre `COPY (SELECT * FROM tabla WHERE
    columna_fecha > corte) TO STDOUT` por cada tabla en alcance, vía el `CopyManager` del driver
    JDBC de PostgreSQL, y empaqueta el resultado en un `.zip` con un manifiesto. El corte de cada
    incremental es la fecha del último respaldo de su cadena (el incremental anterior si existe, si
    no el FULL base), para que cada incremental capture solo lo cambiado desde el respaldo previo.

    **Limitación a tener presente:** este incremental **no es un mecanismo de recuperación ante
    desastres en el sentido estricto de PostgreSQL** (no es WAL/`pg_basebackup`, pospuesto según la
    sección anterior). No reconstruye el estado completo de la base sin su FULL base; no captura de
    forma confiable las filas `DELETE`d; y las tablas sin una columna de fecha de modificación
    reconocida (`respaldo.tablas.columna-fecha` en `application.properties`) se exportan
    **completas** en cada corrida incremental, no se omiten. Restaurar a un punto en el tiempo
    requiere el FULL más **todos** los incrementales de su cadena aplicados en orden, no solo el
    último — si se pierde uno intermedio, los posteriores quedan inútiles.
  - **Retención:** un FULL no se elimina mientras existan incrementales `COMPLETADO` que dependan
    de él, aunque su propia retención ya haya vencido — la retención configurada de un FULL es un
    mínimo, no un máximo estricto.
  - **Almacenamiento:** local únicamente por ahora, bajo `respaldo.ruta-base`
    (`/var/artisync/respaldos` por defecto), respaldado por el volumen Docker nombrado
    `pfc_respaldos` — mismo criterio que `documentos.ruta-base`/`pfc_documentos_verificacion`, sin
    abstracción de proveedor de nube todavía.
  - **Restauración:** deliberadamente **no** es una acción de la aplicación — sigue siendo el
    procedimiento manual de `pg_restore` documentado más abajo, dado su carácter destructivo/
    irreversible sobre la base de datos en uso.
- **Documentos (Blob/almacenamiento local):** persistidos en el volumen `pfc_documentos`
  (renombrado desde `pfc_documentos_verificacion` — ver plan de mejora de almacenamiento) o en
  Azure Blob Storage cuando `DOCUMENTOS_PROVEEDOR=azure`; Azure Blob ya replica por diseño del
  proveedor (redundancia geográfica según el nivel de servicio contratado).

## Frecuencia y retención (compromiso mínimo para la Entrega Final)

Cumpliendo el mínimo exigido por la guía ("como mínimo, se conserva un respaldo diario de la base
de datos durante los treinta días posteriores a la defensa"). Desde que existe el mecanismo
automatizado, frecuencia y retención son **configurables por programación** (`respaldo_programaciones`:
`expresion_cron` + `retencion_dias`) en vez de un valor fijo en código — la tabla siguiente es el
compromiso mínimo por defecto que el equipo declara, no un límite del sistema:

| Elemento | Frecuencia | Destino | Retención |
|---|---|---|---|
| Respaldo automatizado FULL (`pg_dump -Fc`, programación por defecto) | Diario | Volumen local `pfc_respaldos` (`/var/artisync/respaldos`) | 30 días desde la fecha de defensa oral en adelante |
| Respaldo automatizado INCREMENTAL (`COPY` diferencial, programación opcional) | Según se programe (p. ej. cada hora) | Volumen local `pfc_respaldos` | Igual que su FULL base; nunca se purga un FULL con incrementales `COMPLETADO` vivos |
| Respaldo lógico manual (`docker exec ... pg_dump`) | Ad hoc (procedimiento de emergencia) | Fuera del contenedor, a discreción de quien lo ejecuta | Sin política automatizada — a criterio de quien lo genera |
| Volumen `pfc_postgres_data` | Continuo (persistencia normal del contenedor) | Host Docker | Mientras el ambiente esté activo |

El plan completo del PDF de la asignatura (full semanal + WAL diferencial diario + archivado
continuo, retención 4 semanas GFS para el full y 7/14 días para los diferenciales) es el objetivo
a implementar cuando el ambiente de producción final quede desplegado — ver la matriz de
planificación semanal en la Sección II de ese documento.

## Procedimiento de restauración

```bash
# 1. Copiar el dump al contenedor (o montar el volumen donde se generó)
docker cp backup-YYYYMMDD-HHMM.dump pfc_postgres:/tmp/backup.dump

# 2. Restaurar contra una base limpia (o la misma tras vaciar el esquema, ver RUNBOOK.md)
docker exec pfc_postgres pg_restore -U "$DB_USER" -d artisyncbd --clean --if-exists /tmp/backup.dump

# 3. Validar con una consulta de conteo contra el número esperado antes del respaldo
docker exec pfc_postgres psql -U "$DB_USER" -d artisyncbd -c "SELECT count(*) FROM pedidos;"
```

Procedimiento equivalente, paso a paso con capturas de pgAdmin, en la Sección III del
[PDF de la asignatura](../../Documentacion%20sobre%20respaldos.pdf).

## Prueba periódica de restauración

Se recomienda ejecutar el procedimiento anterior contra un entorno de staging (no producción) de
forma mensual, siguiendo el simulacro ya diseñado en la Sección II-A del PDF de la asignatura
(ventana semanal de simulacro, sábado 10:00 AM). **Estado a la fecha de este documento:** el
simulacro no se ha ejecutado todavía sobre el ambiente real de la Entrega Final — queda declarado
como acción pendiente. Con el mecanismo automatizado ya implementado, ese simulacro debe ejecutarse
contra un respaldo generado por él (no contra los dumps manuales antiguos) para que REQ-NF-024 pase
de "implementado" a "verificado" en `docs/requisitos/SRS.md`.
