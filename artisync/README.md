# Artisync

Módulo de Autenticación JWT + Acceso a Datos con Spring Data JPA — Entrega 1B.

## Stack

Java 21 · Spring Boot 3.2 · Spring Security 6 · jjwt 0.12 · Spring Data JPA ·
Hibernate · PostgreSQL 16 · Flyway · Redis 7 · Angular 17+ · Docker Compose.

## Instrucciones de ejecución

```bash
# 1. Clonar el repositorio
git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git
cd Proyecto-WEB-ARTISYNC

# 2. Copiar variables de entorno
cp artisync/.env.example artisync/.env
# Editar artisync/.env con las credenciales del entorno local
# (generar JWT_SECRET con: openssl rand -hex 32)

# 3. Levantar todos los servicios (desde la raiz del repositorio)
make up

# 4. Verificar que todos los servicios estan en estado healthy
docker compose -f artisync/docker-compose.yml ps

# 5. Acceder a la aplicacion
# Frontend:        http://localhost:4200
# Swagger UI:       http://localhost:8080/api/swagger-ui.html
# Actuator health:  http://localhost:8080/actuator/health

# 6. Ejecutar pruebas (sin Docker)
make test
```

### Usuario administrador de arranque

El esquema y la semilla (`artisync/db/schema.sql` + `artisync/db/seed.sql`, montados en
`/docker-entrypoint-initdb.d/`) crean automaticamente una cuenta ADMIN en el primer arranque:

| Correo | Contraseña |
| --- | --- |
| `admin@artisync.com` | `ArtisyncAdmin2026!` |

## Estructura del repositorio

| Ruta                          | Contenido                                                                                               |
| ----------------------------- | ------------------------------------------------------------------------------------------------------- |
| `Backend/src/main/java/`      | Código fuente Java organizado por capa (entity, dto, service, repository, controller, security, config) |
| `Backend/src/main/resources/db/migration/` | Migraciones Flyway `V1__..V5__` — unica fuente de verdad del esquema (`spring.flyway.locations`) |
| `Backend/src/test/`           | Pruebas unitarias JUnit 5 + integración MockMvc                                                         |
| `Frontend/src/app/`           | Módulos Angular: `core/`, `shared/`, `features/`                                                        |
| `Frontend/src/environments/`  | `environment.ts` (dev) y `environment.prod.ts`                                                          |
| `db/schema.sql`, `db/seed.sql` | Esquema y semilla consolidados para bootstrap de Postgres (`/docker-entrypoint-initdb.d/`), generados a partir de las migraciones reales |
| `db/seed_privilegios.sh`      | Crea la cuenta `artisync_app` con privilegios mínimos (A.2.3)                                           |
| `docker-compose.yml`          | Servicios: backend, postgres, redis, frontend (Nginx)                                                   |
| `.env.example`                | Variables de entorno necesarias                                                                         |
| `docs/adr/`                   | Decisiones de arquitectura (ADRs)                                                                       |
| `docs/diccionario_datos.md`   | Diccionario de datos de la base                                                                         |
| `docs/evidencias/`            | Capturas de Postman, Swagger, tests, etc.                                                               |
| `.github/workflows/ci.yml`    | Pipeline CI: compile, test, docker build                                                                |

## Estado de la entrega

- [ ] `docker compose up` levanta los 4 servicios sin errores
- [ ] Autenticación JWT funcional (registro, login, logout, refresh)
- [ ] CRUD completo de la entidad principal con paginación
- [ ] ≥5 pruebas JUnit en verde
- [ ] Informe técnico en PDF adjuntado al SGA
