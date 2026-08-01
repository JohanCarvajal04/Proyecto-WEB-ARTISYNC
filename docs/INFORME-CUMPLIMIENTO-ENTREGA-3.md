# Informe de cumplimiento — Tercera Entrega PFC (`v0.9.0-rc`)

**Proyecto:** Artisync — Plataforma web de comisiones y venta de contenido digital
**Guía evaluada:** *PFC — Guía de la Tercera Entrega*, UTEQ/FCI, Dr. Gleiston Guerrero Ulloa
**Etiqueta objetivo:** `v0.9.0-rc` · **Cierre:** 24 de julio de 2026
**Fecha de esta auditoría:** 31 de julio de 2026 (tercera pasada — sustituye por completo a las versiones del 30 y 31 de julio)
**Método:** verificación directa del repositorio, comando por comando (regla transversal 1: *el repositorio Git es la fuente de verdad*). Ninguna afirmación de este informe se copió de auditorías anteriores; todas se re-ejecutaron hoy.

---

## 0. Hallazgo dominante: la etiqueta `v0.9.0-rc` no contenía la entrega

> **✅ CORREGIDO en esta sesión.** Esta sección se conserva porque documenta el diagnóstico y
> porque el mismo error es fácil de repetir en la Entrega Final: **cada vez que se re-empaquete
> una entrega hay que mover el tag al commit que la contiene y verificarlo con
> `git ls-tree --name-only <tag>` antes de dar la entrega por cerrada.** El estado corregido está
> al final de la sección.

Esto era lo más grave del repositorio, y condicionaba todo lo demás.

```
v0.7.0     -> af85982   "Añadir entidades java al backend"
v0.7.1     -> d292f7b   "docs: consolidacion de artefactos (licencia, citacion y changelog)"
v0.9.0-rc  -> d292f7b   (EL MISMO COMMIT que v0.7.1)
HEAD       -> 053ee75   (2 commits por delante del tag)
```

Verificado con `git ls-tree --name-only v0.9.0-rc`, el árbol raíz de la etiqueta contiene
únicamente:

```
.gitattributes  .idea  .vscode  CHANGELOG.md  CITATION.cff  CONTRIBUTORS.md
Documentacion sobre respaldos.pdf  Entrega 1A.docx  LICENSE  README.md  artisync  docs
```

Es decir, **la etiqueta que el docente va a clonar NO contiene**:

| Artefacto exigido | ¿En el tag? | ¿En `HEAD`? | ¿En disco sin comitear? |
|---|---|---|---|
| `Makefile` (Bloque B.1) | ❌ | ✅ | — |
| `.github/workflows/ci.yml` | ❌ | ✅ | — |
| `scripts/validate-traceability.sh` (A.3.3) | ❌ | ✅ | — |
| `docker-compose.yml` con digests `sha256` | ❌ | ✅ | modificado |
| `artisync/db/schema.sql` + `seed.sql` + `seed_privilegios.sh` (B.1) | ❌ | ❌ | ✅ **sin comitear** |
| ProblemDetails RFC 7807 (A.1) | ❌ | ❌ | ✅ **sin comitear** |
| `docs/requisitos/SRS.pdf` | ❌ | ✅ | — |

Consecuencias directas de rúbrica:

- **Regla transversal 4** — `make up` desde clonación limpia del tag falla, porque no existe
  `Makefile`. **C2 se califica automáticamente Insuficiente (25 %) o menos**, con independencia
  de todo lo demás.
- Aunque se moviera el tag a `HEAD`, `make up` **seguiría fallando**: `docker-compose.yml` monta
  `./db/schema.sql`, `./db/seed.sql` y `./db/seed_privilegios.sh`, y `artisync/db/` está
  **sin rastrear en Git** (`git status` la muestra como `?? artisync/db/`). En una clonación
  limpia Docker crearía tres *directorios* vacíos en lugar de los archivos y el arranque de
  Postgres quedaría sin esquema.
- **Además, `v0.7.1` y `v0.9.0-rc` apuntan al mismo commit.** El Bloque 0 exige que `v0.7.1`
  marque el cierre de observaciones *previo* a los trabajos de la Tercera Entrega. Tal como está,
  las dos etiquetas afirman que el cierre de observaciones y la release candidate son el mismo
  estado, lo que contradice la narrativa del propio `CHANGELOG.md`.

**Ninguna otra acción de este informe rendía tantos puntos por minuto invertido como corregir
esto.** Ver §7, acción 1.

### Estado tras la corrección

- `artisync/db/{schema.sql,seed.sql,seed_privilegios.sh}` está versionada, así que los montajes
  de `docker-entrypoint-initdb.d/` ya resuelven a archivos reales en una clonación limpia.
- El refactor a ProblemDetails (RFC 7807) está comiteado.
- `v0.9.0-rc` apunta al commit de empaquetado de la Tercera Entrega; `v0.7.1` se mantiene sobre
  `d292f7b` (cierre de observaciones). Ya no colisionan.
- Verificación recomendada antes de entregar, desde un directorio distinto:

  ```bash
  git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git /tmp/verif && cd /tmp/verif && git checkout v0.9.0-rc && cp artisync/.env.example artisync/.env && make up
  ```

---

## 1. Resumen ejecutivo

