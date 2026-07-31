# Informe de cumplimiento — Tercera Entrega PFC (`v0.9.0-rc`)

**Proyecto:** Artisync — Plataforma web de comisiones y venta de contenido digital
**Guía evaluada:** *PFC — Guía de la Tercera Entrega*, UTEQ/FCI, Dr. Gleiston Guerrero Ulloa
**Fecha de la auditoría:** 30 de julio de 2026
**Estado del repositorio auditado:** rama `main`, commit `ca8889c`, 68 commits, **sin ningún tag creado**
**Método:** verificación directa contra el repositorio (regla transversal 1: *el repositorio Git es la fuente de verdad*). No se aceptó como evidencia nada que sólo estuviera afirmado en documentos.

---

## 1. Resumen ejecutivo

El proyecto tiene una **base documental de requisitos muy sólida** (SRS, 23 historias INVEST+Gherkin, 23 casos de uso Cockburn, 6 ADRs, matriz de trazabilidad) y **cuatro de los cinco sub-bloques de evidencia empírica ya ejecutados con datos reales** (rendimiento, seguridad, cobertura, Lighthouse). Eso es, con diferencia, lo más caro de producir y ya está hecho.

Lo que hunde la nota no es la falta de trabajo técnico, sino la **ausencia de artefactos de empaquetado y de dos requisitos estructurales duros**:

| Bloqueante | Impacto en la rúbrica |
|---|---|
| No existe el tag `v0.9.0-rc` (ni ningún tag) | **Regla transversal 2: C1–C6 se califican Ausente (0 %)** → 53 % de la nota en riesgo total |
| No existe `Makefile` ni `make up` | Regla transversal 4: C2 ≤ 25 % automáticamente |
| Cero procedimientos almacenados de negocio | Rúbrica C1: *"ausencia total de procedimientos almacenados"* → Insuficiente (25 %) |
| Sin `LICENSE`, `CITATION.cff`, `CONTRIBUTORS.md` | C7 = 0 % |
| Sin DOI Zenodo | C8 = 0 % |
| Sin `docs/informe-entrega-3.pdf` | C10 severamente penalizado; además C4 exige justificar allí el sub-bloque faltante |
| Cobertura JaCoCo 23.0 % (exigido ≥ 60 %) | C4 penalizado |
| SUS con **0 participantes** (`sus-raw.csv` sólo tiene la cabecera) | C4: sub-bloque ausente |

**Estimación de nota (asumiendo que el tag se crea antes del corte):** entre **36/100 y 45/100** (3.6–4.5 sobre 10).
**Si el tag `v0.9.0-rc` no se crea:** los criterios C1–C6 (53 %) se anulan y la nota cae a **≈ 8–12/100**.

La buena noticia: de los ~55 puntos perdidos, **alrededor de 25 se recuperan con trabajo de horas, no de semanas** (tags, LICENSE, CITATION.cff, CONTRIBUTORS.md, CHANGELOG.md, VERSIONING.md, ETHICS.md, Makefile, digests sha256, Zenodo). Ver §7.

---

## 2. Tabla resumen por criterio de la rúbrica

