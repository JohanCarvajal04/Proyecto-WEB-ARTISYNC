# Artisync

Módulo de Autenticación JWT + Acceso a Datos con Spring Data JPA — Entrega 1B.

## Stack

Java 21 · Spring Boot 3.2 · Spring Security 6 · jjwt 0.12 · Spring Data JPA ·
Hibernate · PostgreSQL 16 · Flyway · Redis 7 · Angular 17+ · Docker Compose.

## Instrucciones de ejecución

```bash
# 1. Clonar el repositorio y cambiar a la rama de entrega
git clone https://github.com/[equipo]/[repo].git
cd [repo]
git checkout entrega-1b

# 2. Copiar variables de entorno
cp .env.example .env
# Editar .env con las credenciales del entorno local (generar JWT_SECRET con: openssl rand -hex 32)

# 3. Levantar todos los servicios
docker compose up --build -d

# 4. Verificar que todos los servicios estan en estado healthy
docker compose ps

# 5. Acceder a la aplicacion
# Frontend:        http://localhost
# Swagger UI:       http://localhost:8080/api/swagger-ui.html
# Actuator health:  http://localhost:8080/actuator/health

# 6. Ejecutar pruebas (sin Docker)
cd backend && ./mvnw test
```

## Estructura del repositorio

| Ruta                          | Contenido                                                                                               |
| ----------------------------- | ------------------------------------------------------------------------------------------------------- |
| `backend/src/main/java/`      | Código fuente Java organizado por capa (entity, dto, service, repository, controller, security, config) |
| `backend/src/main/resources/` | `application.yml`, migraciones Flyway (`db/migration/`)                                                 |
| `backend/src/test/`           | Pruebas unitarias JUnit 5 + integración MockMvc                                                         |
| `frontend/src/app/`           | Módulos Angular: `auth/`, `core/`, `shared/`, `features/`                                               |
| `frontend/src/environments/`  | `environment.ts` (dev) y `environment.prod.ts`                                                          |
| `database/migrations/`        | Copia de referencia de los scripts Flyway                                                               |
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