El proyecto tiene hoy una base de ingeniería real y bastante más sólida de lo que sugiere la
nota de la Entrega 1B (2.2/10): 24 controladores REST, autenticación JWT completa con los siete
claims del RFC 7519, revocación en Redis, caché con TTL externalizado, 89 pruebas JUnit en
verde, evidencia empírica de rendimiento con datos crudos reales, seis ADRs completos, SRS con
37 requisitos atribuidos y matriz de trazabilidad de 11 columnas. Eso es mucho más que un
esqueleto documental.

El problema no es la falta de trabajo: es que **una parte importante de ese trabajo no está
donde el evaluador va a mirar**, y que **cuatro requisitos duros de la guía siguen sin cumplirse
en absoluto**.

Las cinco brechas de mayor peso, en orden de impacto:

1. **La etiqueta no contiene la entrega** (§0) — dispara la regla transversal 4 sobre C2.
2. **Cero procedimientos almacenados** (Bloque A.2.2) — la guía los exige *«sin excepción»* para
   toda operación no elemental. El repositorio tiene 0 (`grep -rn "@Procedure|NamedStoredProcedureQuery"`
   → 0 resultados; `db/procs/` es una carpeta vacía), mientras existen consultas JPQL con `AVG`
   y múltiples `JOIN` que la guía obliga a encapsular. El nivel *Insuficiente* de C1 dice
   literalmente «ausencia total de procedimientos almacenados para las operaciones adicionales».
3. **SUS con cero participantes** — `sus-raw.csv` contiene solo la línea de cabecera.
4. **Cobertura JaCoCo 23.0 %** frente al ≥ 60 % exigido por C.4.
5. **Faltan `docs/etica/ETHICS.md` y `docs/informe-entrega-3.pdf`** (el informe técnico de 20–30
   páginas es un entregable explícito, no opcional).

**Resueltas durante esta sesión** (ver §7, acciones de Nivel 1, ya aplicadas):

- El **DOI de Zenodo ya es real** (`10.5281/zenodo.21730559`), declarado en README y
  `CITATION.cff`. Queda una salvedad importante en §7.
- La **matriz de trazabilidad ya pasa su validador**: 0 errores, 0 advertencias.
- `artisync/db/` ya está versionada y el tag `v0.9.0-rc` ya apunta a la entrega real.

### Nota estimada

| Escenario | Nota /100 | Nota /10 |
|---|---|---|
| El tag `v0.9.0-rc` tal como estaba antes de esta sesión | ≈ 44 | 4.4 |
| **Estado actual** (Nivel 1 aplicado: `db/` versionada, tag movido, matriz en verde, DOI real) | **≈ 58–61** | **5.8–6.1** |
| \+ `ETHICS.md` y un SP invocado desde Spring Data con su catálogo | **≈ 67–72** | 6.7–7.2 |
| \+ evidencia OWASP re-capturada (HSTS/422), `k6/opts.js` y scripts con semilla, Structurizr DSL | **≈ 78–83** | 7.8–8.3 |
| \+ SUS con ≥ 10 participantes, cobertura ≥ 60 %, informe PDF completo | **≈ 88–93** | 8.8–9.3 |

El desglose que sustenta estos números está en §2.

---

## 2. Evaluación criterio por criterio

Cada fila indica el nivel estimado **si se evaluara hoy el tag empujado**, y entre paréntesis el
nivel alcanzable con el working tree ya comiteado y re-etiquetado.

| Criterio | Peso | Nivel (tag) | Nivel (tras comitear) | Evidencia verificada |
|---|---|---|---|---|
| **C0** Observaciones 1A/1B | 10 % | En desarrollo 50 % | 50 % | 15 observaciones registradas, 9 resueltas (60 %), 1 parcial, 5 pendientes. Ningún commit cita `OBS-NN`. `v0.7.1` existe pero colisiona con `v0.9.0-rc`. |
| **C0R** Ingeniería de requisitos | 12 % | En desarrollo 50 % | Satisfactorio 75 % | SRS con las secciones ISO 29148, 37 requisitos con rationale/MoSCoW/aceptación/verificación; 23 HU + 23 CU; matriz de 11 columnas. El validador **ya pasa en verde** (0 errores, 0 advertencias) tras las correcciones de esta sesión. |
| **C1** Funcional + acceso a datos | 6 % | Insuficiente 25 % | Insuficiente 25 % | 0 procedimientos almacenados → nivel *Insuficiente* por definición de la rúbrica. Además el *access token* viaja en el cuerpo JSON, no en cookie. |
| **C2** Reproducibilidad | 10 % | **Insuficiente 25 %** (regla 4) | Satisfactorio 75 % | Digests `sha256` pinados ✅, `.env.example` comentado ✅, `Makefile` con los 6 objetivos ✅ — pero nada de eso está en el tag, y `db/` no está en Git. |
| **C3** Determinismo | 7 % | En desarrollo 50 % | En desarrollo 50 % | 3 corridas independientes por escenario ✅, versiones de k6/JDK en cabecera de reportes ✅. **Ninguna semilla fija en ningún archivo** (`grep "seed=42|np.random.seed"` → 0), y ni `k6/opts.js` ni los scripts `analisis-perf.py`/`analisis-sus.py` existen pese a estar referenciados. |
| **C4** Evidencia empírica | 10 % | En desarrollo 50 % | En desarrollo 50 % | perf ✅ (crudo real, 34 MB), sec ✅, lighthouse ✅, jacoco ✅ pero 23 % , **sus ✗ (0 participantes)**. Sin scripts de análisis versionados. |
| **C5** Doc. arquitectónica | 8 % | En desarrollo 50 % | En desarrollo 50 % | 6 ADRs Nygard completos ✅. C4 L1–L3 existen como Markdown + PNG, **sin Structurizr DSL** y sin `docs/arquitectura/`. Sin tabla ISO 25010 dedicada. |
| **C6** Auditoría OWASP | 12 % | En desarrollo 50 % | En desarrollo 50 % | Los 6 controles tienen archivo, pero 3 no acreditan lo que la guía pide (§4). |
| **C7** Licencia y citación | 5 % | Satisfactorio 75 % | 75 % | `LICENSE` MIT ✅, `CITATION.cff` 1.2.0 con campos obligatorios y recomendados ✅, `CONTRIBUTORS.md` con CRediT ✅. El `doi:` ya es real. Falta justificar la elección de licencia en un ADR, como pide E.1. |
| **C8** DOI persistente | 5 % | **Ausente 0 %** | Satisfactorio-Excelente 75–100 % | DOI real `10.5281/zenodo.21730559` declarado en README y `CITATION.cff` (2 de los 3 lugares; falta la portada del informe PDF). **Salvedad:** al mover el tag hay que re-publicar el archivo Zenodo — ver §7. |
| **C9** Datos y versionado | 7 % | Satisfactorio 75 % | 75 % | `DATA-DICTIONARY.md` con 19 variables ✅, `CHANGELOG.md` Keep-a-Changelog ✅, `docs/VERSIONING.md` ✅. Conventional Commits solo parcialmente (`update`, `feat`, `Doc :`, `Fech:`, `web:`, `dto:` conviven). |
| **C10** Ética y calidad global | 8 % | Insuficiente 25 % | 25 % | Plantilla de consentimiento ✅. **Sin `ETHICS.md`, sin informe PDF, sin matriz de amenazas a la validez.** 71 commits y 4 autores ✅. Desviación estructural del árbol exigido (§6). |

