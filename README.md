# Artisync â€” Plataforma web de comisiones y venta de contenido digital

[![CI](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml/badge.svg)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml)
[![DOI software](https://zenodo.org/badge/DOI/10.5281/zenodo.21978572.svg)](https://doi.org/10.5281/zenodo.21978572)
[![DOI dataset](https://zenodo.org/badge/DOI/10.5281/zenodo.22236251.svg)](https://doi.org/10.5281/zenodo.22236251)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v1.1.0-blue)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/releases/tag/v1.1.0)

Proyecto Fin de Curso (PFC) â€” Aplicaciones Web, Quinto nivel.
Universidad TÃ©cnica Estatal de Quevedo Â· Facultad de Ciencias de la ComputaciÃ³n y DiseÃ±o Digital Â· Carrera de IngenierÃ­a de Software.

Artisync centraliza la comercializaciÃ³n de servicios y productos digitales de profesionales creativos (ilustradores, mÃºsicos, diseÃ±adores, desarrolladores). Conecta **Creadores** con **Clientes** y gestiona perfiles, catÃ¡logo dinÃ¡mico, mensajerÃ­a, contratos con firma electrÃ³nica, flujo de pedidos, pagos con patrÃ³n *escrow* vÃ­a PayPal, y funciones sociales.

> **DOI persistente.** El archivo Zenodo del tag `v1.0.0` estÃ¡ publicado con el DOI [`10.5281/zenodo.21978572`](https://doi.org/10.5281/zenodo.21978572), declarado tambiÃ©n en `CITATION.cff` y en la portada del documento acadÃ©mico final (`docs/informe-final/secciones/00-portada-resumen.tex`). La versiÃ³n anterior, `v0.9.0-rc`, quedÃ³ archivada con el DOI [`10.5281/zenodo.21730559`](https://doi.org/10.5281/zenodo.21730559). El dataset de mediciones (`docs/mediciones/`) estÃ¡ depositado por separado, con licencia CC BY 4.0, en el DOI [`10.5281/zenodo.22236251`](https://doi.org/10.5281/zenodo.22236251), siguiendo el principio de citaciÃ³n independiente de software y datos (Bloque D.3 de la guÃ­a).

---

## Arranque rÃ¡pido

Requisitos previos: **Docker** + **Docker Compose**, y **GNU Make**.

```bash
git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git
cd Proyecto-WEB-ARTISYNC
cp artisync/.env.example artisync/.env
make up
```

`make up` levanta los cuatro servicios (PostgreSQL, Redis, backend y frontend). El esquema de la base de datos y los datos semilla se aplican automÃ¡ticamente en el primer arranque desde `artisync/db/`, montados en `/docker-entrypoint-initdb.d/`.

Para un entorno reproducible basta con el `.env.example` tal cual; para un despliegue real, genera un secreto propio con `openssl rand -hex 32` y sustitÃºyelo en `JWT_SECRET`.

### Servicios expuestos

El backend **no publica el puerto 8080 al host** (OBS-AUTO-05 / A07 OWASP): ese es el lÃ­mite de confianza que hace fiable `X-Forwarded-For` para el rate limiting y el log de auditorÃ­a de login. Todo el acceso desde el host pasa por el proxy del frontend, en el 4200 (reglas `/api` y `/actuator` en `Frontend/proxy.docker.conf.json`).

| Servicio | URL |
| --- | --- |
| Frontend (Angular) | http://localhost:4200 |
| API REST (Spring Boot) | http://localhost:4200/api |
| EspecificaciÃ³n OpenAPI | http://localhost:4200/api/docs |
| Swagger UI | http://localhost:4200/api/swagger-ui.html |
| Estado del sistema (Actuator) | http://localhost:4200/actuator/health |

### Credenciales de arranque

La semilla crea una cuenta de administrador con todos los permisos:

| Correo | ContraseÃ±a |
| --- | --- |
| `admin@artisync.com` | `ArtisyncAdmin2026!` |

La aplicaciÃ³n se conecta a PostgreSQL con la cuenta `artisync_app`, de **privilegios mÃ­nimos** (sÃ³lo `SELECT/INSERT/UPDATE/DELETE` y `EXECUTE`, sin DDL ni superusuario); las migraciones Flyway usan una conexiÃ³n separada. Ver `artisync/db/seed_privilegios.sh`.

### Objetivos disponibles

| Comando | AcciÃ³n |
| --- | --- |
| `make up` | Levanta el sistema completo (build incluido) |
| `make down` | Detiene los servicios conservando los datos |
| `make test` | Ejecuta la suite JUnit del backend |
| `make bench` | Prueba de carga k6 contra el endpoint de catÃ¡logo |
| `make audit` | AuditorÃ­a estÃ¡tica de SQL dinÃ¡mico |
| `make clean` | Detiene los servicios, borra volÃºmenes y limpia el build |

---

## Compilar el documento acadÃ©mico

El informe final (`docs/informe-final/Informe-Final-v1.0.0.pdf`) y el SRS
(`docs/requisitos/SRS-v1.0.0.pdf`) se generan a partir de fuente LaTeX/Markdown
versionada â€” **no** hace falta instalar una distribuciÃ³n LaTeX en tu mÃ¡quina,
basta con Docker.

```bash
# Informe acadÃ©mico final (LaTeX -> PDF), desde docs/informe-final/main.tex
make docs

# SRS (Markdown -> PDF vÃ­a pandoc/latex), desde docs/requisitos/SRS.md
make srs
```

`make docs` requiere `pdflatex`/`bibtex` disponibles en el PATH (TeX Live o
MiKTeX) **o**, si prefieres no instalar nada localmente, compÃ­lalo en un
contenedor efÃ­mero con la imagen `texlive/texlive:latest`:

```bash
docker run --rm -v "$(pwd)/docs/informe-final:/repo" -w /repo texlive/texlive:latest \
  bash -c "pdflatex -interaction=nonstopmode main.tex && bibtex main && \
           pdflatex -interaction=nonstopmode main.tex && \
           pdflatex -interaction=nonstopmode main.tex"
cp docs/informe-final/main.pdf docs/informe-final/Informe-Final-v1.0.0.pdf
```

`make srs` ya usa Docker por defecto (imagen `pandoc/latex:3.1`, configurable
con `PANDOC_IMAGE`); exporta `PANDOC_LOCAL=1` si prefieres un `pandoc` local.

---

## Pila tecnolÃ³gica

| Capa | TecnologÃ­a |
| --- | --- |
| Backend | Java Â· Spring Boot 4.1.0 Â· Spring Security Â· Spring Data JPA / Hibernate Â· jjwt |
| Frontend | Angular 22 Â· TypeScript |
| Base de datos | PostgreSQL 16 Â· migraciones Flyway (`V1`â€“`V5`) |
| CachÃ© y revocaciÃ³n de sesiones | Redis 7 |
| DocumentaciÃ³n de API | Springdoc OpenAPI 3 |
| OrquestaciÃ³n | Docker Compose (imÃ¡genes ancladas por digest `sha256`) |
| CI | GitHub Actions (compilaciÃ³n, pruebas, validaciÃ³n de trazabilidad) |

---

## Estructura del repositorio

| Ruta | Contenido |
| --- | --- |
| `artisync/Backend/` | CÃ³digo Spring Boot por capas (entity, dto, repository, service, controller, security, config) |
| `artisync/Frontend/` | AplicaciÃ³n Angular (`core/`, `shared/`, `features/`) |
| `artisync/db/` | `schema.sql`, `seed.sql` y `seed_privilegios.sh` â€” bootstrap de PostgreSQL |
| `artisync/Backend/src/main/resources/db/migration/` | Migraciones Flyway `V1__`â€“`V5__` (fuente de verdad del esquema) |
| `artisync/docker-compose.yml` | OrquestaciÃ³n de los cuatro servicios |
| `docs/requisitos/` | SRS (ISO/IEC/IEEE 29148:2018), historias de usuario, casos de uso |
| `docs/adr/` | Registros de decisiones de arquitectura (plantilla Nygard) |
| `docs/diagramas/` | Diagramas C4 (niveles 1â€“3), modelo entidad-relaciÃ³n y wireframes |
| `docs/mediciones/` | Evidencia empÃ­rica: rendimiento (k6), seguridad (OWASP), cobertura (JaCoCo), Lighthouse, SUS |
| `docs/trazabilidad/matriz.csv` | Matriz requisito â†’ historia â†’ caso de uso â†’ cÃ³digo â†’ prueba â†’ evidencia |
| `docs/observaciones/` | BitÃ¡cora de observaciones de las entregas previas y su resoluciÃ³n |
| `scripts/` | Utilidades de validaciÃ³n ejecutadas en CI |
| `Makefile` | Objetivos `up`, `down`, `test`, `bench`, `audit`, `clean` |

---

## Evidencia empÃ­rica

Todas las mediciones, con sus datos crudos, estÃ¡n versionadas bajo [`docs/mediciones/`](docs/mediciones/):

| DimensiÃ³n | Resultado | Umbral |
| --- | --- | --- |
| Rendimiento (p95, cachÃ© caliente) | 50.17 ms | < 200 ms |
| Rendimiento (p95, cachÃ© frÃ­o) | 39.14 ms | < 500 ms |
| Errores HTTP â‰¥ 500 | 0.00 % | 0 % |
| Lighthouse â€” Rendimiento (mobile / desktop) | 80â€“81 / 100 Â· 100 / 100 | â‰¥ 80 |
| Lighthouse â€” Accesibilidad (mobile / desktop) | 93 / 100 Â· 93 / 100 | â‰¥ 90 |
| Lighthouse â€” Buenas prÃ¡cticas (mobile / desktop) | 96 / 100 Â· 96 / 100 | â‰¥ 90 |
| Lighthouse â€” SEO (mobile / desktop) | 100 / 100 Â· 100 / 100 | â‰¥ 90 |
| Controles OWASP evidenciados | 6 / 6 | 6 |
| Escaneo OWASP ZAP baseline | 0 FAIL Â· 8 WARN Â· 59 PASS | 0 hallazgos altos |
| AnÃ¡lisis estÃ¡tico SQL (SpotBugs + find-sec-bugs) | 0 hallazgos de inyecciÃ³n | 0 hallazgos |
| Cobertura JaCoCo (lÃ­neas / ramas) | 72.0 % / 62.5 % | â‰¥ 70 % |
| Usabilidad SUS | 61.25 / 100 (n=16, Bangor D, no supera el umbral) | > 68 puntos, â‰¥ 10 participantes |

El diccionario de variables estÃ¡ en [`docs/mediciones/DATA-DICTIONARY.md`](docs/mediciones/DATA-DICTIONARY.md). El estado de cumplimiento frente a la guÃ­a de la Entrega Final, incluidas las brechas abiertas, se detalla en [`docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`](docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md).

La colecciÃ³n Postman con 26 peticiones (Ã©xito, validaciÃ³n 400, autorizaciÃ³n 401/403, no encontrado 404) estÃ¡ en [`Pruebas.postman_collection.json`](Pruebas.postman_collection.json), en la raÃ­z del repositorio â€” fuente Ãºnica; la copia antigua bajo `docs/mediciones/` quedÃ³ eliminada por estar desactualizada.

---

## Versionado

El proyecto sigue [Semantic Versioning 2.0.0](https://semver.org/) y [Keep a Changelog](https://keepachangelog.com/). Ver [`CHANGELOG.md`](CHANGELOG.md) y [`docs/VERSIONING.md`](docs/VERSIONING.md).

| Etiqueta | Hito |
| --- | --- |
| `v0.7.0` | Entrega 1B â€” mÃ³dulo de autenticaciÃ³n y acceso a datos |
| `v0.7.1` | Cierre de la aplicaciÃ³n de observaciones de las Entregas 1A y 1B |
# Artisync â€” Plataforma web de comisiones y venta de contenido digital

[![CI](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml/badge.svg)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/workflows/ci.yml)
[![DOI software](https://zenodo.org/badge/DOI/10.5281/zenodo.21978572.svg)](https://doi.org/10.5281/zenodo.21978572)
[![DOI dataset](https://zenodo.org/badge/DOI/10.5281/zenodo.22236251.svg)](https://doi.org/10.5281/zenodo.22236251)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v1.1.0-blue)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/releases/tag/v1.1.0)

Proyecto Fin de Curso (PFC) â€” Aplicaciones Web, Quinto nivel.
Universidad TÃ©cnica Estatal de Quevedo Â· Facultad de Ciencias de la ComputaciÃ³n y DiseÃ±o Digital Â· Carrera de IngenierÃ­a de Software.

Artisync centraliza la comercializaciÃ³n de servicios y productos digitales de profesionales creativos (ilustradores, mÃºsicos, diseÃ±adores, desarrolladores). Conecta **Creadores** con **Clientes** y gestiona perfiles, catÃ¡logo dinÃ¡mico, mensajerÃ­a, contratos con firma electrÃ³nica, flujo de pedidos, pagos con patrÃ³n *escrow* vÃ­a PayPal, y funciones sociales.

> **DOI persistente.** El archivo Zenodo del tag `v1.0.0` estÃ¡ publicado con el DOI [`10.5281/zenodo.21978572`](https://doi.org/10.5281/zenodo.21978572), declarado tambiÃ©n en `CITATION.cff` y en la portada del documento acadÃ©mico final (`docs/informe-final/secciones/00-portada-resumen.tex`). La versiÃ³n anterior, `v0.9.0-rc`, quedÃ³ archivada con el DOI [`10.5281/zenodo.21730559`](https://doi.org/10.5281/zenodo.21730559). El dataset de mediciones (`docs/mediciones/`) estÃ¡ depositado por separado, con licencia CC BY 4.0, en el DOI [`10.5281/zenodo.22236251`](https://doi.org/10.5281/zenodo.22236251), siguiendo el principio de citaciÃ³n independiente de software y datos (Bloque D.3 de la guÃ­a).

---

## Arranque rÃ¡pido

Requisitos previos: **Docker** + **Docker Compose**, y **GNU Make**.

```bash
git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git
cd Proyecto-WEB-ARTISYNC
cp artisync/.env.example artisync/.env
make up
```

`make up` levanta los cuatro servicios (PostgreSQL, Redis, backend y frontend). El esquema de la base de datos y los datos semilla se aplican automÃ¡ticamente en el primer arranque desde `artisync/db/`, montados en `/docker-entrypoint-initdb.d/`.

Para un entorno reproducible basta con el `.env.example` tal cual; para un despliegue real, genera un secreto propio con `openssl rand -hex 32` y sustitÃºyelo en `JWT_SECRET`.

### Servicios expuestos

El backend **no publica el puerto 8080 al host** (OBS-AUTO-05 / A07 OWASP): ese es el lÃ­mite de confianza que hace fiable `X-Forwarded-For` para el rate limiting y el log de auditorÃ­a de login. Todo el acceso desde el host pasa por el proxy del frontend, en el 4200 (reglas `/api` y `/actuator` en `Frontend/proxy.docker.conf.json`).

| Servicio | URL |
| --- | --- |
| Frontend (Angular) | http://localhost:4200 |
| API REST (Spring Boot) | http://localhost:4200/api |
| EspecificaciÃ³n OpenAPI | http://localhost:4200/api/docs |
| Swagger UI | http://localhost:4200/api/swagger-ui.html |
| Estado del sistema (Actuator) | http://localhost:4200/actuator/health |

### Credenciales de arranque

La semilla crea una cuenta de administrador con todos los permisos:

| Correo | ContraseÃ±a |
| --- | --- |
| `admin@artisync.com` | `ArtisyncAdmin2026!` |

La aplicaciÃ³n se conecta a PostgreSQL con la cuenta `artisync_app`, de **privilegios mÃ­nimos** (sÃ³lo `SELECT/INSERT/UPDATE/DELETE` y `EXECUTE`, sin DDL ni superusuario); las migraciones Flyway usan una conexiÃ³n separada. Ver `artisync/db/seed_privilegios.sh`.

### Objetivos disponibles

| Comando | AcciÃ³n |
| --- | --- |
| `make up` | Levanta el sistema completo (build incluido) |
| `make down` | Detiene los servicios conservando los datos |
| `make test` | Ejecuta la suite JUnit del backend |
| `make bench` | Prueba de carga k6 contra el endpoint de catÃ¡logo |
| `make audit` | AuditorÃ­a estÃ¡tica de SQL dinÃ¡mico |
| `make clean` | Detiene los servicios, borra volÃºmenes y limpia el build |

---

## Compilar el documento acadÃ©mico

El informe final (`docs/informe-final/Informe-Final-v1.0.0.pdf`) y el SRS
(`docs/requisitos/SRS-v1.0.0.pdf`) se generan a partir de fuente LaTeX/Markdown
versionada â€” **no** hace falta instalar una distribuciÃ³n LaTeX en tu mÃ¡quina,
basta con Docker.

```bash
# Informe acadÃ©mico final (LaTeX -> PDF), desde docs/informe-final/main.tex
make docs

# SRS (Markdown -> PDF vÃ­a pandoc/latex), desde docs/requisitos/SRS.md
make srs
```

`make docs` requiere `pdflatex`/`bibtex` disponibles en el PATH (TeX Live o
MiKTeX) **o**, si prefieres no instalar nada localmente, compÃ­lalo en un
contenedor efÃ­mero con la imagen `texlive/texlive:latest`:

```bash
docker run --rm -v "$(pwd)/docs/informe-final:/repo" -w /repo texlive/texlive:latest \
  bash -c "pdflatex -interaction=nonstopmode main.tex && bibtex main && \
           pdflatex -interaction=nonstopmode main.tex && \
           pdflatex -interaction=nonstopmode main.tex"
cp docs/informe-final/main.pdf docs/informe-final/Informe-Final-v1.0.0.pdf
```

`make srs` ya usa Docker por defecto (imagen `pandoc/latex:3.1`, configurable
con `PANDOC_IMAGE`); exporta `PANDOC_LOCAL=1` si prefieres un `pandoc` local.

---

## Pila tecnolÃ³gica

| Capa | TecnologÃ­a |
| --- | --- |
| Backend | Java Â· Spring Boot 4.1.0 Â· Spring Security Â· Spring Data JPA / Hibernate Â· jjwt |
| Frontend | Angular 22 Â· TypeScript |
| Base de datos | PostgreSQL 16 Â· migraciones Flyway (`V1`â€“`V5`) |
| CachÃ© y revocaciÃ³n de sesiones | Redis 7 |
| DocumentaciÃ³n de API | Springdoc OpenAPI 3 |
| OrquestaciÃ³n | Docker Compose (imÃ¡genes ancladas por digest `sha256`) |
| CI | GitHub Actions (compilaciÃ³n, pruebas, validaciÃ³n de trazabilidad) |

---

## Estructura del repositorio

| Ruta | Contenido |
| --- | --- |
| `artisync/Backend/` | CÃ³digo Spring Boot por capas (entity, dto, repository, service, controller, security, config) |
| `artisync/Frontend/` | AplicaciÃ³n Angular (`core/`, `shared/`, `features/`) |
| `artisync/db/` | `schema.sql`, `seed.sql` y `seed_privilegios.sh` â€” bootstrap de PostgreSQL |
| `artisync/Backend/src/main/resources/db/migration/` | Migraciones Flyway `V1__`â€“`V5__` (fuente de verdad del esquema) |
| `artisync/docker-compose.yml` | OrquestaciÃ³n de los cuatro servicios |
| `docs/requisitos/` | SRS (ISO/IEC/IEEE 29148:2018), historias de usuario, casos de uso |
| `docs/adr/` | Registros de decisiones de arquitectura (plantilla Nygard) |
| `docs/diagramas/` | Diagramas C4 (niveles 1â€“3), modelo entidad-relaciÃ³n y wireframes |
| `docs/mediciones/` | Evidencia empÃ­rica: rendimiento (k6), seguridad (OWASP), cobertura (JaCoCo), Lighthouse, SUS |
| `docs/trazabilidad/matriz.csv` | Matriz requisito â†’ historia â†’ caso de uso â†’ cÃ³digo â†’ prueba â†’ evidencia |
| `docs/observaciones/` | BitÃ¡cora de observaciones de las entregas previas y su resoluciÃ³n |
| `scripts/` | Utilidades de validaciÃ³n ejecutadas en CI |
| `Makefile` | Objetivos `up`, `down`, `test`, `bench`, `audit`, `clean` |

---

## Evidencia empÃ­rica

Todas las mediciones, con sus datos crudos, estÃ¡n versionadas bajo [`docs/mediciones/`](docs/mediciones/):

| DimensiÃ³n | Resultado | Umbral |
| --- | --- | --- |
| Rendimiento (p95, cachÃ© caliente) | 50.17 ms | < 200 ms |
| Rendimiento (p95, cachÃ© frÃ­o) | 39.14 ms | < 500 ms |
| Errores HTTP â‰¥ 500 | 0.00 % | 0 % |
| Lighthouse â€” Rendimiento (mobile / desktop) | 80â€“81 / 100 Â· 100 / 100 | â‰¥ 80 |
| Lighthouse â€” Accesibilidad (mobile / desktop) | 93 / 100 Â· 93 / 100 | â‰¥ 90 |
| Lighthouse â€” Buenas prÃ¡cticas (mobile / desktop) | 96 / 100 Â· 96 / 100 | â‰¥ 90 |
| Lighthouse â€” SEO (mobile / desktop) | 100 / 100 Â· 100 / 100 | â‰¥ 90 |
| Controles OWASP evidenciados | 6 / 6 | 6 |
| Escaneo OWASP ZAP baseline | 0 FAIL Â· 8 WARN Â· 59 PASS | 0 hallazgos altos |
| AnÃ¡lisis estÃ¡tico SQL (SpotBugs + find-sec-bugs) | 0 hallazgos de inyecciÃ³n | 0 hallazgos |
| Cobertura JaCoCo (lÃ­neas / ramas) | 72.0 % / 62.5 % | â‰¥ 70 % |
| Usabilidad SUS | 61.25 / 100 (n=16, Bangor D, no supera el umbral) | > 68 puntos, â‰¥ 10 participantes |

El diccionario de variables estÃ¡ en [`docs/mediciones/DATA-DICTIONARY.md`](docs/mediciones/DATA-DICTIONARY.md). El estado de cumplimiento frente a la guÃ­a de la Entrega Final, incluidas las brechas abiertas, se detalla en [`docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`](docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md).

La colecciÃ³n Postman con 26 peticiones (Ã©xito, validaciÃ³n 400, autorizaciÃ³n 401/403, no encontrado 404) estÃ¡ en [`Pruebas.postman_collection.json`](Pruebas.postman_collection.json), en la raÃ­z del repositorio â€” fuente Ãºnica; la copia antigua bajo `docs/mediciones/` quedÃ³ eliminada por estar desactualizada.

---

## Versionado

El proyecto sigue [Semantic Versioning 2.0.0](https://semver.org/) y [Keep a Changelog](https://keepachangelog.com/). Ver [`CHANGELOG.md`](CHANGELOG.md) y [`docs/VERSIONING.md`](docs/VERSIONING.md).

| Etiqueta | Hito |
| --- | --- |
| `v0.7.0` | Entrega 1B â€” mÃ³dulo de autenticaciÃ³n y acceso a datos |
| `v0.7.1` | Cierre de la aplicaciÃ³n de observaciones de las Entregas 1A y 1B |
| `v0.9.0-rc` | Tercera Entrega â€” *release candidate* |
| `v1.0.0` | Entrega Final â€” primera versiÃ³n estable de producciÃ³n (commit `d07656b`, archivado con DOI en Zenodo) |
| `v1.1.0` | Trabajo posterior al cierre acadÃ©mico: refactor de autorizaciÃ³n por permisos (backend/frontend) y endurecimiento de seguridad â€” no forma parte de la Entrega Final evaluada |

## Equipo y contribuciones

Los roles de cada integrante, según la taxonomía CRediT, están declarados en [`CONTRIBUTORS.md`](CONTRIBUTORS.md).

> **Aclaración sobre la composición del equipo y los registros en Git:**
> El docente podrá notar que en el historial de `git log` aparecen **4 personas** realizando commits, mientras que la nómina oficial de esta asignatura registra sólo a **3 integrantes** (Johan, Bryan y Jhon Kevin). La cuarta integrante, **Niurca Scarleth Bone Arroyo**, colabora con el equipo debido a que este proyecto también es el caso de estudio para la materia paralela de *Administración de Bases de Datos*, donde ella sí es compañera de curso. Su aporte se concentra en la capa de datos (procedimientos almacenados) y fue notificado previamente. Además, cabe aclarar que los usuarios `Jhon-Kevin-Rios-Cuyabazo` y `Jk-RiosC` corresponden al mismo integrante.


## Licencia

Distribuido bajo licencia MIT. Ver [`LICENSE`](LICENSE).