| Criterio | Peso | Nivel estimado | Puntos | Razón principal |
|---|---|---|---|---|
| **C0** Aplicación de observaciones | 10 % | Insuficiente (25 %) | 2.5 | Sólo 3 de 15 observaciones cerradas (20 %); commits no referencian `OBS-NN`; sin tag `v0.7.1` |
| **C0R** Ingeniería de requisitos | 12 % | Satisfactorio (75 %) | 9.0 | SRS + HU + CU + matriz completos y de buena calidad; falta `SRS.pdf` y el validador en CI |
| **C1** Consolidación funcional + acceso a datos | 6 % | Insuficiente (25 %) | 1.5 | Cero SPs; sin ProblemDetails RFC 7807; access token fuera de cookie |
| **C2** Reproducibilidad automática | 10 % | Insuficiente (25 %) | 2.5 | Sin `Makefile`; sin digests sha256; sin `db/schema.sql` + `seed.sql` |
| **C3** Determinismo de mediciones | 7 % | En desarrollo (50 %) → riesgo 25 % | 1.75–3.5 | 3 corridas ✅ y versiones registradas ✅, pero **ningún script versionado** y JSON crudos excluidos por `.gitignore` |
| **C4** Evidencia empírica cuantitativa | 10 % | En desarrollo–Satisfactorio | 5.0–7.5 | 4/5 sub-bloques con datos reales; SUS vacío; cobertura 23 % |
| **C5** Documentación arquitectónica | 8 % | En desarrollo–Satisfactorio | 4.0–6.0 | 6 ADRs Nygard ✅, matriz ✅; **sin Structurizr DSL** y **sin tabla ISO 25010** |
| **C6** Auditoría OWASP | 12 % | Satisfactorio (75 %) | 9.0 | Los 6 controles con salida `curl` real; A03 responde `200` en vez de `422`+ProblemDetails |
| **C7** Licencia, citación, contribución | 5 % | Ausente (0 %) | 0 | Los tres archivos faltan |
| **C8** Archivo permanente e identificador | 5 % | Ausente (0 %) | 0 | Sin DOI |
| **C9** Datos, diccionario y versionado | 7 % | Insuficiente (25 %) | 1.75 | `DATA-DICTIONARY.md` excelente, pero sin `CHANGELOG.md` ni `VERSIONING.md` |
| **C10** Ética y calidad global | 8 % | Insuficiente (25 %) | 2.0 | Sin `ETHICS.md`, sin informe PDF, sin matriz de amenazas a la validez |
| **TOTAL** | 100 % | | **36–45** | ≈ 3.6–4.5 / 10 |

---

## 3. Análisis por bloque

### Bloque 0 — Aplicación de observaciones (C0, 10 %)

**Cumple:** `docs/observaciones/OBSERVACIONES.md` existe y es de buena calidad: 15 observaciones (12 del docente + 3 auto-detectadas), cada una con código único, fuente, criterio de rúbrica afectado, texto íntegro, decisión y columna de commit. La disciplina de separar las auto-detectadas (`OBS-AUTO-NN`) es un acierto metodológico.

**No cumple:**
- **Sólo 3 de 15 observaciones cerradas (20 %).** La rúbrica exige ≥ 80 % para Satisfactorio y penaliza con Insuficiente por debajo del 60 %.
- **Ningún mensaje de commit referencia un código `OBS-NN`.** Verificado sobre los 68 commits del historial. La bitácora referencia commits *a posteriori*, pero la rúbrica pide lo contrario: que el commit declare la observación que cierra.
- **No existe el tag `v0.7.1`** (ni `v0.7.0`, ni ninguno). `git tag -l` devuelve vacío.
- OBS-02 (versión de Angular inconsistente) **empeoró**: la documentación dice Angular 17/19 y `Frontend/package.json` declara `"@angular/core": "^22.0.0"`.
- OBS-AUTO-03 se declara pendiente pero **ya está resuelta**: el directorio `Frontend/` sí existe hoy. Conviene cerrarla formalmente.

### Bloque A.1 — Consolidación funcional (parte de C1)

| Exigencia | Estado | Evidencia |
|---|---|---|
| Endpoints CRUD operativos y documentados en `/api/docs` | ✅ | `springdoc.api-docs.path=/api/docs`; 22 controladores con `@RequestMapping` |
| Siete claims JWT (`iss, sub, aud, exp, nbf, iat, jti`) | ✅ | `JwtService.java:67-77` — los siete presentes, incluido `jti` y `nbf` |
| Autenticación bajo cookie `HttpOnly + Secure + SameSite=Strict` | ⚠️ **Parcial** | Sólo el **refresh token** va en cookie (`AuthController.java:95-105`, atributos correctos). El **access token** viaja en el cuerpo JSON y el frontend lo envía por cabecera `Authorization: Bearer` (`auth.interceptor.ts:14`) |
| ProblemDetails RFC 7807 (`type, title, status, detail, instance`) | ❌ **No cumple** | `ManejadorGlobalExcepciones` devuelve un DTO propio `RespuestaError(timestamp, status, error, message, path, fieldErrors)`. No hay ninguna referencia a `ProblemDetail` en el código |
| Cache Redis con TTL en configuración externa | ✅ | `app.cache.catalogo.ttl-seconds=${CATALOGO_CACHE_TTL:60}` + `@Cacheable(cacheNames="catalogo")` |
| *Hit ratio* medida empíricamente y reportada | ❌ | `REPORTE-PERF.md` no reporta hit ratio; de hecho admite que el diseño de la prueba no aísla los *miss* |