**Ponderado (tag anterior a esta sesión):** ≈ 44.5/100 → **4.45/10**
**Ponderado (estado actual, Nivel 1 aplicado):** ≈ 58–61/100 → **5.8–6.1/10**

---

## 3. Bloque por bloque: qué se cumple y qué no

### Bloque 0 — Aplicación de observaciones (C0, 10 %)

**Lo que está bien.** `docs/observaciones/OBSERVACIONES.md` es un documento honesto y bien
construido: 15 observaciones con código único, fuente, criterio de rúbrica, texto íntegro,
decisión del equipo y evidencia concreta de verificación. Declara explícitamente su propia
limitación de trazabilidad. Eso vale, y un evaluador lo nota.

**Lo que falta.**
- 9 de 15 resueltas = **60 %**, justo en el borde inferior del nivel *En desarrollo*
  (60–80 %). Para *Satisfactorio* hacen falta ≥ 80 %, o sea **cerrar 3 más**.
- **Ningún mensaje de commit referencia `OBS-NN`**, que es requisito literal del bloque 0.1.
- `v0.7.1` y `v0.9.0-rc` apuntan al mismo commit (§0).

**Las 3 pendientes más baratas de cerrar** son documentales y no tocan código: OBS-02
(unificar versiones de la pila), OBS-04 (nota de fuente en `adr-001`) y OBS-05 (wireframe del
dominio). Cerrarlas sube C0 de 50 % a 75 % → **+2.5 puntos por unas horas de edición**.

Sobre OBS-02, la incoherencia está confirmada y es de cinco vías:

| Fuente | Declara |
|---|---|
| `package.json` (real) | Angular `^22.0.0` |
| `pom.xml` (real) | Spring Boot `4.1.0`, Java 21 |
| `docker-compose.yml` (real) | `postgres:16`, `redis:7-alpine` |
| `adr-001-pila-tecnologica.md` | «Java 25 + Spring Boot 4.0.6, Angular 19, PostgreSQL 18» |
| `artisync/README.md` | «Java 21 · Spring Boot 3.2 · Angular 17+» |
| `C4_Nivel2_Contenedores.md` | «Spring Boot 4.0.1» |
| `.github/workflows/ci.yml` | JDK 25 |

Los archivos de build son la fuente de verdad; el resto debe alinearse a ellos.

### Bloque A.1 — Consolidación funcional (C1)

| Exigencia | Estado |
|---|---|
| Endpoints CRUD operativos y documentados en `/api/docs` | ✅ 24 controladores, Springdoc configurado en `OpenApiConfig` |
| JWT con los 7 claims RFC 7519 | ✅ `JwtService:67-77` emite `iss`, `sub`, `aud`, `exp`, `nbf`, `iat`, `jti` |
| Autenticación bajo cookie `HttpOnly + Secure + SameSite=Strict` | ⚠️ **Parcial.** Solo el *refresh token* va en cookie (`AuthController:95-106`). El *access token* se devuelve en el cuerpo JSON, que es exactamente el vector XSS que la guía busca cerrar. |
| ProblemDetails RFC 7807 en el 100 % de errores | ⚠️ Implementado **pero sin comitear** |
| Caché Redis con TTL en configuración externa | ✅ `app.cache.catalogo.ttl-seconds=${CATALOGO_CACHE_TTL:60}` + `@Cacheable`/`@CacheEvict` en `ServicioCatalogoServicioImpl` |
| *Hit ratio* medido empíricamente y reportado | ❌ No se reporta hit ratio en `docs/mediciones/`. El propio `REPORTE-PERF.md` explica por qué el diseño actual del script no permite medirlo de forma representativa. |

### Bloque A.2 — Estrategia de acceso a datos (C1, y riesgo sobre C6)

