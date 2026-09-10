# Reporte de Demostración de Caída y Recuperación — REQ-NF-009

- Fecha: 2026-09-10 (UTC-05:00; timestamps crudos en UTC)
- Commit base: `932632d` (rama `feat/ia-verificacion-asistida`)
- Entorno: `artisync/docker-compose.yml` local, `docker compose` v5.3.1
- Motor Docker: `29.6.2 linux/amd64`, `Docker Desktop`, runtime `runc`, kernel
  `6.18.33.1-microsoft-standard-WSL2` (Docker Desktop para Windows, backend WSL2)
- Método de verificación declarado en el SRS: demostración (`ps aux`, healthcheck Docker)

## Resultado: **no cumple**

Los 4 servicios reales de `docker-compose.yml` declaran `restart: unless-stopped`
(`pfc_backend`, `pfc_postgres`, `pfc_redis` con healthcheck propio; `pfc_frontend` y
`pfc_azurite` sin healthcheck propio). Se simuló una caída abrupta (`docker kill`,
señal `SIGKILL`, no `docker stop`, para no activar la bandera "detenido manualmente"
que Docker respeta correctamente al no reiniciar) sobre dos servicios distintos con
healthcheck real, y en **ninguno de los dos casos el contenedor se reinició
automáticamente** dentro de la ventana observada.

### Prueba 1 — `pfc_backend`

```
$ docker kill pfc_backend
```

- Kill: `2026-09-10T10:26:24.090Z`
- Polling cada ~3.7s con `docker inspect --format='{{.State.Status}} health={{...}}'`
- Estado observado durante **2 minutos 28 segundos** (42 muestras): `exited health=unhealthy`,
  ininterrumpidamente. `docker events` (ver [`docker-events-backend-20260910.txt`](docker-events-backend-20260910.txt))
  solo registra `kill` → `die (exitCode=137)`; ningún evento `start`/`restart` posterior.
- `docker inspect pfc_backend --format='{{.RestartCount}}'` inmediatamente después del
  incidente: **0**.
- Transcripción completa: [`docker-inspect-polling-backend-20260910.txt`](docker-inspect-polling-backend-20260910.txt).
- Recuperado manualmente con `docker start pfc_backend` (vuelve a `healthy` en ~20s).

### Prueba 2 — `pfc_postgres`

```
$ docker kill pfc_postgres
```

- Kill: `2026-09-10T10:30:47.068Z`
- Estado observado durante **71 segundos** (20 muestras): `exited health=unhealthy
  restartcount=0`, ininterrumpidamente.
- Transcripción completa: [`docker-inspect-polling-postgres-20260910.txt`](docker-inspect-polling-postgres-20260910.txt).
- Recuperado manualmente con `docker start pfc_postgres` (vuelve a `healthy` en ~15s).

## Diagnóstico

La política está correctamente declarada tanto en `docker-compose.yml` como en el
contenedor real (`docker inspect ... HostConfig.RestartPolicy.Name` = `unless-stopped`,
`MaximumRetryCount=0` = sin límite), y `docker kill` no activa la bandera de "detenido
manualmente" que sí respetaría `docker stop`. Aun así, `dockerd` no disparó el reinicio
automático en ninguna de las dos pruebas, en dos servicios distintos, lo que descarta que
sea un problema puntual de un solo healthcheck. Es consistente con reportes conocidos de
`restart: unless-stopped` no disparándose de forma fiable en Docker Desktop sobre
WSL2 ante un `SIGKILL` directo del contenedor (a diferencia de un reinicio completo del
propio Docker Desktop, donde sí se ha observado que los contenedores `unless-stopped` se
recuperan). No se investigó más a fondo la causa raíz en esta sesión.

**Nota importante de alcance:** esta política de `docker-compose.yml` solo gobierna un
despliegue self-hosted vía Docker Compose. El despliegue real del proyecto en Render
(`render.yaml`) **no usa `docker-compose` ni esta política de reinicio** — Render
administra el ciclo de vida y el reinicio ante fallo de sus propios servicios con su
propio supervisor, independiente de este archivo. Es decir, este resultado negativo
describe el comportamiento de la política declarada en el repositorio bajo Docker
Desktop local; no permite concluir nada sobre la resiliencia real de la producción en
Render, que tendría que demostrarse por otro medio (p. ej., provocando el reinicio de un
servicio desde el dashboard de Render y observando su healthcheck).

## Umbral esperado vs. resultado

**Umbral esperado:** el contenedor se reinicia automáticamente y vuelve a `healthy` tras
una caída, sin intervención manual — **Resultado: no cumple** en el entorno probado
(Docker Desktop / WSL2 local). No se sube el estado a `verificado`; se documenta como
hallazgo activo, no como evidencia pendiente.