### Bloque A.2 — Estrategia obligatoria de acceso a datos (parte de C1) — **brecha crítica**

**Cero procedimientos almacenados o funciones de negocio en el repositorio.** No existe `db/procs/`, no existe `docs/basedatos/CATALOGO-SP.md`, y no hay una sola anotación `@Procedure` ni `@NamedStoredProcedureQuery` en las ~2 900 líneas de backend. La única función SQL del proyecto es el trigger de auditoría `set_actualizado_en` en `V2__ajustes_requisitos_pfc.sql`.

Hay **operaciones que la guía obliga explícitamente a encapsular en SP y que hoy están en JPQL**:

- `ResenaServicioRepository.java:35` — `SELECT AVG(r.calificacionEstrellas) ... JOIN r.pedido p JOIN p.servicio s` → agregación + doble join (A.2.2, viñetas 1 y 2).
- `ResenaServicioRepository.java:25` — consulta con dos joins.
- `TransaccionPagoRepository.java:21` — tres joins encadenados (`TransaccionPago → Pago → Contrato → Pedido`).
- El listado del catálogo se resuelve con `specification.catalogo` (Criteria API) cuando cruza tablas.

**Atenuante importante:** todas usan **parámetros nombrados** (`:idPerfil`) y no hay una sola concatenación de entrada de usuario en JPQL/HQL/SQL nativo ni ningún `EXECUTE IMMEDIATE`. Esto significa que **la regla transversal 7 no se dispara** — que era el peor escenario posible, porque habría anulado C1 *y* C6 simultáneamente. La brecha es de arquitectura exigida, no de seguridad.

El ADR-006 ya documenta la decisión correcta y lista cinco candidatos concretos a SP. Está redactado; sólo falta implementarlo.

### Bloque A.3 — Ingeniería de requisitos (C0R, 12 %) — **el punto más fuerte del proyecto**

**Cumple bien:**
- `docs/requisitos/SRS.md` (266 líneas) con las secciones de ISO/IEC/IEEE 29148:2018: introducción (propósito, alcance, definiciones, referencias, resumen), descripción global (perspectiva, funciones, usuarios, restricciones, supuestos) y requisitos específicos funcionales y no funcionales.
- 23 `REQ-F` + 14 `REQ-NF`, cada uno con **identificador persistente, rationale, prioridad MoSCoW, criterio de aceptación y método de verificación**. La tabla de equivalencia `RF-NN → REQ-F-0NN` respecto de la Entrega 1A es exactamente la trazabilidad hacia el corpus 1A que pide la guía.
- 23 historias en formato Connextra, con justificación INVEST explícita y criterios de aceptación en Gherkin.
- 23 casos de uso con plantilla Cockburn y los cuatro niveles de precisión.
- `CHANGELOG-REQ.md` presente.
- `docs/trazabilidad/matriz.csv` con las **once columnas exactas** que exige A.3.3 y 37 filas (100 % de los requisitos).

**No cumple / mejorable:**
- **Falta `docs/requisitos/SRS.pdf`.** La guía pide el PDF *y* su fuente. Hoy sólo existe el `.md`.
- **Falta `scripts/validate-traceability.sh` y su ejecución en CI.** El nivel Excelente lo exige explícitamente.
- **La matriz tiene columnas vacías en requisitos `Must`:** `prueba_automatizada` vacía en REQ-F-004, 006, 007, 008, 011, 012, 013, 017–021 y en casi todos los `REQ-NF`; `evidencia_empirica` vacía en la mayoría.
- **Los endpoints de la matriz no coinciden con el código.** La matriz dice `POST /api/catalogo/servicios` y `GET /api/catalogo/servicios`; el código expone `/api/v1/catalogo` y `/api/v1/servicios`. Un revisor que cruce matriz contra código lo detecta en el primer intento.
- **Incoherencia matriz ↔ realidad en `tipo_acceso`:** REQ-F-006, 007, 009, 013, 021, 023 están marcados como `SP`, pero no existe ningún SP. La matriz declara una arquitectura que el código no tiene.
- Falta el atributo de trazabilidad hacia el *stakeholder* de origen que pide A.3.1.

