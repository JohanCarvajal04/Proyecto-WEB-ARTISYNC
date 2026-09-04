# Despliegue — Artisync (Entrega Final, v1.0.0)

Cumple el Bloque A.4.2 de la guía de la Entrega Final. Describe el proveedor, los recursos
consumidos, la topología de red simplificada, las variables de entorno de producción (sin valores
sensibles) y el procedimiento para reproducir el despliegue.

## Estado a la fecha de este documento (2026-09-04)

El sistema **está desplegado en Render** con dominio público y HTTPS (requisito A.4.1 de la guía),
usando el Blueprint definido en `render.yaml` (raíz del repositorio):

- Frontend: <https://artisync-frontend.onrender.com>
- Backend: <https://artisync-backend.onrender.com> (health check en `/actuator/health`)

La topología para Azure (`artisync/docker-compose.azure.yml`, `ADR-005`) sigue disponible y
documentada más abajo como alternativa equivalente, por si el equipo necesita migrar de proveedor.

## Proveedor

El repositorio soporta tres topologías, seleccionables por archivo de compose o por Blueprint:

- **Render** (`render.yaml`, activa en producción): cuatro servicios gestionados por un único
  Blueprint — `artisync-db` (PostgreSQL managed, plan free), `artisync-redis` (plan free),
  `artisync-backend` y `artisync-frontend` (ambos `runtime: docker`, construidos desde
  `artisync/Backend/Dockerfile` y `artisync/Frontend/Dockerfile.render` respectivamente).
- **Local / desarrollo** (`artisync/docker-compose.yml`): los cuatro servicios (`postgres`,
  `redis`, `backend`, `frontend`) corren en la misma máquina o VM, sin dependencias externas de
  nube.
- **Azure** (`artisync/docker-compose.azure.yml`): backend apunta a una instancia de **Azure
  Database for PostgreSQL** (variables `DB_URL`/`DB_USER`/`DB_PASSWORD` externas, no un contenedor
  local de Postgres) y opcionalmente a **Azure Blob Storage** para documentos
  (`DOCUMENTOS_PROVEEDOR=azure`, con **Azurite** como emulador local para desarrollo — perfil
  `azure` de `docker-compose.yml`, ver `AlmacenamientoAzureIntegracionTest.java`).

## Recursos consumidos

### Render (producción, plan free en los cuatro servicios)

| Servicio | CPU | RAM | Disco |
|---|---|---|---|
| `artisync-db` (PostgreSQL 18, managed) | compartida | 256 MB | 1 GB — **expira el 2026-10-01** si no se actualiza a un plan pago (límite del plan free de Render) |
| `artisync-redis` (Valkey 8, managed) | compartida | 25 MB | Sin persistencia (cache) |
| `artisync-backend` (Spring Boot, JRE 21, Docker) | compartida (~0.1 vCPU) | 512 MB | Sin estado — `DOCUMENTOS_PROVEEDOR=local` guarda en filesystem **efímero**, se pierde en cada redeploy/restart (ver nota abajo) |
| `artisync-frontend` (nginx + build Angular, Docker) | compartida (~0.1 vCPU) | 512 MB | Sin estado |

La CPU compartida del plan free es agresivamente limitada: el arranque de `artisync-backend` sin
ajustes tardaba más de los 10 minutos que da Render para detectar el puerto abierto, por la
inicialización eager de ~50+ beans/repositorios JPA. Se resolvió con
`spring.main.lazy-initialization=true` y flags de JVM (`-XX:TieredStopAtLevel=1
-XX:+UseSerialGC -XX:MaxRAMPercentage=75`, ver `artisync/Backend/Dockerfile`), que bajaron el
arranque a ~145 s.

**Almacenamiento de documentos:** con `DOCUMENTOS_PROVEEDOR=local` (default) los archivos subidos
(cédulas, títulos de verificación) se pierden en cada redeploy porque el filesystem de los web
services de Render no es persistente. Para producción real hay que migrar a
`DOCUMENTOS_PROVEEDOR=azure` (ya soportado por el código) o equivalente con almacenamiento
durable.

