# Artisync — Plataforma web de comisiones y venta de contenido digital

[![CI](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml/badge.svg)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.21978572.svg)](https://doi.org/10.5281/zenodo.21978572)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v1.1.0-blue)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/releases/tag/v1.1.0)

Proyecto Fin de Curso (PFC) — Aplicaciones Web, Quinto nivel.
Universidad Técnica Estatal de Quevedo · Facultad de Ciencias de la Computación y Diseño Digital · Carrera de Ingeniería de Software.

Artisync centraliza la comercialización de servicios y productos digitales de profesionales creativos (ilustradores, músicos, diseñadores, desarrolladores). Conecta **Creadores** con **Clientes** y gestiona perfiles, catálogo dinámico, mensajería, contratos con firma electrónica, flujo de pedidos, pagos con patrón *escrow* vía PayPal, y funciones sociales.

> **DOI persistente.** El archivo Zenodo del tag `v1.0.0` está publicado con el DOI [`10.5281/zenodo.21978572`](https://doi.org/10.5281/zenodo.21978572), declarado también en `CITATION.cff` y en la portada del documento académico final (`docs/informe-final/secciones/00-portada-resumen.tex`). La versión anterior, `v0.9.0-rc`, quedó archivada con el DOI [`10.5281/zenodo.21730559`](https://doi.org/10.5281/zenodo.21730559). Pendiente: depositar el dataset de mediciones en un registro Zenodo separado con DOI propio (ver Bloque D.3 de la guía).

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
| Lighthouse — Rendimiento (mobile / desktop) | 80–81 / 100 · 100 / 100 | ≥ 80 |
| Lighthouse — Accesibilidad (mobile / desktop) | 93 / 100 · 93 / 100 | ≥ 90 |
| Lighthouse — Buenas prácticas (mobile / desktop) | 96 / 100 · 96 / 100 | ≥ 90 |
| Lighthouse — SEO (mobile / desktop) | 100 / 100 · 100 / 100 | ≥ 90 |
| Controles OWASP evidenciados | 6 / 6 | 6 |
| Escaneo OWASP ZAP baseline | 0 FAIL · 8 WARN · 59 PASS | 0 hallazgos altos |
| Análisis estático SQL (SpotBugs + find-sec-bugs) | 0 hallazgos de inyección | 0 hallazgos |
| Cobertura JaCoCo (líneas / ramas) | 72.0 % / 62.5 % | ≥ 70 % |
| Usabilidad SUS | 76.88 / 100 (n=16, Bangor B) | > 68 puntos, ≥ 10 participantes |

El diccionario de variables está en [`docs/mediciones/DATA-DICTIONARY.md`](docs/mediciones/DATA-DICTIONARY.md). El estado de cumplimiento frente a la guía de la Entrega Final, incluidas las brechas abiertas, se detalla en [`docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`](docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md).

La colección Postman con 26 peticiones (éxito, validación 400, autorización 401/403, no encontrado 404) está en [`Pruebas.postman_collection.json`](Pruebas.postman_collection.json), en la raíz del repositorio — fuente única; la copia antigua bajo `docs/mediciones/` quedó eliminada por estar desactualizada.

---

## Versionado

El proyecto sigue [Semantic Versioning 2.0.0](https://semver.org/) y [Keep a Changelog](https://keepachangelog.com/). Ver [`CHANGELOG.md`](CHANGELOG.md) y [`docs/VERSIONING.md`](docs/VERSIONING.md).

| Etiqueta | Hito |
| --- | --- |
| `v0.7.0` | Entrega 1B — módulo de autenticación y acceso a datos |
| `v0.7.1` | Cierre de la aplicación de observaciones de las Entregas 1A y 1B |
| `v0.9.0-rc` | Tercera Entrega — *release candidate* |
| `v1.0.0` | Entrega Final — primera versión estable de producción (commit `d07656b`, archivado con DOI en Zenodo) |
| `v1.1.0` | Trabajo posterior al cierre académico: refactor de autorización por permisos (backend/frontend) y endurecimiento de seguridad — no forma parte de la Entrega Final evaluada |

## Equipo y contribuciones

Los roles de cada integrante, según la taxonomía CRediT, están declarados en [`CONTRIBUTORS.md`](CONTRIBUTORS.md). Para citar este software, ver [`CITATION.cff`](CITATION.cff).

## Licencia

Distribuido bajo licencia MIT. Ver [`LICENSE`](LICENSE).