### Bloque B — Reproducibilidad (C2 10 %, C3 7 %)

| Exigencia | Estado |
|---|---|
| `Makefile` con `up, down, test, bench, audit, clean` | ❌ No existe ninguno (ni `justfile` ni `Taskfile.yml`) |
| `make up` levanta todo sin intervención | ❌ Imposible sin Makefile → **regla transversal 4: C2 ≤ 25 %** |
| Imágenes pinadas por digest `sha256` | ❌ `image: postgres:16`, `image: redis:7-alpine` — tags móviles |
| `.env.example` comentado, `.env` en `.gitignore` | ✅ `artisync/.env.example` versionado; `.env` correctamente **no** rastreado |
| Esquema desde `db/schema.sql` + `db/seed.sql` en `/docker-entrypoint-initdb.d/` | ❌ No existe el directorio `db/`. El esquema se aplica por Flyway (`database/migrations/V1..V5`) |
| Prohibido `ddl-auto=update` | ✅ `spring.jpa.hibernate.ddl-auto=validate` |
| Semilla `admin` con hash BCrypt documentado en README | ❌ No existe |
| Cuenta de BD con privilegios mínimos (sin superuser) | ❌ El compose usa el superusuario `POSTGRES_USER` |
| 3 corridas independientes por benchmark | ✅ 3 calientes + 3 frías, consolas reales archivadas |
| Versiones de herramientas en cada archivo de resultados | ✅ k6 v2.1.0, Lighthouse 12.6.1, fecha y commit base declarados |
| Semilla aleatoria fija documentada en los scripts | ❌ **No hay scripts versionados en todo el repositorio** — ni `k6/`, ni `k6/opts.js`, ni `scripts/perf-analysis.py`, ni `analisis-sus.py`, ni `validate-traceability.sh`, ni `audit-sql-dynamic.sh` |
| `lighthouserc.js` en raíz | ⚠️ Existe como `artisync/Frontend/lighthouserc.json`, fuera de la ruta exigida |

**Riesgo adicional grave — `docs/mediciones/.gitignore`:** excluye `perf/k6-run*.json` y `perf/k6-cold-run*.json` (~34 MB). La guía nombra esos archivos exactamente en C.1 (`docs/mediciones/perf/kNN-run{1,2,3}.json`) y la **regla transversal 8** dice que los archivos crudos deben conservarse tal cual los produjo la herramienta. El razonamiento del `.gitignore` es defendible en ingeniería, pero contradice el requisito literal. Además `DATA-DICTIONARY.md` cita como fuente archivos que un revisor **no encontrará al clonar**.

### Bloque C — Evidencia empírica (C4 10 %, C6 12 %)

**C.1 Rendimiento — cumple con honestidad metodológica.** 3 corridas × 2 escenarios, 4 500 muestras por escenario, media/mediana/DT/IC 95 %/p50-p90-p95-p99, error ≥ 500 = 0 %, throughput ≈ 48.5–49 req/s. p95 caliente 50.17 ms (< 200 ✅), p95 frío 39.14 ms (< 500 ✅). El reporte **admite abiertamente** que el escenario "frío" salió más rápido que el "caliente" porque el script golpea siempre la misma URL y sólo 1 de 1 500 iteraciones es un *miss* real. Esa transparencia es exactamente lo que se espera de un artefacto de investigación — pero deja el escenario frío sin medir de verdad. Falta el script `k6` versionado.

**C.2 Seguridad — el bloque mejor ejecutado.** Los seis controles con salida `curl` archivada:
- A01 → `403` cruzando usuarios ✅
- A02 → TLSv1.3 + `TLS_AES_256_GCM_SHA384` (AEAD) ✅
- A03 → payload `' OR '1'='1` devuelve `200` con `content: []`. **Desviación:** la guía espera `422` con ProblemDetails. Es seguro (consulta parametrizada), pero no es la respuesta pedida.
- A05 → `nosniff`, `X-Frame-Options: DENY`, CSP, Referrer-Policy, Permissions-Policy ✅. HSTS no aparece en el `.txt` capturado sobre `:8080`; se explica y se verifica sobre `:8443`, pero **no hay archivo crudo que lo demuestre** — conviene capturarlo.
- A07 → 5×`401` + `429` con `Retry-After` en el sexto ✅ (`LoginRateLimitFilter` con Redis `INCR`+`EXPIRE`)
- A09 → logs con `evento`, `resultado`, `correo`, `ip`, `sub` ✅