### Perfil local (referencia de dimensionamiento, sin límites de proveedor)

| Servicio | CPU (estimado) | RAM (estimado) | Disco |
|---|---|---|---|
| `postgres` (16, imagen anclada por digest) | 0.5 vCPU | 512 MB | Volumen `pfc_postgres_data` (crece con datos) |
| `redis` (7-alpine) | 0.1 vCPU | 64 MB | Sin persistencia por diseño (cache) |
| `backend` (Spring Boot, JRE 21) | 1 vCPU | 512 MB–1 GB (JVM) | Sin estado (logs a stdout) |
| `frontend` (nginx + build Angular) | 0.1 vCPU | 64 MB | Sin estado |

## Topología de red simplificada

```
Internet
   │  HTTPS (443)
   ▼
[artisync-frontend: nginx]  ── sirve Angular build de produccion
   │  proxy interno /api, /actuator, /ws
   │  (nginx.render.conf.template, via BACKEND_INTERNAL_URL)
   ▼
[artisync-backend: Spring Boot :10000 (=$PORT de Render)]
   │                    │
   ▼                    ▼
[artisync-db :5432]  [artisync-redis :6379]
(red interna de Render, ambos con acceso solo desde servicios
 del mismo proyecto — artisync-redis con ipAllowList: [])
```

Todo el tráfico externo entra por `artisync-frontend`; el backend no es alcanzable directamente
salvo por su propia URL pública de Render (usada para health checks y como origen permitido de
CORS del propio frontend). Cada servicio Docker de Render escucha en el puerto que indica la
variable `PORT` que Render inyecta — el backend lo lee vía `server.port=${PORT:8080}` y el
frontend vía `ENV PORT=8080` + `envsubst` sobre `nginx.render.conf.template`.

## Variables de entorno de producción (sin valores sensibles)

Ver `artisync/.env.example` para la lista completa con placeholders, y `render.yaml` para cuáles
son automáticas (`fromDatabase`/`fromService`/`generateValue`) y cuáles hay que completar a mano
en el dashboard de Render (`sync: false`, valor vacío por defecto):

| Variable | Servicio | Origen en Render |
|---|---|---|
| `DB_URL` | `artisync-backend` | Manual (`sync: false`) — Internal/External Database URL de `artisync-db`, con esquema `jdbc:postgresql://` y **sin** usuario/password embebidos en la URL (esos van aparte en `DB_USER`/`DB_PASSWORD`) |
| `DB_USER` / `DB_PASSWORD` | `artisync-backend` | Automático (`fromDatabase`) — cuenta admin de `artisync-db`, usada solo por Flyway para DDL |
| `DB_APP_USER` / `DB_APP_PASSWORD` | `artisync-backend` | `DB_APP_USER` fijo (`artisync_app`); `DB_APP_PASSWORD` manual — cuenta de privilegios mínimos para Hibernate en runtime, creada corriendo `artisync/db/seed_privilegios_render.sql` a mano contra `artisync-db` (Render no ejecuta `docker-entrypoint-initdb.d`) |
| `REDIS_HOST` / `REDIS_PORT` | `artisync-backend` | Automático (`fromService`) |
| `JWT_SECRET` | `artisync-backend` | Automático (`generateValue: true`) |
| `FRONTEND_URL` | `artisync-backend` | Manual — URL pública de `artisync-frontend`; se reusa también como origen permitido de CORS (`app.cors.allowed-origins`, ver `application.properties`) |
| `MAIL_USER` / `MAIL_PASSWORD` | `artisync-backend` | Manual — cuenta SMTP de producción |
| `PAYPAL_CLIENT_ID` / `PAYPAL_CLIENT_SECRET` / `PAYPAL_WEBHOOK_ID` | `artisync-backend` | Manual — credenciales de la app en developer.paypal.com |
| `APP_COOKIE_SECURE` | `artisync-backend` | Fijo `"true"` (cookies con `Secure`, hay HTTPS) |
| `BACKEND_INTERNAL_URL` | `artisync-frontend` | Manual — URL pública de `artisync-backend`, usada por `nginx.render.conf.template` para el proxy `/api`, `/actuator`, `/ws` |

