# Despliegue en Render — ARTISYNC, paso a paso

Instancia concreta del procedimiento genérico descrito en
[`DEPLOYMENT.md`](DEPLOYMENT.md) para el proveedor **Render** (opción "proveedor de
contenedores gratuito" del Bloque A.4.2 de la guía). Usa el Blueprint
[`render.yaml`](../../render.yaml) versionado en la raíz del repositorio.

## 0. Antes de empezar — seguridad

> [!CAUTION]
> Si vas a retomar la rama `feat/despliegue-render`: su `artisync/.env.example`
> tiene credenciales **reales** committeadas (contraseña de la cuenta admin de
> Azure PostgreSQL y un `JWT_SECRET`), y esa rama ya está empujada a `origin`.
> Antes de continuar: **rota la contraseña de esa cuenta en Azure y genera un
> `JWT_SECRET` nuevo** (`openssl rand -hex 32`). No reuses esos valores en el
> despliegue de Render. Esta guía parte de `main`, cuyo `.env.example` sí tiene
> solo placeholders.

## 1. Arquitectura del despliegue

```
Internet
   │  HTTPS (Render provee el certificado en *.onrender.com)
   ▼
[artisync-frontend] Web Service (Docker: nginx + build Angular)
   │  proxy interno /api/* y /ws/*
   ▼
[artisync-backend] Web Service (Docker: Spring Boot :8080)
   │                              │
   ▼                              ▼
Azure Database for PostgreSQL   [artisync-redis] Key Value de Render
(externo, ya provisionado)      (red interna de Render)
```

Tres servicios de Render, definidos en `render.yaml`:

| Servicio | Tipo Render | Rol |
|---|---|---|
| `artisync-backend` | `web` (Docker) | API Spring Boot |
| `artisync-frontend` | `web` (Docker) | Angular + nginx, único punto público real de tráfico de usuario |
| `artisync-redis` | `keyvalue` | Caché de catálogo + blacklist de JWT |

La base de datos **no** se crea en Render: sigue siendo la instancia de Azure
PostgreSQL ya provisionada (mismo patrón que `artisync/docker-compose.azure.yml`).
Motivo: el Postgres gratuito de Render tiene 1&nbsp;GB fijo y **expira a los 30
días** — no sirve para una base de datos que debe persistir.

### Por qué `artisync-backend` es `web` y no `pserv`

En el diseño local (`docker-compose.yml`), el backend nunca publica el puerto
8080 al host: solo el frontend puede alcanzarlo. El equivalente en Render sería
un *Private Service* (`type: pserv`) — pero **los Private Services no están
disponibles en el plan free de Render**. Con el plan free, el backend queda
como `web`, lo que le asigna una URL pública propia (`artisync-backend-xxxx.onrender.com`),
además de ser alcanzable por red interna desde el frontend.

Esto es una concesión real de seguridad frente al diseño original, documentada
aquí con la misma honestidad que el resto del proyecto (ver
`docs/observaciones/OBSERVACIONES.md`). Mitigación aplicada:

- `APP_COOKIE_SECURE=true` y CORS siguen activos (`SecurityConfig`), así que la
  API pública sigue exigiendo autenticación igual que en local.
- Swagger UI (`/api/swagger-ui.html`) queda accesible en esa URL pública. Si no
  quieres exponerlo, dalo de baja para producción o dile a quien evalúe cuál es
  la URL exacta y espera a que termine.
- Si más adelante se paga un plan Starter o superior, cambiar `type: web` por
  `type: pserv` en `render.yaml` para `artisync-backend` recupera el
  aislamiento original (ese plan sí soporta Private Services).

## 2. Prerrequisitos

- Cuenta en [render.com](https://render.com) (el plan free alcanza para esta guía).
- El repositorio en GitHub, con acceso de Render a él (se autoriza en el paso 4).
- La instancia de Azure PostgreSQL ya provisionada, con:
  - la cuenta admin (`DB_USER`/`DB_PASSWORD`) — rotada si venías de la sección 0;
  - la cuenta de privilegios mínimos `artisync_app` (`DB_APP_USER`/`DB_APP_PASSWORD`),
    creada con `db/seed_privilegios.sh` — ver `docs/basedatos/`.
- Una cuenta SMTP para envío de correo (Gmail con contraseña de aplicación, u
  otro proveedor).
- `openssl` disponible localmente (o cualquier generador de 32 bytes en hex)
  para el `JWT_SECRET`.
- Opcional según los módulos que quieras activar: credenciales de PayPal
  Sandbox (`developer.paypal.com`), API key de Gemini o NVIDIA (verificación
  IA), connection string de Azure Blob Storage (documentos de verificación).

## 3. Verificar que los archivos del Blueprint están en el repo

Ya deben existir en `main` (creados junto con esta guía):

- [`render.yaml`](../../render.yaml) — raíz del repo.
- `artisync/Frontend/Dockerfile.render`
- `artisync/Frontend/nginx.render.conf`
- `artisync/Frontend/docker-entrypoint-render.sh`
- `spring.data.redis.url=${REDIS_URL:}` agregado en
  `artisync/Backend/src/main/resources/application.properties` (necesario
  porque el Key Value de Render solo expone un `connectionString`, no host y
  puerto por separado).

Confírmalo y comitéalos si aún no están en tu rama de trabajo:

```bash
git status
git add render.yaml artisync/Frontend/Dockerfile.render artisync/Frontend/nginx.render.conf artisync/Frontend/docker-entrypoint-render.sh artisync/Backend/src/main/resources/application.properties
git commit -m "feat(despliegue): blueprint de Render para backend, frontend y redis"
git push
```

## 4. Crear el Blueprint en Render

1. Entra a [dashboard.render.com](https://dashboard.render.com) → **New +** → **Blueprint**.
2. Conecta tu cuenta de GitHub si no lo has hecho, y selecciona el repositorio
   `Proyecto-WEB-ARTISYNC`.
3. Elige la rama (`main`, o la que tenga el `render.yaml` ya comiteado).
4. Render detecta `render.yaml` automáticamente y muestra un preview con los
   tres servicios (`artisync-backend`, `artisync-frontend`, `artisync-redis`).
   Revisa que los nombres coincidan y pulsa **Apply**.
5. Render crea los tres servicios y arranca el primer build. **Va a fallar o
   quedar en espera** hasta completar el paso 5 (variables `sync: false` sin
   valor).

## 5. Completar las variables de entorno (dashboard)

Ve a `artisync-backend` → **Environment** y completa cada variable marcada
`sync: false` en `render.yaml`:

| Variable | De dónde sacarla |
|---|---|
| `DB_URL` | `jdbc:postgresql://<tu-servidor>.postgres.database.azure.com:5432/artisyncbd?sslmode=require` |
| `DB_USER` | Cuenta admin de Azure PostgreSQL (rotada, ver sección 0) |
| `DB_PASSWORD` | Contraseña de esa cuenta admin |
| `DB_APP_USER` | `artisync_app` |
| `DB_APP_PASSWORD` | Contraseña de la cuenta de privilegios mínimos. **Sin escapar el `$`** — a diferencia de `.env` con Docker Compose, aquí se pega el valor real tal cual (ej. `App$ecure2026!`, no `App$$ecure2026!`) |
| `JWT_SECRET` | `openssl rand -hex 32` — nuevo, distinto al de desarrollo |
| `MAIL_USER` / `MAIL_PASSWORD` | Cuenta SMTP de producción (contraseña de aplicación, no la contraseña normal de la cuenta) |
| `FRONTEND_URL` | Déjalo vacío por ahora — se completa en el paso 6, cuando `artisync-frontend` ya tenga URL asignada |
| `AZURE_STORAGE_CONNECTION_STRING` | Solo si usas `DOCUMENTOS_PROVEEDOR=azure` (ya viene fijado así en `render.yaml`) |
| `NVIDIA_API_KEY` / `GEMINI_API_KEY` | Solo si cambias `IA_PROVIDER` de `mock` a `nvidia` o `gemini` |
| `PAYPAL_CLIENT_ID` / `PAYPAL_CLIENT_SECRET` / `PAYPAL_WEBHOOK_ID` | App de PayPal Sandbox en `developer.paypal.com` |

`REDIS_URL` **no** se completa a mano: `render.yaml` ya la resuelve
automáticamente desde `artisync-redis` (`fromService` → `connectionString`).

Guarda los cambios — cada guardado dispara un nuevo deploy del backend.

## 6. Primer deploy y verificación de arranque

1. En `artisync-backend` → **Logs**, sigue el build. Los pasos esperados:
   Maven descarga dependencias → compila → arranca Spring Boot → Flyway valida
   contra Azure → `Started ArtisyncApplication`.
2. Cuando el servicio quede **Live**, abre
   `https://<tu-backend>.onrender.com/actuator/health` — debe responder
   `{"status":"UP"}`.
   - Si da `DOWN` en el componente `db`: revisa `DB_URL`/`DB_APP_USER`/`DB_APP_PASSWORD`
     y que el firewall de Azure PostgreSQL permita conexiones desde Render (en
     Azure, agrega la regla "Allow Azure services and resources" o el rango de
     IPs salientes de Render — Render → tu servicio → **Connect** → **Outbound**).
   - Si da `DOWN` en `redis`: revisa que `artisync-redis` esté `Live` y que
     `REDIS_URL` se haya poblado (Environment del backend, ya sin `sync: false`
     al ser `fromService`).
3. En `artisync-frontend` → **Logs**, confirma que el build de Angular termina
   sin errores y que nginx arranca (`docker-entrypoint-render.sh` debe loguear
   sin el error de `BACKEND_INTERNAL_URL no está definida`).
4. Copia la URL pública de `artisync-frontend`
   (`https://artisync-frontend-xxxx.onrender.com`).

## 7. Cerrar el círculo: `FRONTEND_URL`

Con la URL del frontend ya asignada:

1. `artisync-backend` → **Environment** → `FRONTEND_URL` → pega la URL completa
   (`https://artisync-frontend-xxxx.onrender.com`, sin `/` final).
2. Guarda — esto dispara un redeploy del backend. Esa variable la usa
   `EmailService` para construir enlaces de recuperación de contraseña y el
   retorno de PayPal (`app.frontend.url`).

## 8. Verificación funcional end-to-end

1. Abre la URL del frontend en el navegador.
2. Registra un usuario de prueba, inicia sesión, navega el catálogo — confirma
   que las llamadas a `/api/...` resuelven (Network tab del navegador, todas
   same-origin contra el propio dominio del frontend, sin CORS visible).
3. Si el módulo de chat (RF-14) ya está activo: confirma que la conexión WS
   contra `/ws/chat` abre correctamente (nginx.render.conf ya incluye el
   proxy con cabeceras `Upgrade`/`Connection`).
4. Prueba recuperación de contraseña — confirma que el correo llega con un
   enlace apuntando a la URL del frontend, no a `localhost`.

## 9. Limitaciones del plan free a tener presentes

| Limitación | Efecto práctico |
|---|---|
| Web Services se duermen tras 15 min sin tráfico | La primera petición tras inactividad tarda ~1 min en responder (cold start) |
| 750 h de instancia gratis por workspace/mes | Suficiente para dos servicios web corriendo continuamente casi todo el mes; vigila el uso si agregas más servicios |
| Key Value free: datos solo en memoria | Un reinicio del plan free de `artisync-redis` **borra** la blacklist de JWT y el caché de catálogo — comportamiento aceptable aquí porque ambos son efímeros por diseño (ver ADR-004), pero implica que sesiones revocadas antes del reinicio "vuelven a ser válidas" hasta que expiren por tiempo. Documentarlo como limitación conocida si se reporta en la entrega. |
| Solo una instancia Key Value free por workspace | No se puede tener staging + producción con Redis free simultáneos en la misma cuenta |
| Sin disco persistente en `web` free | Los documentos subidos (verificación de identidad) deben ir a `DOCUMENTOS_PROVEEDOR=azure`, nunca a `local` — con `local` se perderían en cada redeploy |

## 10. Checklist de seguridad posterior al despliegue

- [ ] Contraseña de la cuenta admin de Azure PostgreSQL rotada (si venías de
      `feat/despliegue-render`, ver sección 0).
- [ ] `JWT_SECRET` nuevo, distinto al usado en desarrollo o en cualquier commit
      anterior.
- [ ] `APP_COOKIE_SECURE=true` confirmado en el Environment del backend.
- [ ] Firewall de Azure PostgreSQL restringido a las IPs salientes de Render,
      no abierto a `0.0.0.0/0`.
- [ ] `PAYPAL_MODE=sandbox` mientras no haya aprobación para producción real.
- [ ] URL pública de `artisync-backend` documentada para quien evalúe, con
      nota de que Swagger UI queda accesible ahí (ver sección 1).

## Referencias

- [`DEPLOYMENT.md`](DEPLOYMENT.md) — procedimiento genérico multi-proveedor.
- [`RUNBOOK.md`](RUNBOOK.md) — operación día a día.
- [`BACKUP.md`](BACKUP.md) — estrategia de respaldo (Azure PostgreSQL, no cubierto por Render).
- [ADR-005](../adr/adr-005-estrategia-despliegue.md) — decisión de arquitectura de despliegue.
- [Render Blueprint spec](https://render.com/docs/blueprint-spec)
- [Render Private Services](https://render.com/docs/private-services)
- [Render Free plan](https://render.com/docs/free)