**C.3 Usabilidad — ausente.** `sus-raw.csv` contiene **sólo la fila de cabecera**; `REPORTE-SUS.md` es una plantilla con campos en blanco (la decisión de no rellenarla con datos falsos es correcta). Se exigen ≥ 10 participantes externos. Existe `docs/etica/consentimientos/plantilla.md`, pero no hay consentimientos ni participantes.

**C.4 Cobertura — incumple el umbral.** JaCoCo mide **23.0 % líneas / 13.8 % ramas / 16.8 % complejidad**; se exige **≥ 60 %** en esta entrega. Las 18 clases de test cubren seguridad, comunicación y social; **pedido, legal, catálogo y perfil están sin pruebas**. El reporte declara la tendencia creciente respecto de 1B de forma cualitativa y honesta (no se pudo medir el punto de comparación).

**C.5 Lighthouse — cumple los cuatro umbrales.** Performance 92 (≥80), Accessibility 100 (≥90), Best Practices 100 (≥90), SEO 100 (≥90), contra el contenedor nginx con perfil móvil y *throttling*. Se conservan las corridas antes/después con JSON y HTML. Dos observaciones menores: el nombre de archivo debería ser `lhci-YYYYMMDD-HHMM.json` sin el sufijo `-mejorado`, y `REPORTE-LIGHTHOUSE.md` enlaza a `PLAN-MEJORA-LIGHTHOUSE.md`, que **no está versionado** (enlace roto).

> **Enlaces rotos detectados:** `REPORTE-SEC.md` → `docs/PLAN-MEDICIONES.md` (no existe); `REPORTE-LIGHTHOUSE.md` → `PLAN-MEJORA-LIGHTHOUSE.md` (no existe); `REPORTE-SUS.md` → `salida-sus.txt` (no existe); `REPORTE-JACOCO.md` → `html/index.html` (excluido por `.gitignore`).

### Bloque D — Documentación arquitectónica (C5, 8 %)

**Cumple:** los **seis ADRs obligatorios** existen y siguen la plantilla Nygard con Contexto / Opciones / Decisión / Consecuencias, cubriendo exactamente los seis temas exigidos (pila, autenticación, gestor de BD, caché, despliegue, acceso a datos). El ADR-006 es especialmente bueno: documenta su propio incumplimiento y lo marca como el riesgo mayor de la entrega. Diagramas C4 de los tres niveles presentes (`C4_Nivel1_Contexto.md`, `C4_Nivel2_Contenedores.md`, `C4_Nivel3_Componentes_Backend.md` + PNG/SVG). Matriz de trazabilidad presente.

**No cumple:**
- **Sin código fuente Structurizr DSL** y sin directorio `docs/arquitectura/`. Los diagramas están en `docs/diagramas/` como Markdown + imágenes. La guía exige DSL versionado y exportación PNG generada por el pipeline.
- **Sin tabla de atributos de calidad ISO/IEC 25010** con prioridad, escenario y estrategia. La norma sólo se cita de pasada en el SRS y en el ADR-001.

### Bloque E — Publicabilidad (C7 5 %, C8 5 %, C9 7 %)

| Artefacto | Estado |
|---|---|
| `LICENSE` (OSI-approved) | ❌ Ausente → **C7 = 0 %** |
| `CITATION.cff` v1.2.0 | ❌ Ausente |
| `CONTRIBUTORS.md` con los 14 roles CRediT | ❌ Ausente |
| DOI Zenodo sobre `v0.9.0-rc` en 3 lugares | ❌ Ausente → **C8 = 0 %** |
| Badges (DOI, CI, licencia) en README | ❌ El README raíz son 20 líneas con tres ejemplos de `curl` |
| `docs/mediciones/DATA-DICTIONARY.md` | ✅ **Excelente**: 20 variables con descripción, tipo, unidad, fuente, rango esperado y valor medido |
| `docs/VERSIONING.md` (SemVer 2.0.0) | ❌ Ausente |
| `CHANGELOG.md` (Keep a Changelog) | ❌ Ausente |
| Commits Conventional Commits | ⚠️ Parcial: hay buenos (`feat(repository): ...`) junto a `feat`, `update`, `Doc :`, `Fech:`, `Interfaces`, `Creacion de carpetas` |
| ≥ 30 commits granulares con autoría de todos los integrantes | ✅ 68 commits, 5 autores distintos del equipo |