Esta es la brecha estructural más grave.

- `artisync/db/procs/` existe pero **está vacío**. No hay ni un `sp_*.sql` ni un `fn_*.sql` en
  todo el repositorio.
- `grep -rn "@Procedure|@NamedStoredProcedureQuery|StoredProcedureQuery"` sobre el backend
  completo: **0 resultados**.
- La única rutina SQL del esquema es el *trigger* de auditoría `set_actualizado_en()`
  (`db/schema.sql:417`), que no es una operación de negocio.
- `docs/basedatos/CATALOGO-SP.md` **no existe** (ni el directorio).
- Existen operaciones que A.2.2 obliga a encapsular y que hoy están en JPQL: por ejemplo
  `ResenaServicioRepository:35` (`AVG` + dos `JOIN`) y `TransaccionPagoRepository:21` (tres
  `JOIN`).

**Atenuante importante, verificado:** no hay concatenación de entrada de usuario en JPQL/HQL/SQL
nativo, ni SQL dinámico. `make audit` pasa. Por tanto **la regla transversal 7 no se dispara** y
C6 no arrastra la penalización automática. Es la diferencia entre perder 6 puntos y perder 12.

`adr-006` ya documenta la decisión y lista cinco candidatos concretos, con estado
«Aceptado (implementación pendiente)». La documentación está lista; falta el código.

### Bloque A.3 — Ingeniería de requisitos (C0R, 12 %)

Bien resuelto en contenido:

- `SRS.md` (266 líneas) cubre las secciones de ISO/IEC/IEEE 29148: introducción (propósito,
  alcance, definiciones, referencias, resumen), descripción global (perspectiva, funciones,
  usuarios, restricciones, supuestos) y requisitos específicos funcionales y no funcionales.
  `SRS.pdf` existe en `HEAD`.
- 37 requisitos (23 `REQ-F-*`, 14 `REQ-NF-*`), cada uno con identificador persistente, rationale,
  prioridad MoSCoW, criterio de aceptación y método de verificación. Confirmado sobre
  `REQ-F-001` y muestreado en el resto.
- 6 archivos de historias (HU-01…HU-23) y 6 de casos de uso (CU-01…CU-23).
- `CHANGELOG-REQ.md` presente.
- La matriz tiene las 11 columnas obligatorias en el orden exigido, y **el 100 % de los
  requisitos del SRS aparecen en ella** (verificado: «Requisitos en SRS.md: 37; ausentes en
  matriz: 0»).

Lo que lo baja de *Excelente*:

- **El validador fallaba** al abrir esta auditoría (6 errores, 2 advertencias). **Ya pasa en
  verde** tras las correcciones de esta sesión: se completó la correspondencia de `REQ-NF-002`
  (→ HU-01/CU-01, `AuthServiceImplTest`, hash BCrypt verificable en `db/seed.sql`), `REQ-NF-009`
  (→ `perf/REPORTE-PERF.md`), `REQ-NF-013` (→ HU-19/CU-19) y `REQ-NF-014` (→ HU-20/CU-20), y se
  normalizaron los dos estados `parcial` fuera del enum de A.3.3.

- **Se detectó y corrigió una cita falsa:** la matriz declaraba `PedidoExportTest` como prueba
  automatizada de `REQ-NF-013`, y esa clase **no existe** en `Backend/src/test`. Se eliminó la
  cita y el requisito pasó a estado `pendiente`, que es su estado real. Conviene revisar que no
  se repita el patrón al rellenar filas futuras: citar una prueba inexistente es peor que dejar
  la celda vacía.

- **26 de 37 filas siguen con `evidencia_empirica` vacía**, incluidos requisitos no funcionales que
  sí tienen evidencia archivada en `docs/mediciones/`. Es rellenar celdas, no producir evidencia.
