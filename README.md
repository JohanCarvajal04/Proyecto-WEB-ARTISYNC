# Artisync — Plataforma web de comisiones y venta de contenido digital

[![CI](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml/badge.svg)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml)
[![DOI software](https://zenodo.org/badge/DOI/10.5281/zenodo.21978572.svg)](https://doi.org/10.5281/zenodo.21978572)
[![DOI dataset](https://zenodo.org/badge/DOI/10.5281/zenodo.22236251.svg)](https://doi.org/10.5281/zenodo.22236251)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v1.1.0-blue)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/releases/tag/v1.1.0)

Proyecto Fin de Curso (PFC) — Aplicaciones Web, Quinto nivel.
Universidad Técnica Estatal de Quevedo · Facultad de Ciencias de la Computación y Diseño Digital · Carrera de Ingeniería de Software.

Artisync centraliza la comercialización de servicios y productos digitales de profesionales creativos (ilustradores, músicos, diseñadores, desarrolladores). Conecta **Creadores** con **Clientes** y gestiona perfiles, catálogo dinámico, mensajería, contratos con firma electrónica, flujo de pedidos, pagos con patrón *escrow* vía PayPal, y funciones sociales.

> **DOI persistente.** El archivo Zenodo del tag `v1.0.0` está publicado con el DOI [`10.5281/zenodo.21978572`](https://doi.org/10.5281/zenodo.21978572), declarado también en `CITATION.cff` y en la portada del documento académico final (`docs/informe-final/secciones/00-portada-resumen.tex`). La versión anterior, `v0.9.0-rc`, quedó archivada con el DOI [`10.5281/zenodo.21730559`](https://doi.org/10.5281/zenodo.21730559). El dataset de mediciones (`docs/mediciones/`) está depositado por separado, con licencia CC BY 4.0, en el DOI [`10.5281/zenodo.22236251`](https://doi.org/10.5281/zenodo.22236251), siguiendo el principio de citación independiente de software y datos (Bloque D.3 de la guía).

---

## Índice

- [Requisitos previos](#requisitos-previos)
- [Arranque rápido](#arranque-rápido)
- [Variables de entorno (`.env`)](#variables-de-entorno-env)
- [Servicios expuestos](#servicios-expuestos)
- [Credenciales de arranque](#credenciales-de-arranque)
- [Objetivos de `make`](#objetivos-de-make)
- [Reproducción end-to-end completa (`make all`)](#reproducción-end-to-end-completa-make-all)
- [Compilar el documento académico](#compilar-el-documento-académico)
- [Pila tecnológica](#pila-tecnológica)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Evidencia empírica](#evidencia-empírica)
- [Integridad del documento académico (SHA-256)](#integridad-del-documento-académico-sha-256)
- [Versionado](#versionado)
- [Equipo y contribuciones](#equipo-y-contribuciones)
- [Licencia](#licencia)

---

## Requisitos previos

Para levantar el sistema (`make up`) solo hace falta:

- **Docker** y **Docker Compose** (v2, el comando es `docker compose`, no `docker-compose`).
- **GNU Make**.
- **Git**.

No necesitas instalar Java, Node, Angular CLI, Maven ni PostgreSQL en tu máquina: todo corre dentro de contenedores.

Para reproducir **también** las mediciones empíricas (`make all`, ver más abajo) hacen falta además:

- **k6** — pruebas de carga (<https://k6.io/docs/get-started/installation/>).
- **Python 3** con `pip` — análisis estadístico (SUS, inferencia sobre k6).
- Nada más para LaTeX/Lighthouse/SpotBugs/ZAP: esos objetivos ya usan contenedores Docker efímeros (`texlive/texlive`, `pandoc/latex`, `node:20` + Chromium, `maven:3.9`, `zaproxy/zaproxy`) para no exigir instalaciones locales.

---

## Arranque rápido

```bash
git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git
cd Proyecto-WEB-ARTISYNC
cp artisync/.env.example artisync/.env
make up
```

`make up` levanta los cuatro servicios (PostgreSQL, Redis, backend y frontend) con `docker compose`. El esquema de la base de datos y los datos semilla se aplican **automáticamente** en el primer arranque desde `artisync/db/schema.sql` y `artisync/db/seed.sql`, montados en `/docker-entrypoint-initdb.d/` — no hay ningún paso manual de migración.

Si olvidas el `cp artisync/.env.example artisync/.env`, `make up` lo genera por ti (y, si tienes `openssl` instalado, genera un `JWT_SECRET` real de 32 bytes automáticamente en vez de dejar el placeholder del ejemplo, que es demasiado corto y hace fallar el arranque del backend).

**Verificar que todo levantó bien:**

```bash
docker compose -f artisync/docker-compose.yml ps
curl http://localhost:4200/actuator/health
```

Debe mostrar los cuatro contenedores (`pfc_postgres`, `pfc_redis`, `pfc_backend`, `pfc_frontend`) en estado `healthy`/`running`, y el `curl` debe responder `{"status":"UP"}`. El backend tarda hasta ~2 minutos en pasar su healthcheck la primera vez (build + arranque de Spring Boot).

**Para detener o limpiar:**

```bash
make down    # detiene los servicios, conserva los datos (volumen de Postgres)
make clean   # detiene los servicios, BORRA los volúmenes y limpia el build de Maven
```

---

## Variables de entorno (`.env`)

`artisync/.env.example` es la plantilla versionada; `artisync/.env` (con valores reales) **nunca se sube a Git**. Para un entorno de desarrollo/reproducción local, los valores por defecto del ejemplo son suficientes tal cual (excepto `JWT_SECRET`, que `make up` ya resuelve automáticamente). Las variables más relevantes:

| Variable | Para qué sirve | Obligatoria para `make up` |
| --- | --- | --- |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Base de datos y cuenta admin que usa Flyway para crear/migrar el esquema (DDL) | Sí (defaults funcionan) |
| `DB_APP_USER`, `DB_APP_PASSWORD` | Cuenta de **privilegios mínimos** (`artisync_app`, solo `SELECT/INSERT/UPDATE/DELETE`/`EXECUTE`) que usa Hibernate en tiempo de ejecución | Sí (defaults funcionan) |
| `JWT_SECRET` | Clave HS256 (mínimo 32 bytes) para firmar los tokens de sesión — generar con `openssl rand -hex 32` | Sí (`make up` la genera si falta) |
| `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION` | Vigencia del access token y del refresh token, en milisegundos | No (tienen default) |
| `APP_COOKIE_SECURE` | `true` en producción (HTTPS) para que la cookie de refresh lleve `Secure`; `false` en desarrollo local sin TLS | No (default `false` en local) |
| `REDIS_HOST`, `REDIS_PORT` | Conexión a Redis (blacklist de JWT y caché del catálogo) | Sí (defaults funcionan con `make up`) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD` | SMTP para correos de verificación, 2FA y notificaciones | Solo si necesitas probar esos flujos |
| `IA_PROVIDER` | `mock` (por defecto, no requiere credenciales), `gemini` o `nvidia` para verificación real por IA | No |
| `DOCUMENTOS_PROVEEDOR` | `local` (por defecto) o `azure` (requiere `AZURE_STORAGE_*` y el perfil `docker compose --profile azure up -d azurite` para el emulador) | No |
| `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_MODE`, `PAYPAL_WEBHOOK_ID` | Integración de pagos con patrón *escrow* (sandbox por defecto) | Solo si necesitas probar pagos |

> **Nota sobre contraseñas con `$`:** si una contraseña contiene el signo `$` (ej. `App$ecure`), escápalo como `$$` en el `.env` (`App$$ecure2026!`) para que Docker Compose no lo interprete como una variable.

---

## Servicios expuestos

El backend **no publica el puerto 8080 al host** (control OWASP A07): es el límite de confianza que hace fiable `X-Forwarded-For` para el *rate limiting* y el log de auditoría de login. Todo el acceso desde el host pasa por el proxy del frontend, en el puerto 4200 (reglas `/api` y `/actuator` en `artisync/Frontend/proxy.docker.conf.json`).

| Servicio | URL |
| --- | --- |
| Frontend (Angular) | <http://localhost:4200> |
| API REST (Spring Boot) | <http://localhost:4200/api> |
| Especificación OpenAPI | <http://localhost:4200/api/docs> |
| Swagger UI | <http://localhost:4200/api/swagger-ui.html> |
| Estado del sistema (Actuator) | <http://localhost:4200/actuator/health> |

Para acceder directo al backend en el 8080 (Swagger sin pasar por el proxy, `make bench-auth`, capturas de evidencia OWASP), usa el override explícito documentado en `artisync/docker-compose.yml`:

```bash
docker compose -f artisync/docker-compose.yml -f artisync/docker-compose.dev.yml up -d --build backend
```

---

## Credenciales de arranque

La semilla (`artisync/db/seed.sql`) crea una cuenta de administrador con todos los permisos:

| Correo | Contraseña |
| --- | --- |
| `admin@artisync.com` | `ArtisyncAdmin2026!` |

La aplicación se conecta a PostgreSQL con la cuenta `artisync_app`, de **privilegios mínimos** (solo `SELECT/INSERT/UPDATE/DELETE` y `EXECUTE`, sin DDL ni superusuario); las migraciones Flyway usan una conexión separada con la cuenta admin. Ver `artisync/db/seed_privilegios.sh`.

---

## Objetivos de `make`

Todos los objetivos se ejecutan desde la raíz del repositorio.

### Ciclo de vida del sistema

| Comando | Acción |
| --- | --- |
| `make up` | Levanta el sistema completo (build incluido); genera `.env` y `JWT_SECRET` si faltan |
| `make down` | Detiene los servicios conservando los datos |
| `make clean` | Detiene los servicios, borra volúmenes y limpia el build de Maven |
| `make test` | Ejecuta la suite JUnit del backend (no requiere Docker levantado) |

### Base de datos y procedimientos almacenados

| Comando | Acción |
| --- | --- |
| `make sync-procs` | Regenera `R__procedimientos.sql` a partir de la fuente canónica en `db/procs/` |
| `make sync-procs-check` | Verifica que `R__procedimientos.sql` esté sincronizado (lo mismo que corre en CI) |

### Rendimiento (k6)

| Comando | Acción |
| --- | --- |
| `make bench` | Carga k6 (50 VUs, 30 s) contra el endpoint de catálogo público (`permitAll`) — requiere `make up` y `k6` instalado |
| `make bench-auth` | 5 corridas "calientes" contra el endpoint **protegido y autenticado** (`/api/v1/admin/reportes/finanzas`) |
| `make bench-auth-cold` | Igual que `bench-auth`, pero reinicia el backend antes de cada corrida (JVM sin JIT calentado) |
| `make perf-stats` | Recalcula el test inferencial (Mann-Whitney U + Â₁₂ de Vargha-Delaney, con Welch t / d de Cohen como contraste) sobre los NDJSON ya versionados — requiere `pip` (instala `scipy` automáticamente) |

### Seguridad

| Comando | Acción |
| --- | --- |
| `make audit` | Auditoría estática de SQL dinámico (texto) + SpotBugs/find-sec-bugs (bytecode, en contenedor Maven) |
| `make audit-sql-dynamic` | Solo la auditoría de SQL dinámico como script independiente (cubre `db/procs/` y migraciones Flyway) |
| `make audit-zap` | Escaneo dinámico OWASP ZAP *baseline* contra el frontend — requiere `make up` |

### Calidad web y usabilidad

| Comando | Acción |
| --- | --- |
| `make lighthouse` | Auditoría Lighthouse (mobile + desktop, 3 corridas cada uno, 3 rutas) contra el **despliegue público** (`LIGHTHOUSE_URL`, por defecto `https://artisync-frontend.onrender.com`) |
| `make sus` | Calcula el puntaje SUS desde `docs/mediciones/sus/sus-raw.csv` — requiere `python3` |

### Documento académico y reproducibilidad

| Comando | Acción |
| --- | --- |
| `make caratula` | Compila `docs/informe-final/caratula.tex` → `Caratula-v1.1.0.pdf` (TeX local o contenedor `texlive/texlive`) |
| `make docs` | Compila el informe final completo (`main.tex` → `Informe-Final-v1.0.0.pdf`), con `caratula` como prerrequisito |
| `make srs` | Genera `docs/requisitos/SRS-v1.0.0.pdf` desde `SRS.md` vía Pandoc (contenedor `pandoc/latex` por defecto; `PANDOC_LOCAL=1` para usar un `pandoc` local) |
| `make notebook` | Ejecuta de punta a punta `docs/mediciones/reproduccion.ipynb`, recalculando cobertura/percentiles/SUS/Lighthouse desde los artefactos crudos ya versionados — requiere `pip` y `jupyter` |
| `make all` | Pipeline de reproducción end-to-end completo — ver la siguiente sección |

---

## Reproducción end-to-end completa (`make all`)

```bash
make all
```

Encadena, en este orden, sobre una clonación limpia y sin intervención manual adicional (más allá de tener Docker/k6/Python3 instalados):

`up` → `test` → `bench` → `audit` (incluye `audit-sql-dynamic`) → `audit-zap` → `lighthouse` → `sus` → `perf-stats` → `srs` → `docs`

Es decir: levanta los contenedores (Flyway aplica migraciones y Postgres aplica la semilla automáticamente), corre las pruebas unitarias, la prueba de carga k6, las auditorías estáticas y dinámicas de seguridad, Lighthouse contra el despliegue público, el análisis SUS y el test inferencial sobre los datos de rendimiento ya recolectados, y compila tanto el SRS como el informe académico final a PDF. Termina con código de salida `0` solo si todo el pipeline se reprodujo sin errores.

`make -n all` (modo *dry-run*, sin ejecutar nada) permite ver la secuencia completa de comandos antes de correrla.

> `bench-auth` y `bench-auth-cold` **no** están encadenados en `make all` a propósito: son 5 corridas de ~30 s cada una que sobrescribirían evidencia ya versionada cada vez que alguien reproduce el pipeline completo. Se corren a demanda cuando hace falta regenerar esa evidencia específica.

---

## Compilar el documento académico

El informe final (`docs/informe-final/Informe-Final-v1.0.0.pdf`), la carátula (`docs/informe-final/Caratula-v1.1.0.pdf`) y el SRS (`docs/requisitos/SRS-v1.0.0.pdf`) se generan a partir de fuente LaTeX/Markdown versionada — **no** hace falta instalar una distribución LaTeX en tu máquina, basta con Docker:

```bash
make caratula   # Carátula de una página (LaTeX -> PDF)
make docs       # Informe académico final completo (LaTeX -> PDF), incluye make caratula
make srs        # SRS (Markdown -> PDF vía Pandoc)
```

Si tienes `pdflatex`/`bibtex` (y opcionalmente `makeglossaries`) instalados localmente (TeX Live o MiKTeX), `make docs`/`make caratula` los usan directamente; si no, caen automáticamente a un contenedor efímero con la imagen `texlive/texlive:latest`, montando el repositorio completo (el informe referencia imágenes en `docs/diagramas/` y `docs/mediciones/`, fuera de `docs/informe-final/`).

`make srs` usa Docker por defecto (imagen `pandoc/latex:3.1`, configurable con `PANDOC_IMAGE`); exporta `PANDOC_LOCAL=1` si prefieres un `pandoc` local.

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
| `docs/diagramas/` | Diagramas C4 (niveles 1–3, incluido `workspace.dsl` en Structurizr), modelo entidad-relación y wireframes |
| `docs/mediciones/` | Evidencia empírica: rendimiento (k6), seguridad (OWASP), cobertura (JaCoCo), Lighthouse, SUS, cuaderno de reproducción |
| `docs/trazabilidad/matriz.csv` | Matriz requisito → historia → caso de uso → código → prueba → evidencia |
| `docs/observaciones/` | Bitácora de observaciones de las entregas previas y su resolución |
| `scripts/` | Utilidades de validación ejecutadas en CI |
| `Makefile` | Todos los objetivos de arranque, pruebas y reproducción (ver arriba) |

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
| Usabilidad SUS | 61.25 / 100 (n=16, Bangor D, no supera el umbral) | > 68 puntos, ≥ 10 participantes |

Las cifras de Lighthouse son contra el **despliegue público real** (`https://artisync-frontend.onrender.com`, no localhost), sobre 3 rutas (`/explorar`, `/explorar/creadores`, `/auth/login`) y 3 corridas por ruta y perfil — ver el detalle completo, incluidos los hallazgos que no cumplen umbral con su diagnóstico de causa raíz, en [`docs/mediciones/lighthouse/REPORTE-LIGHTHOUSE.md`](docs/mediciones/lighthouse/REPORTE-LIGHTHOUSE.md#adenda-obs-p4-01-2026-09-04--despliegue-público-3-rutas).

El diccionario de variables está en [`docs/mediciones/DATA-DICTIONARY.md`](docs/mediciones/DATA-DICTIONARY.md). El estado de cumplimiento frente a la guía de la Entrega Final, incluidas las brechas abiertas, se detalla en [`docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`](docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md).

La colección Postman con 26 peticiones (éxito, validación 400, autorización 401/403, no encontrado 404) está en [`Pruebas.postman_collection.json`](Pruebas.postman_collection.json), en la raíz del repositorio — fuente única; la copia antigua bajo `docs/mediciones/` quedó eliminada por estar desactualizada.

### Integridad del documento académico (SHA-256)

El digest SHA-256 del PDF final permite verificar que el artefacto descargado (release de GitHub, Zenodo o clon del repositorio) es bit a bit idéntico al evaluado. Publicado en los tres sitios exigidos: este README, la carátula suelta (`docs/informe-final/caratula.tex` → `Caratula-v1.1.0.pdf`) y `CITATION.cff`.

```
sha256sum docs/informe-final/Informe-Final-v1.0.0.pdf
9e563d4db4a6dce1737b0ffae4d534b6a50f56c21b76ca87041b34ed61c0a28d  Informe-Final-v1.0.0.pdf
```

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

Los roles de cada integrante, según la taxonomía CRediT, están declarados en [`CONTRIBUTORS.md`](CONTRIBUTORS.md).

> **Aclaración sobre la composición del equipo y los registros en Git:**
> El docente podrá notar que en el historial de `git log` aparecen **4 personas** realizando commits, mientras que la nómina oficial de esta asignatura registra sólo a **3 integrantes** (Johan, Bryan y Jhon Kevin). La cuarta integrante, **Niurca Scarleth Bone Arroyo**, colabora con el equipo debido a que este proyecto también es el caso de estudio para la materia paralela de *Administración de Bases de Datos*, donde ella sí es compañera de curso. Su aporte se concentra en la capa de datos (procedimientos almacenados) y fue notificado previamente. Además, cabe aclarar que los usuarios `Jhon-Kevin-Rios-Cuyabazo` y `Jk-RiosC` corresponden al mismo integrante.

## Licencia

Distribuido bajo licencia MIT. Ver [`LICENSE`](LICENSE).