### Bloque F — Ética (parte de C10, 8 %)

- ✅ `docs/etica/consentimientos/plantilla.md` existe.
- ❌ **`docs/etica/ETHICS.md` no existe.** Faltan los cuatro puntos exigidos: fuentes de datos y licencia, tratamiento de datos personales, mecanismo de consentimiento, ausencia de datos identificables.
- ❌ **`docs/informe-entrega-3.pdf` no existe** (ninguna de las 10 secciones, incluida la matriz de amenazas a la validez).
- ❌ `docs/postman/coleccion.json` con ≥ 20 peticiones: no existe.
- ❌ Vídeo de 2–3 minutos enlazado desde el README: no existe.

---

## 4. Estructura del repositorio: exigida vs. real

La guía advierte que *"cualquier desviación se considera evidencia de una entrega incompleta"*. La desviación estructural más visible: **la guía asume `backend/`, `frontend/`, `db/`, `docker-compose.yml` y el `Makefile` en la raíz del repositorio; aquí todo cuelga de `artisync/`**, mientras `docs/` sí está en la raíz. Un revisor que clone y ejecute los comandos del ejemplo (`cp .env.example .env && make up`) falla en el primer paso.

```
Exigido en la raíz          Estado real
──────────────────────────────────────────────────────────────
README.md                   ⚠️  existe pero es un borrador de 20 líneas
LICENSE                     ❌
CITATION.cff                ❌
CONTRIBUTORS.md             ❌
CHANGELOG.md                ❌
Makefile                    ❌
docker-compose.yml          ⚠️  en artisync/ y sin digests
.env.example                ⚠️  en artisync/
.gitignore                  ⚠️  en artisync/
backend/                    ⚠️  artisync/Backend/
frontend/                   ⚠️  artisync/Frontend/
db/schema.sql|seed.sql|procs/ ❌
database/migrations/        ⚠️  artisync/database/migrations/ (V1..V5 ✅)
docs/requisitos/SRS.pdf     ❌ (SRS.md ✅)
docs/observaciones/         ✅
docs/adr/ (6)               ✅
docs/basedatos/CATALOGO-SP.md ❌
docs/arquitectura/ (DSL)    ❌ (docs/diagramas/ con MD+PNG)
docs/mediciones/perf|sec|sus|lighthouse|jacoco ✅ (crudos de perf gitignorados ⚠️)
docs/mediciones/DATA-DICTIONARY.md ✅
docs/trazabilidad/matriz.csv ✅
docs/etica/ETHICS.md        ❌ (consentimientos/plantilla.md ✅)
docs/VERSIONING.md          ❌
docs/informe-entrega-3.pdf  ❌
docs/postman/coleccion.json ❌
k6/                         ❌
lighthouserc.js             ⚠️  artisync/Frontend/lighthouserc.json
scripts/                    ❌
.github/workflows/          ⚠️  artisync/.github/workflows/ci.yml
```

**Sobre el CI:** `ci.yml` sólo compila, ejecuta tests y construye la imagen, y **sólo se dispara en `develop` y `entrega-*`** — nunca en `main`, que es la rama actual. La guía exige que el pipeline ejecute `test`, `bench` y `audit`, y que valide la trazabilidad y el diccionario de datos.

---

## 5. Reglas transversales — evaluación de riesgo