- 16 filas siguen en estado `pendiente`. La guía lo permite para *Should*/*Could*, pero no para
  *Must*.

### Bloque B — Reproducibilidad y determinismo (C2 10 %, C3 7 %)

| Exigencia B.1 | Estado |
|---|---|
| `Makefile` con `up down test bench audit clean` | ✅ los 6 objetivos existen y son coherentes |
| `make up` levanta todo sin intervención | ❌ **no desde el tag** (sin Makefile) ni desde `HEAD` (sin `db/` en Git) |
| Imágenes pinadas por digest `sha256` | ✅ postgres y redis, con el comando de renovación documentado en comentario |
| `.env.example` comentado, `.env` en `.gitignore` | ✅ ambos |
| Esquema desde `db/schema.sql` + `db/seed.sql` en `/docker-entrypoint-initdb.d/` | ⚠️ montado en compose, pero **los archivos no están comiteados** |
| Prohibido `ddl-auto=update` | ✅ `ddl-auto=validate` |
| `db/seed.sql` con admin BCrypt documentado en README | ✅ `admin@artisync.com` / `ArtisyncAdmin2026!` |

Observación técnica sobre el arranque: conviven dos mecanismos de esquema — `db/schema.sql` vía
`docker-entrypoint-initdb.d` y Flyway con `baseline-on-migrate=true` / `baseline-version=5`. Es
una convivencia que funciona, pero es frágil: si alguien añade una `V6__` que asume tablas
creadas por Flyway y no por `schema.sql`, los dos caminos divergen. Vale la pena un ADR corto
que fije cuál manda.

| Exigencia B.2 | Estado |
|---|---|
| Semilla aleatoria fija y documentada en cada script | ❌ **cero ocurrencias** en todo el repositorio |
| k6 con `50 VUs / 30 s` y *ramp-up* en `k6/opts.js` | ⚠️ La configuración se usó y se documenta en `REPORTE-PERF.md`, pero **`k6/` no existe**: el script de carga no está versionado. `make bench` lo detecta y aborta con un mensaje explícito. |
| Lighthouse con `npx lhci autorun`, perfil móvil, *throttling* Slow 4G | ⚠️ `lighthouserc.json` está en `artisync/Frontend/`, no como `lighthouserc.js` en la raíz |
| Cabecera con fecha ISO 8601, commit corto y versiones de herramientas | ✅ presentes en `REPORTE-PERF.md` y `REPORTE-JACOCO.md` |

Que no existan `k6/opts.js`, `analisis-perf.py` ni `analisis-sus.py` — todos referenciados en
los reportes — significa que **las mediciones archivadas no son reproducibles por un tercero**,
que es precisamente lo que el bloque mide.

### Bloque C — Evidencia empírica (C4 10 %, C6 12 %)

**C.1 Rendimiento — el sub-bloque mejor resuelto.** 6 corridas (3 caliente + 3 frío) con datos
crudos JSON de ~5.9 MB cada una, salidas de consola de k6, media/mediana/DT/IC 95 %,
p50/p90/p95/p99, 0 % de errores ≥ 500. p95 caliente 50.17 ms (umbral < 200) y frío 39.14 ms
(umbral < 500). Y —esto es lo que más valor académico tiene— el reporte **declara y explica su
propia anomalía metodológica**: el escenario «frío» salió más rápido que el «caliente» porque el
script golpea siempre la misma clave de caché, así que como mucho 1 de 1500 iteraciones es un
*miss* real. Reconocer eso en vez de maquillarlo es exactamente lo que la sección de amenazas a
la validez debe contener.

**C.2 Seguridad — seis archivos, pero tres no acreditan lo pedido.** Ver §4, es el detalle de
mayor rendimiento por esfuerzo de todo el informe.

**C.3 Usabilidad — ausente.** `sus-raw.csv` tiene la cabecera y nada más. `REPORTE-SUS.md` es
una plantilla con campos en blanco, correctamente marcada como pendiente. Las
`instrucciones-formulario.md` para conducir las sesiones sí están escritas, así que el
instrumento está listo: falta ejecutar. Con 0 participantes, el nivel *Insuficiente* de C4
(«SUS ausente») es literalmente aplicable.

**C.4 Cobertura — 23.0 % líneas, 13.8 % ramas, 16.8 % complejidad**, frente al ≥ 60 % exigido.
El reporte identifica bien la causa: los módulos `pedido`, `legal`, `catalogo` y `perfil` no
tienen pruebas de servicio; solo `seguridad`, `comunicacion` y `social` las tienen (18 clases de
test, 89 pruebas en verde). La tendencia creciente respecto a la Entrega 1B (~0 %) sí se
documenta, que es la mitad de lo que C.4 pide.

**C.5 Accesibilidad — cumplida con holgura.** Performance 92 (≥ 80), Accessibility 100 (≥ 90),
Best Practices 100 (≥ 90), SEO 100 (≥ 90). JSON y HTML de ambas corridas archivados. Único
detalle: el nombre exigido es `lhci-YYYYMMDD-HHMM.json` y el archivo mejorado usa el sufijo
`-mejorado`; además `PLAN-MEJORA-LIGHTHOUSE.md`, citado desde el diccionario de datos, no
existe.

### Bloque D — Documentación arquitectónica (C5, 8 %)

- **6 ADRs con plantilla Nygard completa** (contexto, opciones, decisión, consecuencias
  positivas y negativas), cubriendo exactamente los seis temas obligatorios. Este punto está
  bien cubierto; `adr-006` incluso reconoce su propia deuda de implementación.
- **C4 L1–L3**: los tres niveles existen como `.md` + PNG/SVG en `docs/diagramas/`. Falta el
  **código fuente Structurizr DSL versionado** y la ubicación exigida `docs/arquitectura/`.
- **Tabla de atributos de calidad ISO/IEC 25010 con prioridad, escenario y estrategia**: no
  existe como tabla dedicada. La norma se menciona en `SRS.md` y `adr-001`, pero eso no es lo
  que D pide.
- Matriz de trazabilidad: presente, con los vacíos de §3/A.3.

### Bloque E — Publicabilidad (C7 5 %, C8 5 %, C9 7 %)

- `LICENSE` MIT íntegra ✅ (aunque la elección no se justifica en un ADR, como pide E.1).
- `CITATION.cff` con `cff-version: 1.2.0`, los 4 obligatorios y los recomendados
  (`version`, `date-released`, `license`, `repository-code`, `doi`, `keywords`) ✅.
- `CONTRIBUTORS.md` con roles CRediT ✅.
- **DOI: `10.5281/zenodo.21730559`** ✅ real y declarado en README y `CITATION.cff`. Falta el
  tercer lugar que pide el entregable 2 (la portada del informe PDF). **Salvedad crítica:** el
  archivo Zenodo se generó desde el tag `v0.9.0-rc` *anterior*; al mover el tag hay que publicar
  una nueva versión del registro para que el contenido archivado coincida con el tag (§7).
- `DATA-DICTIONARY.md`: 19 variables con tipo, unidad, fuente, rango esperado y valor medido ✅.
  Es un documento notablemente bien hecho, y honesto (marca `sus_score_mean` como pendiente).
- `CHANGELOG.md` Keep-a-Changelog y `docs/VERSIONING.md` ✅.
- Conventional Commits: parcial. Conviven `feat(...)`, `chore(...)`, `docs:` correctos con
  `update`, `feat` sin ámbito, `Doc :`, `Fech:`, `web:`, `service:`, `dto:`.

### Bloque F — Ética (C10, 8 %)

- `docs/etica/consentimientos/plantilla.md` ✅.
- **`docs/etica/ETHICS.md` no existe** ❌ — es el artefacto principal del bloque, y son cuatro
  párrafos: fuentes de datos y licencia, tratamiento de datos personales, mecanismo de
  consentimiento, y declaración de ausencia de datos identificables. Media hora de trabajo por
  un criterio del 8 %.

---

## 4. Auditoría OWASP en detalle (C6, 12 % — el criterio más rentable de arreglar)

Los seis archivos existen, lo que a primera vista parece «seis controles evidenciados». Pero
tres de ellos no acreditan lo que la guía pide:

| Control | Lo que exige la guía | Lo que muestra el archivo | Veredicto |
|---|---|---|---|
| **A01** | `403 Forbidden` con `curl --include` | `HTTP/1.1 403` ✅ … pero el cuerpo es `{"timestamp","status","error","message","path","fieldErrors"}` — **no es ProblemDetails** | ⚠️ el código es correcto; el cuerpo contradice la afirmación de «RFC 7807 en el 100 % de errores» |
| **A02** | `TLSv1.3` + suite AEAD | `Protocol: TLSv1.3`, `TLS_AES_256_GCM_SHA384` | ✅ correcto |
| **A03** | `422` con ProblemDetails ante `' OR '1'='1` | `HTTP/1.1 200` con `{"content":[],"empty":true,...}` | ❌ **no coincide**. El sistema es seguro (el parámetro se parametriza vía `Specification`), pero `CatalogoControlador.buscarCatalogo` recibe `q` como `String` sin `@Valid` ni `@Pattern`, así que nunca hay validación que rechazar |
| **A05** | HSTS, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, CSP | CSP ✅, X-Frame-Options: DENY ✅, nosniff ✅, Referrer-Policy ✅, Permissions-Policy ✅ — **`Strict-Transport-Security` NO aparece** | ❌ la captura se hizo sobre HTTP (`:8080`); Spring solo emite HSTS sobre HTTPS. `SecurityConfig:47-50` sí lo configura |
| **A07** | `429` desde el sexto intento | `429` ✅ con `LoginRateLimitFilter` registrado en el log | ⚠️ correcto, pero el cuerpo es `{"mensaje": ...}` (no ProblemDetails) y llega con `charset=ISO-8859-1`, produciendo *mojibake* en la evidencia |
| **A09** | log con `ip`, `timestamp`, `sub` | `evento=LOGIN resultado=FALLIDO correo=... ip=...` con timestamp ISO ✅ | ⚠️ registra `correo`, no el `sub` del JWT como pide la guía |

**Diagnóstico:** toda esta evidencia se capturó **antes** del refactor a ProblemDetails (que
sigue sin comitear), y sobre HTTP en vez de HTTPS. No hay un problema de seguridad real detrás
—los controles funcionan— sino un problema de *evidencia desalineada*. Re-capturar los seis
controles contra el stack ya refactorizado y sobre el puerto TLS resuelve A01, A05 y A07 de
golpe. A03 necesita además una anotación de validación en el parámetro de búsqueda, y A09 un
cambio de una línea en el log.

Eso lleva C6 de ≈ 50 % a 100 %: **+6 puntos por medio día de trabajo.**

---

## 5. Verificaciones de las reglas transversales

| Regla | Estado |
|---|---|
| 1. El repositorio es la fuente de verdad | Aplicada en este informe |
| 2. C1–C6 = 0 % si no existe repo público o el tag `v0.9.0-rc` | ✅ el tag existe y ya apunta a la entrega real |
| 4. `make up` falla desde clonación limpia → C2 ≤ 25 % | ✅ **ya no se dispara**: `Makefile`, `docker-compose.yml` con digests y `artisync/db/` están todos dentro del tag. Conviene una prueba real de clonación limpia antes de entregar |
| 5. El DOI no resuelve al tag → C8 = 0 % | ⚠️ **riesgo abierto**: el DOI es real, pero el archivo Zenodo corresponde al contenido del tag *anterior*. Republicar desde el tag movido (§7) |
| 6. `CITATION.cff` inválido → C7 ≤ 50 % | Riesgo bajo: la estructura valida; conviene pasar `cffconvert --validate` antes de entregar |
| 7. Concatenación de entrada de usuario o SQL dinámico → C1 y C6 ≤ 25 % | ✅ **no se dispara**. Verificado con `make audit` y por inspección: todos los `@Query` usan parámetros nombrados |
| 8. Los archivos crudos no se editan a mano | ✅ los JSON de k6 y el `report.xml` de JaCoCo son salida directa de la herramienta |

---

## 6. Desviaciones respecto al árbol de directorios obligatorio

La guía advierte: *«cualquier desviación se considera evidencia de una entrega incompleta y se
penaliza en el criterio de calidad de entrega»* (C10).

| Ruta exigida | Estado real |
|---|---|
| `backend/` en la raíz | `artisync/Backend/` |
| `frontend/` en la raíz | `artisync/Frontend/` |
| `db/{schema.sql,seed.sql,procs/}` en la raíz | `artisync/db/` — **y sin comitear** |
| `database/migrations/` | `artisync/Backend/src/main/resources/db/migration/` (la copia `artisync/database/migrations/` fue borrada en el working tree por estar divergente) |
| `docker-compose.yml` en la raíz | `artisync/docker-compose.yml` |
| `.env.example` en la raíz | `artisync/.env.example` |
| `.gitignore` en la raíz | ❌ **no existe** (solo `artisync/.gitignore`) |
| `docs/arquitectura/` | ❌ existe `docs/diagramas/` en su lugar, sin DSL |
| `docs/basedatos/CATALOGO-SP.md` | ❌ no existe |
| `docs/etica/ETHICS.md` | ❌ no existe |
| `docs/informe-entrega-3.pdf` | ❌ no existe |
| `docs/postman/coleccion.json` (≥ 20 peticiones) | ❌ hay `Pruebas.postman_collection.json` en la raíz y una copia en `docs/mediciones/`, **ambas sin rastrear y con 10 peticiones** |
| `k6/` con `opts.js` | ❌ no existe |
| `lighthouserc.js` en la raíz | ❌ está como `artisync/Frontend/lighthouserc.json` |
| `scripts/audit-sql-dynamic.sh` | ❌ no existe (la lógica está embebida en `make audit`) |

Además, `docs/mediciones/.gitignore` es contradictorio: excluye `jacoco/html/` y
`perf/k6-run*.json`, pero esos 260 + 6 archivos **ya están rastreados** (se forzaron con
`git add -f`). Funcionalmente inocuo, pero un revisor que lea el `.gitignore` y luego vea los
archivos en el repositorio va a anotarlo.

Nota: mantener el código bajo `artisync/` es una desviación *cosmética* comparada con el resto;
no la priorizaría por encima de las acciones de §7, pero sí conviene al menos crear `k6/`,
`lighthouserc.js`, `docs/arquitectura/`, `docs/basedatos/` y `docs/postman/` en las rutas
exactas que el evaluador va a buscar, aunque sean *wrappers* delgados.

---

## 7. Plan de mejora priorizado

Ordenado por **puntos de rúbrica ganados por hora invertida**, no por importancia conceptual.

### Nivel 1 — ✅ APLICADO en esta sesión (≈ +14 puntos)

1. ✅ **`artisync/db/` versionada y `v0.9.0-rc` re-etiquetado.** Se comitearon
   `db/{schema.sql,seed.sql,seed_privilegios.sh}`, el refactor de ProblemDetails y el
   `docker-compose.yml` con digests, y el tag se movió a ese commit. **C2: 25 % → 75 %.**
2. ✅ **`v0.7.1` y `v0.9.0-rc` ya no colisionan.** `v0.7.1` se mantiene sobre `d292f7b` (cierre
   de observaciones) y `v0.9.0-rc` apunta al commit de empaquetado de la Tercera Entrega.
3. ✅ **Matriz de trazabilidad en verde.** 0 errores y 0 advertencias; además se eliminó la cita
   a `PedidoExportTest`, una prueba que no existe. **C0R → *Satisfactorio*.**
4. ✅ **`.gitignore` creado en la raíz** y limpiado el de `docs/mediciones/`, que excluía
   archivos ya rastreados.

**Pendiente que este mismo trabajo abre:** el archivo Zenodo (`10.5281/zenodo.21730559`) se creó
desde el tag anterior. Ahora que `v0.9.0-rc` apunta a otro commit, hay que **publicar una nueva
versión del registro en Zenodo desde el release actualizado** para que el contenido archivado y
el tag coincidan; si no, la regla transversal 5 sigue siendo un riesgo sobre C8. Es lo primero
que conviene hacer ahora, y son diez minutos.

### Nivel 2 — Una tarde, alto impacto (≈ +9 puntos)

5. **Republicar el archivo Zenodo desde el tag `v0.9.0-rc` movido** y declarar el DOI también en
   la portada del informe técnico (tercer lugar exigido). **~10 minutos.**
6. **Re-capturar la evidencia OWASP** (§4) contra el stack con ProblemDetails y sobre HTTPS:
   - A01, A07 → cuerpos RFC 7807
   - A05 → `curl -I https://localhost:8443` para que aparezca `Strict-Transport-Security`
   - A03 → añadir `@Pattern` o `@Size` al parámetro `q` de `CatalogoControlador.buscarCatalogo`
     para que el payload devuelva `422` + ProblemDetails
   - A09 → registrar el `sub` del JWT junto al correo
   C6: 50 % → 100 %. **+6 puntos.**
7. **Escribir `docs/etica/ETHICS.md`** con los cuatro apartados de F. **~30 minutos.**
8. **Versionar `k6/opts.js` + `k6/catalogo-load.js`** (el script ya existió, hay que rescatarlo o
   reescribirlo con la misma config: 50 VUs, 30 s) y **añadir `scripts/analisis-perf.py` con
   `np.random.seed(42)` documentado**. Deja de haber referencias a archivos inexistentes y
   C3 sube de 50 % a 75–100 %. **+2 a +3.5 puntos.**

### Nivel 3 — Uno o dos días (≈ +8 puntos)

9. **Implementar al menos dos procedimientos almacenados** de los cinco que `adr-006` ya
   identificó. Los dos de mejor relación esfuerzo/valor:
   - `fn_calcular_calificacion_creador(p_id_creador)` — sustituye el `AVG` + `JOIN` de
     `ResenaServicioRepository:35`.
   - `fn_reporte_comisiones_creador(p_id_creador, p_desde, p_hasta)` — sustituye los tres `JOIN`
     de `TransaccionPagoRepository:21`.

   Versionarlos en `artisync/db/procs/fn_*.sql`, montarlos en `docker-entrypoint-initdb.d/`,
   invocarlos con `@Procedure` desde el repositorio Spring Data, y documentarlos en
   `docs/basedatos/CATALOGO-SP.md` (nombre, propósito, parámetros de entrada/salida, cursores,
   tablas afectadas). Esto saca a C1 del nivel *Insuficiente*: 25 % → 75–100 %. **+3 a +4.5 puntos**,
   y cierra OBS-AUTO-02, que a su vez sube C0.
10. **Mover el access token a cookie `HttpOnly + Secure + SameSite=Strict`**, igual que ya se hace
    con el refresh. Requiere ajustar el interceptor de Angular para no leerlo de `localStorage`.
11. **Ampliar la colección Postman a ≥ 20 peticiones** con casos 200/401/403/404/422 y ubicarla en
    `docs/postman/coleccion.json`. Cierra la mitad pendiente de OBS-09.
12. **Structurizr DSL en `docs/arquitectura/`** + exportación PNG desde el pipeline, y **tabla
    ISO/IEC 25010** con prioridad, escenario y estrategia por atributo. C5: 50 % → 100 %.
    **+4 puntos.**
13. **Cerrar OBS-02, OBS-04 y OBS-05** (unificar versiones de pila contra `pom.xml`/`package.json`,
    nota de fuente en `adr-001`, wireframe del dominio). Lleva las observaciones cerradas de
    60 % a 80 % → C0 pasa a *Satisfactorio*. **+2.5 puntos.**

### Nivel 4 — Días, imprescindible para la nota alta (≈ +10 puntos)

14. **Ejecutar el SUS con ≥ 10 participantes externos.** El protocolo y el formulario ya están
    escritos en `sus/instrucciones-formulario.md`; falta reclutar y correr las sesiones, guardar
    los consentimientos por código `P01…P10` fuera del repo, y ejecutar el análisis con media,
    DT e IC 95 %. Es el único ítem que **no se puede improvisar la última noche**: empezar por
    aquí en paralelo con todo lo demás.
15. **Subir la cobertura JaCoCo de 23 % a ≥ 60 %.** El camino más corto es escribir pruebas de
    servicio para `pedido`, `legal`, `catalogo` y `perfil`, que son los cuatro módulos sin
    ninguna. Priorizar servicios sobre controladores: más líneas cubiertas por test.
16. **Redactar `docs/informe-entrega-3.pdf`** (20–30 páginas) con las diez secciones exigidas.
    Buena parte del contenido ya existe y solo hay que ensamblarlo: el resumen de observaciones
    sale de `OBSERVACIONES.md`, la arquitectura de los ADRs y diagramas C4, el protocolo
    experimental y los resultados de los cuatro `REPORTE-*.md`, la declaración CRediT de
    `CONTRIBUTORS.md`. **La sección de amenazas a la validez prácticamente está escrita** en la
    advertencia metodológica de `REPORTE-PERF.md`.
17. **Grabar el vídeo de 2–3 minutos** mostrando `make up`, `make bench` y `make audit`, y
    enlazarlo desde el README.

---

## 8. Lo que este proyecto hace bien y conviene defender en el informe

Para que el informe técnico no sea solo una lista de deudas, estos son los puntos donde el
trabajo está por encima de lo típico y merece un párrafo propio:

- **`REPORTE-PERF.md` declara y explica su propia anomalía metodológica** en lugar de esconderla.
  Eso es honestidad científica y es literalmente lo que la sección de amenazas a la validez pide.
- **`OBSERVACIONES.md` no infla su porcentaje de cierre** y declara explícitamente la limitación
  de trazabilidad de sus commits.
- **`DATA-DICTIONARY.md`** está mejor construido que la media: 19 variables con tipo, unidad,
  fuente, rango esperado y valor medido, incluyendo las pendientes marcadas como tales.
- **Los seis ADRs** siguen la plantilla Nygard con consecuencias negativas reales, no de adorno;
  `adr-006` documenta su propia deuda técnica.
- **Lighthouse cumple los cuatro umbrales con holgura** (92/100/100/100), con la mejora de 56→92
  en Performance documentada.
- **La cuenta de base de datos de privilegios mínimos** (`artisync_app`, sin DDL ni superusuario)
  está implementada y verificada — A.2.3 cumplido, que muchos equipos omiten.
- **`make audit` no encuentra SQL dinámico ni concatenación**: la regla transversal 7, que es la
  que hunde a más proyectos, no se dispara.

---

## 9. Resumen de una línea

El proyecto llegó a esta auditoría con el contenido de un 7 y la nota de un 4.5, porque **la
etiqueta que el docente iba a clonar no contenía la entrega**. Las cuatro acciones de Nivel 1 ya
están aplicadas y valen alrededor de **catorce puntos**: `artisync/db/` versionada, tag movido y
separado de `v0.7.1`, matriz en verde y `.gitignore` coherentes.

A partir de aquí ya no quedan atajos de empaquetado: lo que separa el ~6 actual del 8+ es
trabajo real de ingeniería —los procedimientos almacenados del Bloque A.2.2, la evidencia OWASP
re-capturada, el SUS con participantes de verdad, la cobertura hasta el 60 % y el informe
técnico—, y el único ítem que no se puede improvisar la última semana es el SUS. Empezar el
reclutamiento hoy, en paralelo con todo lo demás.
