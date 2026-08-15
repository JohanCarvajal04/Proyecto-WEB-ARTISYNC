# Artisync — Plataforma web de comisiones y venta de contenido digital

[![CI](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml/badge.svg)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.21730559.svg)](https://doi.org/10.5281/zenodo.21730559)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v0.9.0--rc-blue)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/releases/tag/v0.9.0-rc)

Proyecto Fin de Curso (PFC) — Aplicaciones Web, Quinto nivel.
Universidad Técnica Estatal de Quevedo · Facultad de Ciencias de la Computación y Diseño Digital · Carrera de Ingeniería de Software.

Artisync centraliza la comercialización de servicios y productos digitales de profesionales creativos (ilustradores, músicos, diseñadores, desarrolladores). Conecta **Creadores** con **Clientes** y gestiona perfiles, catálogo dinámico, mensajería, contratos con firma electrónica, flujo de pedidos, pagos con patrón *escrow* vía PayPal, y funciones sociales.

> **DOI persistente.** El archivo Zenodo del tag `v0.9.0-rc` está publicado con el DOI [`10.5281/zenodo.21730559`](https://doi.org/10.5281/zenodo.21730559), declarado también en `CITATION.cff`. Falta declararlo en la portada del informe técnico (`docs/informe-entrega-3.pdf`) cuando este se redacte.

---

## Arranque rápido

Requisitos previos: **Docker** + **Docker Compose**, y **GNU Make**.

```bash
git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git
cd Proyecto-WEB-ARTISYNC
cp artisync/.env.example artisync/.env
make up
```

`make up` levanta los cuatro servicios (PostgreSQL, Redis, backend y frontend). El esquema de la base de datos y los datos semilla se aplican automáticamente en el primer arranque desde `artisync/db/`, montados en `/docker-entrypoint-initdb.d/`.

Para un entorno reproducible basta con el `.env.example` tal cual; para un despliegue real, genera un secreto propio con `openssl rand -hex 32` y sustitúyelo en `JWT_SECRET`.

### Servicios expuestos

El backend **no publica el puerto 8080 al host** (OBS-AUTO-05 / A07 OWASP): ese es el límite de confianza que hace fiable `X-Forwarded-For` para el rate limiting y el log de auditoría de login. Todo el acceso desde el host pasa por el proxy del frontend, en el 4200 (reglas `/api` y `/actuator` en `Frontend/proxy.docker.conf.json`).

| Servicio | URL |
| --- | --- |
| Frontend (Angular) | http://localhost:4200 |
| API REST (Spring Boot) | http://localhost:4200/api |
| Especificación OpenAPI | http://localhost:4200/api/docs |
| Swagger UI | http://localhost:4200/api/swagger-ui.html |
| Estado del sistema (Actuator) | http://localhost:4200/actuator/health |

### Credenciales de arranque

La semilla crea una cuenta de administrador con todos los permisos:

| Correo | Contraseña |
| --- | --- |
| `admin@artisync.com` | `ArtisyncAdmin2026!` |

La aplicación se conecta a PostgreSQL con la cuenta `artisync_app`, de **privilegios mínimos** (sólo `SELECT/INSERT/UPDATE/DELETE` y `EXECUTE`, sin DDL ni superusuario); las migraciones Flyway usan una conexión separada. Ver `artisync/db/seed_privilegios.sh`.

### Objetivos disponibles

| Comando | Acción |
| --- | --- |
| `make up` | Levanta el sistema completo (build incluido) |
| `make down` | Detiene los servicios conservando los datos |
| `make test` | Ejecuta la suite JUnit del backend |
| `make bench` | Prueba de carga k6 contra el endpoint de catálogo |
| `make audit` | Auditoría estática de SQL dinámico |
| `make clean` | Detiene los servicios, borra volúmenes y limpia el build |

---

## Pila tecnológica

| Capa | Tecnología |
| --- | --- |
| Backend | Java · Spring Boot 4.1.0 · Spring Security · Spring Data JPA / Hibernate · jjwt |
| Frontend | Angular 22 · TypeScript |
| Base de datos | PostgreSQL 16 · migraciones Flyway (`V1`–`V5`) |
| Caché y revocación de sesiones | Redis 7 |
| Documentación de API | Springdoc OpenAPI 3 |
| Orquestación | Docker Compose (imágenes ancladas por digest `sha256`) |
| CI | GitHub Actions (compilación, pruebas, validación de trazabilidad) |

---

## Estructura del repositorio

| Ruta | Contenido |
| --- | --- |
| `artisync/Backend/` | Código Spring Boot por capas (entity, dto, repository, service, controller, security, config) |
| `artisync/Frontend/` | Aplicación Angular (`core/`, `shared/`, `features/`) |
| `artisync/db/` | `schema.sql`, `seed.sql` y `seed_privilegios.sh` — bootstrap de PostgreSQL |
| `artisync/Backend/src/main/resources/db/migration/` | Migraciones Flyway `V1__`–`V5__` (fuente de verdad del esquema) |
| `artisync/docker-compose.yml` | Orquestación de los cuatro servicios |
| `docs/requisitos/` | SRS (ISO/IEC/IEEE 29148:2018), historias de usuario, casos de uso |
| `docs/adr/` | Registros de decisiones de arquitectura (plantilla Nygard) |
| `docs/diagramas/` | Diagramas C4 (niveles 1–3), modelo entidad-relación y wireframes |
| `docs/mediciones/` | Evidencia empírica: rendimiento (k6), seguridad (OWASP), cobertura (JaCoCo), Lighthouse, SUS |
| `docs/trazabilidad/matriz.csv` | Matriz requisito → historia → caso de uso → código → prueba → evidencia |
| `docs/observaciones/` | Bitácora de observaciones de las entregas previas y su resolución |
| `scripts/` | Utilidades de validación ejecutadas en CI |
| `Makefile` | Objetivos `up`, `down`, `test`, `bench`, `audit`, `clean` |

---

## Evidencia empírica

Todas las mediciones, con sus datos crudos, están versionadas bajo [`docs/mediciones/`](docs/mediciones/):

| Dimensión | Resultado | Umbral |
| --- | --- | --- |
| Rendimiento (p95, caché caliente) | 50.17 ms | < 200 ms |
| Rendimiento (p95, caché frío) | 39.14 ms | < 500 ms |
| Errores HTTP ≥ 500 | 0.00 % | 0 % |
| Lighthouse — Rendimiento | 92 / 100 | ≥ 80 |
| Lighthouse — Accesibilidad | 100 / 100 | ≥ 90 |
| Lighthouse — Buenas prácticas | 100 / 100 | ≥ 90 |
| Lighthouse — SEO | 100 / 100 | ≥ 90 |
| Controles OWASP evidenciados | 6 / 6 | 6 |
| Cobertura JaCoCo (líneas) | 23.0 % | ≥ 60 % *(no alcanzado)* |
| Usabilidad SUS | sin participantes *(pendiente)* | ≥ 10 participantes |

El diccionario de variables está en [`docs/mediciones/DATA-DICTIONARY.md`](docs/mediciones/DATA-DICTIONARY.md). El estado de cumplimiento frente a la guía de la entrega, incluidas las brechas abiertas, se detalla en [`docs/INFORME-CUMPLIMIENTO-ENTREGA-3.md`](docs/INFORME-CUMPLIMIENTO-ENTREGA-3.md).

---

## Versionado

El proyecto sigue [Semantic Versioning 2.0.0](https://semver.org/) y [Keep a Changelog](https://keepachangelog.com/). Ver [`CHANGELOG.md`](CHANGELOG.md) y [`docs/VERSIONING.md`](docs/VERSIONING.md).

| Etiqueta | Hito |
| --- | --- |
| `v0.7.0` | Entrega 1B — módulo de autenticación y acceso a datos |
| `v0.7.1` | Cierre de la aplicación de observaciones de las Entregas 1A y 1B |
| `v0.9.0-rc` | Tercera Entrega — *release candidate* |

## Equipo y contribuciones

Los roles de cada integrante, según la taxonomía CRediT, están declarados en [`CONTRIBUTORS.md`](CONTRIBUTORS.md). Para citar este software, ver [`CITATION.cff`](CITATION.cff).

## Licencia

Distribuido bajo licencia MIT. Ver [`LICENSE`](LICENSE).
