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
- **Documentos (Blob/almacenamiento local):** persistidos en el volumen `pfc_documentos`
  (renombrado desde `pfc_documentos_verificacion` — ver plan de mejora de almacenamiento) o en
  Azure Blob Storage cuando `DOCUMENTOS_PROVEEDOR=azure`; Azure Blob ya replica por diseño del
  proveedor (redundancia geográfica según el nivel de servicio contratado).

## Frecuencia y retención (compromiso mínimo para la Entrega Final)

Cumpliendo el mínimo exigido por la guía ("como mínimo, se conserva un respaldo diario de la base
de datos durante los treinta días posteriores a la defensa"):

| Elemento | Frecuencia | Destino | Retención |
|---|---|---|---|
| Respaldo lógico completo (`pg_dump -Fc`) | Diario | Almacenamiento fuera del contenedor (disco del host o almacenamiento del proveedor cloud) | 30 días desde la fecha de defensa oral en adelante |
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
como acción pendiente en `docs/observaciones/OBSERVACIONES.md` en vez de afirmarse sin evidencia.