| Regla | Estado |
|---|---|
| 1. El repositorio es la fuente de verdad | Aplicada en esta auditoría |
| 2. **Sin tag `v0.9.0-rc` → C1–C6 = Ausente (0 %)** | 🔴 **SE DISPARA HOY.** 53 % de la nota |
| 3. Commits posteriores al corte se ignoran | Sin efecto (no hay informe con fecha de corte) |
| 4. `make up` falla → C2 ≤ 25 % | 🔴 Se dispara (no hay Makefile) |
| 5. DOI que no resuelve → C8 = 0 % | 🔴 Se dispara por ausencia de DOI |
| 6. `CITATION.cff` inválido → C7 ≤ 50 % | 🔴 Peor: ausente → C7 = 0 % |
| 7. **Concatenación de entrada de usuario o SQL dinámico → C1 y C6 = 25 %** | 🟢 **NO se dispara.** Todo el JPQL usa parámetros nombrados; sin `nativeQuery`, sin `EXECUTE IMMEDIATE` |
| 8. Archivos crudos sin editar manualmente | 🟡 No hay edición manual, pero los JSON crudos de k6 están excluidos del repositorio |

---

## 6. Lo que está bien hecho (conviene no tocarlo)

1. **El corpus de requisitos.** SRS + 23 HU + 23 CU + matriz de 11 columnas es material de nivel Excelente; con `SRS.pdf` y el validador en CI, C0R llega a 100 %.
2. **La auditoría OWASP.** Seis controles con `curl` real y remediaciones trazadas a `OBS-08`. El `LoginRateLimitFilter` con Redis y la validación de pertenencia en `PedidoServicioImpl` son arreglos de verdad, no cosméticos.
3. **La honestidad metodológica.** El reporte de rendimiento explica por qué su escenario frío no mide lo que dice medir; el de JaCoCo se niega a inventar el punto de comparación de 1B; el de SUS se niega a rellenarse con datos de ejemplo. Esto es exactamente lo que las normas de artefactos de investigación premian, y hay que **decirlo explícitamente en el informe PDF** para que se lea como rigor y no como omisión.
4. **`DATA-DICTIONARY.md`.** Cumple E.3 al 100 %, incluida la columna de valor medido.
5. **Los seis ADRs**, en particular el ADR-006, que documenta la brecha antes de que la detecte el evaluador.
6. **Higiene de seguridad de datos:** `.env` no está rastreado; el `.p12` está excluido; el secreto JWT viene de variable de entorno.

---

## 7. Plan de mejora priorizado

### Prioridad 0 — Bloqueantes absolutos (horas, ~15–20 puntos)

1. **Crear los tags.** Sin esto se pierde el 53 % de la nota:
   ```bash
   git tag -a v0.7.0 <commit-de-entrega-1b> -m "Entrega 1B" && git tag -a v0.7.1 -m "Cierre de observaciones 1A/1B" && git tag -a v0.9.0-rc -m "Tercera Entrega" && git push --tags
   ```
2. **`Makefile` en la raíz** con `up, down, test, bench, audit, clean`, envolviendo el `docker compose -f artisync/docker-compose.yml`. Evita la anulación automática de C2.
3. **`LICENSE`** (MIT o Apache-2.0, justificado en un ADR-007), **`CITATION.cff`** v1.2.0 validado con `cffconvert`, **`CONTRIBUTORS.md`** con roles CRediT para los cinco integrantes → C7 pasa de 0 % a 100 % (5 puntos).
4. **Zenodo:** conectar el repositorio, publicar el release `v0.9.0-rc`, y declarar el DOI en README + `CITATION.cff` + portada del PDF → C8 de 0 % a 100 % (5 puntos).
5. **`CHANGELOG.md`** (Keep a Changelog) + **`docs/VERSIONING.md`** (SemVer 2.0.0) → C9 de 25 % a ~75 % (+3.5 puntos).
6. **`docs/etica/ETHICS.md`** con los cuatro apartados → desbloquea la mitad de C10.
7. **Pinar los digests sha256** en `docker-compose.yml`:
   ```bash
   docker inspect --format='{{index .RepoDigests 0}}' postgres:16 redis:7-alpine
   ```

### Prioridad 1 — Estructurales (1–3 días, ~10–14 puntos)

