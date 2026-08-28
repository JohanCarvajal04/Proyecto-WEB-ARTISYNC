# Runbook de operación — Artisync (Entrega Final, v1.0.0)

Cumple el Bloque A.4.2 de la guía de la Entrega Final. Manual de operación básica: arranque y
apagado ordenado, rotación de secretos, rotación de contenedores por actualizaciones de
seguridad, y restauración desde respaldo (procedimiento detallado de respaldo en
[`BACKUP.md`](BACKUP.md)).

## 1. Arranque ordenado

```bash
cp artisync/.env.example artisync/.env   # solo la primera vez; luego editar con valores reales
make up
```

`make up` ejecuta `docker compose up -d --build`, que Docker Compose ya orquesta en el orden
correcto por las dependencias declaradas (`depends_on` + `healthcheck`) en
`artisync/docker-compose.yml`:

1. `postgres` y `redis` arrancan primero; Compose espera su `healthcheck` en verde.
2. `backend` arranca solo cuando postgres/redis están saludables. Al iniciar, Flyway aplica
   automáticamente cualquier migración pendiente (`spring.flyway.enabled=true`) contra el
   esquema versionado en `artisync/Backend/src/main/resources/db/migration/`.
3. `frontend` arranca al final, sirviendo el build de producción de Angular vía nginx.

Verificación post-arranque: `curl https://<dominio>/actuator/health` (o `http://localhost:4200`
en local) debe responder `{"status":"UP"}`. Desde H-05, `/actuator/health` sin autenticar solo
devuelve el estado agregado (sin el detalle por componente, `show-details=when-authorized`); para
ver el desglose de `db`/`redis` hace falta un token de un usuario ADMIN
(`curl -H "Authorization: Bearer <token-admin>" https://<dominio>/actuator/health`), o revisar
los logs de arranque del `backend` (Spring Boot registra el resultado de cada `HealthIndicator`).

## 2. Apagado ordenado

```bash
make down       # detiene los contenedores, conserva los volúmenes (datos de postgres persisten)
```

Para un apagado que además libera todos los datos locales (solo en entornos de desarrollo/prueba,
**nunca en producción sin haber respaldado antes**):

```bash
docker compose -f artisync/docker-compose.yml down -v
```

## 3. Rotación de secretos

### JWT_SECRET

1. Generar un nuevo secreto: `openssl rand -hex 32`.
2. Actualizar `JWT_SECRET` en el `.env` (o el gestor de secretos del proveedor) del ambiente de
   producción.
3. Reiniciar únicamente el servicio `backend`: `docker compose -f artisync/docker-compose.yml up -d --build backend`.
4. **Efecto esperado:** todos los JWT emitidos con el secreto anterior dejan de validar de
   inmediato (`JwtAuthenticationFilter` rechaza la firma) — todas las sesiones activas se cierran.
   Rotar en una ventana de mantenimiento anunciada, o aceptar el cierre de sesión forzado como
   parte del procedimiento.

### Contraseñas de base de datos (`DB_PASSWORD`, `DB_APP_PASSWORD`)

1. Cambiar la contraseña en el motor de base de datos (`ALTER USER ... WITH PASSWORD '...'` o el
   panel del proveedor gestionado).
2. Actualizar la variable correspondiente en `.env`/secretos del proveedor.
3. Reiniciar `backend` (el pool de conexiones de Spring/HikariCP reconecta con las nuevas
   credenciales al arrancar).
4. Confirmar con `/actuator/health` que el estado agregado sigue en `UP`; para ver el componente
   `db` específicamente hace falta autenticar la petición como ADMIN (ver nota de H-05 en la
   sección 1) o revisar los logs de arranque del `backend`.

## 4. Rotación de contenedores por actualizaciones de seguridad

Las imágenes de terceros están ancladas por digest `sha256` en
`artisync/docker-compose.yml` (`postgres:16@sha256:...`, `redis:7-alpine@sha256:...`) — un
`docker compose pull` normal **no** las actualiza automáticamente, por diseño (evita derivas
silenciosas, ver `ADR-005`). Para aplicar un parche de seguridad publicado por la imagen base:

1. Obtener el nuevo digest: `docker pull postgres:16 && docker inspect --format='{{index .RepoDigests 0}}' postgres:16` (análogo para `redis:7-alpine`).
2. Actualizar el digest en `artisync/docker-compose.yml` (comentario junto a cada `image:` ya
   documenta el comando exacto).
3. `docker compose -f artisync/docker-compose.yml up -d --build` — recrea solo los servicios cuya
   definición cambió.
4. Para las imágenes propias (`backend`, `frontend`): reconstruir con `--build` tras actualizar la
   imagen base en el `Dockerfile` correspondiente (`eclipse-temurin:21-jre-alpine`,
   `node:24-alpine`), y verificar que la suite de pruebas (`make test`) sigue en verde antes de
   promover a producción.

## 5. Restauración desde respaldo

Ver el procedimiento operativo completo en [`BACKUP.md`](BACKUP.md). Resumen de los pasos:

1. Detener el tráfico de escritura (modo mantenimiento o apagar `backend`).
2. Restaurar el dump más reciente (`pg_restore` o `psql < backup.sql` según el formato) contra
   una base de datos limpia o la misma instancia tras vaciar el esquema.
3. Verificar con una consulta de conteo (ej. `SELECT count(*) FROM pedidos;`) contra el número
   esperado antes del incidente.
4. Reiniciar `backend` y confirmar `/actuator/health` en `UP`.
5. Documentar el incidente y la restauración en `docs/observaciones/OBSERVACIONES.md` si aplica.