Las credenciales reales nunca se versionan; se completan directamente en el dashboard de Render
(Environment de cada servicio) o, para desarrollo local, vía `artisync/.env` (gitignored).

## Procedimiento paso a paso para reproducir el despliegue en Render

1. En Render: **New +** → **Blueprint** → conectar el repositorio → debe detectar `render.yaml` en
   la raíz y crear los cuatro servicios (`artisync-db`, `artisync-redis`, `artisync-backend`,
   `artisync-frontend`).
2. En `artisync-backend` → **Environment**, completar las variables marcadas `sync: false` en la
   tabla de arriba (`DB_URL`, `DB_APP_PASSWORD`, `MAIL_USER`/`MAIL_PASSWORD`,
   `PAYPAL_CLIENT_ID`/`PAYPAL_CLIENT_SECRET`/`PAYPAL_WEBHOOK_ID`). `FRONTEND_URL` se completa
   después del paso 5.
3. Ejecutar `artisync/db/seed_privilegios_render.sql` una sola vez contra `artisync-db` (pestaña
   **Connect** → External Database URL + `psql`), reemplazando el placeholder de password por el
   mismo valor de `DB_APP_PASSWORD`. Sin este paso, Hibernate falla en runtime con
   `password authentication failed for user "artisync_app"` aunque Flyway sí conecte.
4. Esperar el deploy de `artisync-backend` y confirmar `GET https://<backend>.onrender.com/actuator/health` → `UP`.
5. En `artisync-frontend` → **Environment**, completar `BACKEND_INTERNAL_URL` con la URL pública
   del backend (sin barra final). Esperar el redeploy.
6. Volver a `artisync-backend` → **Environment** y completar `FRONTEND_URL` con la URL pública del
   frontend. Esto también habilita CORS para ese origen (`app.cors.allowed-origins` lo reusa) —
   sin este paso, el login y cualquier llamada del frontend fallan con 403 aunque las credenciales
   sean correctas.
7. Verificar el login con el usuario demo (`admin@artisync.com` / `ArtisyncAdmin2026!` — ver
   `artisync/db/seed.sql`). Si la migración `V1__schema_inicial.sql` sembró el usuario admin con
   un hash de password distinto al documentado (puede pasar si `V1` corrió antes que existiera
   `seed.sql`), actualizar el hash a mano:
   `UPDATE usuarios SET contrasena_hash = '<hash de seed.sql>' WHERE correo = 'admin@artisync.com';`
8. Publicar las URLs en el `README.md` (sección de arranque) y en la portada del documento
   académico final, junto con las credenciales del usuario demo.

Para un proveedor distinto a Render (VPS, Azure, contenedores genéricos), usar en su lugar:

1. Clonar el repositorio y copiar `artisync/.env.example` a `artisync/.env`.
2. Completar las variables marcadas `<...>` en `.env` con los valores reales del proveedor
   elegido (base de datos, JWT, correo, y `DOCUMENTOS_PROVEEDOR` según corresponda).
3. Si se usa un proveedor cloud (Azure u otro): `docker compose -f artisync/docker-compose.yml -f artisync/docker-compose.azure.yml --env-file artisync/.env up -d --build`.
   Si se usa un VPS con Docker: `make up` (equivalente al perfil local, con Postgres/Redis en el
   mismo host).
4. Configurar un proxy inverso (Caddy, nginx, o el balanceador del proveedor) delante del
   contenedor `frontend` con un certificado TLS válido (ej. Let's Encrypt vía Caddy automático).
5. Verificar `GET https://<dominio>/actuator/health` → debe responder `UP` en todos los
   componentes registrados (base de datos, Redis).

Ver también `docs/despliegue/RUNBOOK.md` (operación día a día) y `docs/despliegue/BACKUP.md`
(estrategia de respaldo).