8. **Reubicar a la raíz** `docker-compose.yml`, `.env.example`, `.gitignore`, `lighthouserc.js` y `.github/workflows/` (o dejar en la raíz envoltorios que deleguen). Añadir `main` a los disparadores del CI.
9. **Implementar al menos 3 procedimientos almacenados** de los cinco que el ADR-006 ya identifica — el más rentable es el cálculo de calificación promedio (`AVG` + 2 joins), que sustituye directamente `ResenaServicioRepository:35`. Versionarlos en `db/procs/sp_*.sql`, invocarlos con `@Procedure` y catalogarlos en `docs/basedatos/CATALOGO-SP.md`. Esto saca C1 de Insuficiente.
10. **Migrar el manejo de errores a `ProblemDetail`** (RFC 7807). En Spring Boot 3 es cambiar `RespuestaError` por `ProblemDetail.forStatusAndDetail(...)` con `type`, `title`, `status`, `detail`, `instance`. Afecta a C1 y a C6 (control A03, que debe devolver `422` + ProblemDetails).
11. **Crear `db/schema.sql` y `db/seed.sql`** montados en `/docker-entrypoint-initdb.d/`, con el usuario `admin` y su hash BCrypt documentado en el README. Crear además el rol de BD con privilegios mínimos (`EXECUTE` + CRUD sobre tablas del dominio, sin superusuario).
12. **Versionar los scripts que ya se usaron**: `k6/opts.js` + el script de carga, `scripts/perf-analysis.py` (con `np.random.seed(42)`), `scripts/analisis-sus.py`, `scripts/validate-traceability.sh`, `scripts/audit-sql-dynamic.sh`. Sin ellos, C3 no puede pasar de En desarrollo por mucho que las mediciones sean reales.
13. **Reconsiderar el `.gitignore` de mediciones.** Si los 34 MB son un problema, comprimir (`k6-run1.json.gz`) o recortar el volumen de la corrida, pero **el crudo tiene que estar en el repositorio** — la regla 8 y el criterio C.1 lo nombran literalmente.

### Prioridad 2 — Cierre de contenido (3–7 días, ~12–18 puntos)

14. **Ejecutar el SUS con 10+ participantes externos.** Es el único sub-bloque de C4 sin datos y no se puede improvisar el último día. La infraestructura (plantilla de consentimiento, instrucciones, CSV) ya está lista; sólo faltan las sesiones.
15. **Subir la cobertura de 23 % a ≥ 60 %.** El camino más corto son los módulos sin ninguna prueba: pedido, legal, catálogo y perfil, empezando por los servicios (`*ServicioImpl`), que concentran la lógica.
16. **Redactar `docs/informe-entrega-3.pdf`** (20–30 páginas, las 10 secciones). Debe abrir con la tabla-resumen de observaciones cerradas y su porcentaje, e incluir la matriz de amenazas a la validez —donde la advertencia metodológica del escenario frío encaja perfectamente.
17. **Cerrar las observaciones pendientes con commits que citen `OBS-NN`** (`fix(frontend): unifica version de Angular a 22 en docs y package.json (OBS-02)`), y colocar `v0.7.1` sobre el commit de cierre.
18. **Colección Postman** con ≥ 20 peticiones cubriendo `200/401/403/404/422`.
19. **Structurizr DSL** en `docs/arquitectura/` + **tabla ISO 25010** (prioridad, escenario, estrategia por atributo) → C5 a Excelente.
20. **Corregir la matriz de trazabilidad**: endpoints reales (`/api/v1/...`), rellenar `prueba_automatizada` y `evidencia_empirica` en los `Must`, y alinear la columna `tipo_acceso` con lo que realmente se implemente en el punto 9.
21. **Reparar los cuatro enlaces rotos** de los reportes de mediciones (§3, Bloque C) y **generar `SRS.pdf`**.

---

## 8. Proyección

| Escenario | Nota estimada |
|---|---|
| Estado actual, sin tag | **≈ 8–12 / 100** (C1–C6 anulados por la regla 2) |
| Estado actual, con el tag creado | **36–45 / 100** |
| \+ Prioridad 0 completa | **55–62 / 100** |
| \+ Prioridad 1 completa | **70–78 / 100** |
| \+ Prioridad 2 completa | **88–95 / 100** |

La Prioridad 0 es casi toda trabajo de empaquetado —archivos de metadatos, tags, un Makefile— y por sí sola cambia la calificación de reprobatoria a aprobatoria. Conviene ejecutarla **hoy**, antes de tocar código.

---

*Informe generado por auditoría directa del repositorio en `main@ca8889c`. Cada afirmación de incumplimiento es verificable con `git ls-files`, `git tag -l`, `git log` o inspección del archivo citado.*
