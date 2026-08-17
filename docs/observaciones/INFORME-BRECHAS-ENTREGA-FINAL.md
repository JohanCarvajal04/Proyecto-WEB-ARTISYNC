# Informe de brechas — Entrega Final (v1.0.0)

**Fecha del informe:** 2026-08-16
**Referencia:** Guía de la Entrega Final (Cuarta Entrega), PPA 2026-2027, Aplicaciones Web — Quinto nivel, UTEQ-FCI.
**Objetivo:** documentar, bloque por bloque, qué requisitos de la guía ya están cubiertos por el repositorio y cuáles faltan, con evidencia verificable (ruta de archivo, contenido leído, comandos ejecutados). No es un plan de implementación; es un diagnóstico.

## Actualización — 17 de agosto de 2026 (tras 9 commits)

Desde el informe original (2026-08-16, commit `6f8c09c`) se hicieron 9 commits nuevos que cierran una parte real de las brechas de abajo. Esta sección se re-auditó desde cero contra el estado actual del repositorio (`HEAD` en `6af8595` al momento de escribir esto) — no se asume nada del informe original sin volver a verificar.

### Lo que se cerró de verdad

| Ítem | Commit | Evidencia |
|---|---|---|
| Lighthouse desktop + 3 corridas por perfil | `69e43b8` | 12 reportes reales archivados en `docs/mediciones/lighthouse/`; mobile 80-81/93/96/100, desktop 100/93/96/100, ambos dentro de umbral |
| ZAP baseline + SpotBugs/find-sec-bugs | `8b88512` | ZAP: 0 FAIL/8 WARN/59 PASS; SpotBugs: 0 hallazgos de inyección SQL; ambos con reporte real archivado |
| Procedimientos almacenados conectados al código | `f7c2ecd` | 7 rutinas nuevas (`fn_registrar_usuario`, `fn_resolver_estado_login`, `fn_sincronizar_permisos_rol`, `fn_eliminar_rol`, `fn_registrar_infraccion`, `fn_restablecer_contrasena`, `fn_seleccionar_ganadores_sorteo`) verificadas conectadas end-to-end vía `@Query(nativeQuery=true)` a `AuthServiceImpl`, `RolePermissionServiceImpl`, `InfraccionServiceImpl` y `SorteoScheduler`. `R__procedimientos.sql` (1306 líneas, 13 funciones) ahora existe y `db/procs/` está versionado (ya no gitignored) |
| Evidencia OWASP reorganizada + DATA-PROVENANCE.md | `5308153` | Los 6 `.txt` de A01-A09 movidos a `sec/owasp/`; nuevo `DATA-PROVENANCE.md` con narrativa por métrica |
| Colección Postman ampliada | `367122c` | 10 → **26 peticiones**, con casos 400 (validación), 401/403 (autorización) y 404 explícitos — cumple el umbral ≥25 de la guía |
| Marcadores de merge sin resolver en `.env.example` | `d4d3dc0` | Verificado: cero ocurrencias de `<<<<<<<`/`=======`/`>>>>>>>` |
| Emulador azurite en `docker-compose.yml` | `6af8595` | Servicio bien formado, gateado por `profiles: ["azure"]`, conectado a `AlmacenamientoAzureIntegracionTest.java` |

### Brechas nuevas encontradas en esta re-auditoría (no estaban en el informe original)

1. **Los checklists de Bloque E están casi vacíos, pese a que el commit dice "completados".** El commit `b52451d` dice textualmente "Se agregan los cuatro checklists **completados**". Verificado con `grep` de checkboxes: **cero** casillas marcadas (`[x]`) en los cuatro archivos.
   - `ralph-2021-checklist.md`: plantilla sin responder, sin declarar tipo de estudio empírico, sin evidencia enlazada.
   - `fair-checklist.md`: plantilla sin responder, solo repite las preguntas.
   - `incose-checklist.md`: plantilla sin responder y además **no cubre la taxonomía completa** C1–C15 de INCOSE v4 (cubre ~6 de 9 características individuales, ~4 de 6 de conjunto, sin nombrarlas explícitamente ni citar ningún REQ-F concreto).
   - `prisma-2020-checklist.md`: **este sí está bien resuelto** — declara correctamente "No Aplica" con justificación verificada (el proyecto no tiene una revisión sistemática de literatura).
   - Conclusión: 1 de 4 checklists está realmente terminado. Los otros 3 necesitan trabajo real, no solo existir como archivo.

2. **`docs/trazabilidad/matriz.csv` no se actualizó junto con el código.** Los requisitos que ahora usan las 7 rutinas nuevas (REQ-F-001, REQ-F-002, REQ-F-004, REQ-F-015, REQ-F-023, entre otros) siguen marcados `CRUD-ORM` en la columna `tipo_acceso`, no `SP`. Solo 2 filas (no relacionadas con este cambio) dicen `SP`.

3. **`docs/observaciones/OBSERVACIONES.md` no se tocó.** Sigue fechado el 31-07-2026, con el mismo 68% de resolución que el informe original. Ninguno de los 9 commits de hoy quedó registrado ahí. En particular, **OBS-AUTO-02** (la observación específica sobre procedimientos almacenados no conectados) sigue diciendo "Pendiente" con evidencia que ahora es falsa (afirma 0 resultados de `@Procedure`, cuando hoy hay 7 rutinas conectadas). Debería pasar a "Parcialmente resuelta" (7 de 13 rutinas conectadas; las 6 originales de la Tercera Entrega siguen sin conectar, admitido honestamente en el propio ADR-006).

4. **`docs/basedatos/CATALOGO-SP.md` se contradice a sí mismo.** En dos lugares dice "ocho" rutinas nuevas de la ampliación, en otro dice "siete" (el número correcto, que coincide con ADR-006 y con lo verificado en el código). Es un desliz de conteo, no un problema de fondo, pero conviene corregirlo.

5. **Afirmaciones de CI que no son ciertas.** `db/procs/README.md` y comentarios en `scripts/sync-procs.sh`/`scripts/audit-sql-dynamic.sh` dicen que el pipeline de CI ejecuta `sync-procs.sh --check` y `audit-sql-dynamic.sh`. Verificado contra `.github/workflows/ci.yml` actual: **no ejecuta ninguno de los dos**, ni tampoco SpotBugs, ZAP o Lighthouse. Toda esta tooling nueva es solo local (`make audit`, `make audit-zap`, `make lighthouse`), consistente con la decisión ya tomada de no tocar CI por el plazo, pero la documentación interna no debería afirmar lo contrario.

6. **`docs/mediciones/DATA-DICTIONARY.md` sigue con las cifras de JaCoCo obsoletas.** El commit `5308153` tocó este archivo (para las rutas de OWASP y el dato de SUS) pero **no corrigió** las filas de JaCoCo: sigue mostrando 23.0%/13.8%/16.8% cuando el reporte real archivado (`docs/mediciones/jacoco/report.xml`, mismo repositorio) ya marca 72.0%/62.5%/56.5% desde el 16 de agosto. Este hallazgo ya estaba en el informe original y sigue sin corregirse pese a que el archivo se editó hoy para otra cosa.

7. **Colección Postman duplicada y desactualizada.** Existe una segunda copia en `docs/mediciones/Pruebas.postman_collection.json` que **no se actualizó** — sigue con las 10 peticiones originales. Si alguien evalúa desde `docs/mediciones/` en vez de la raíz del repo, vería la versión vieja.

8. **`DATA-PROVENANCE.md` (nuevo) es sustantivo pero no cumple del todo la trazabilidad exigida.** Tiene narrativa real por métrica, pero solo declara el hash de commit para la tanda de mediciones del 30 de julio; las remediciones del 16 de agosto (SUS, JaCoCo) no tienen commit ni script citado.

### Lo que sigue exactamente igual (sin cambios desde el 16 de agosto)

Tag `v1.0.0`, documento académico final (Bloque B, 40% de la nota), `docs/requisitos/SRS-v1.0.0.pdf`, `docs/requisitos/historico/` y `elicitacion/`, publicación de imagen en GHCR, `docs/despliegue/`, URL pública de producción, `docs/entorno/versions.txt`, target `make all`, `docs/etica/ETHICS.md` y `ai-disclosure.md`, segundo DOI de Zenodo para el dataset, ORCID de los integrantes en `CITATION.cff`, y la versión de `CITATION.cff`/README (siguen en `v0.9.0-rc`).

### Balance

De los 9 commits de hoy, 7 cerraron brechas reales del informe original con evidencia verificable (Lighthouse, seguridad automatizada, procedimientos almacenados, Postman, `.env.example`, azurite, reorganización OWASP). Pero el trabajo generó **documentación que no se actualizó en cascada** (matriz de trazabilidad, bitácora de observaciones, DATA-DICTIONARY) y **un commit con una afirmación de completitud que no corresponde al contenido real** (checklists). Ninguno de estos hallazgos nuevos es grave por sí solo, pero conviene resolverlos antes de dar por cerrados los bloques 0, A.2, A.3 y E frente al docente-director.

---

## Advertencia de plazo

La guía fija el cierre el **viernes 17 de agosto de 2026**. Este informe se produce el **16 de agosto de 2026**, es decir, con el cierre a menos de 24 horas. Varias brechas aquí documentadas (documento académico IMRaD de 35–60 páginas, segundo DOI de Zenodo para el dataset, checklists de estándares de reporte, video de demostración de 5–7 min) requieren trabajo sustancial que no es razonable completar desde cero en ese plazo. Este informe no asume que todo se resolverá antes del cierre; su función es dar al equipo una fotografía exacta del estado real para decidir prioridades, qué declarar como brecha conocida en `OBSERVACIONES.md`, y si corresponde negociar alcance con el docente-director.

El repositorio parte de la Entrega 3 (`v0.9.0-rc`, 24 de julio de 2026). Ese release candidate sigue siendo la base actual: no existe ningún artefacto producido específicamente para v1.0.0 todavía (ni tag, ni documento final, ni checklists de estándares).

## Metodología

Se auditó el repositorio completo (`D:\Proyecto\Proyecto-WEB-ARTISYNC`) contra los siete bloques de requisitos de la guía (0, A–G), leyendo archivos reales, ejecutando `git tag`/`git log`, y usando `grep`/búsqueda de contenido para confirmar invocaciones de código, en vez de inferir a partir de nombres de carpetas. Cada fila de las tablas siguientes indica el archivo o comando que sustenta el veredicto.

---

## Hallazgos que bloquean múltiples criterios de la rúbrica

La guía define "reglas transversales de calificación" (sección 5.3) que activan penalizaciones automáticas independientes del resto de la evidencia. Estos son los hallazgos que las disparan o que tienen impacto equivalente:

1. **No existe el tag `v1.0.0`.** `git tag` solo devuelve `v0.7.0`, `v0.7.1`, `v0.9.0-rc`. Regla transversal #2: sin este tag, los criterios del Eje 1 (P1–P4) y del Eje 3 (R1–R4) se califican **Ausente (0 %)** automáticamente, sin importar el resto de la evidencia.

2. **`db/procs/` está excluido de git.** La línea 65 del `.gitignore` raíz contiene exactamente `db/procs`. Los 6 procedimientos/funciones SQL catalogados en `docs/basedatos/CATALOGO-SP.md` existen en el disco local de este equipo, pero **no están versionados**: un clon limpio del repositorio no los tendría. Esto reabre la brecha que la propia bitácora (OBS-AUTO-02) señalaba como crítica.

3. **Ninguno de esos 6 procedimientos está invocado desde el backend ni desplegado en la base de datos.** Búsqueda de cada nombre `fn_*` bajo `artisync/Backend/src/main/java` no arroja resultados. Falta el archivo `R__procedimientos.sql` en `artisync/Backend/src/main/resources/db/migration/` que `scripts/sync-procs.sh` debería generar a partir de `db/procs/*.sql` — nunca se ejecutó o su resultado nunca se comiteó. Lo único que sí se invoca vía JPA (`@Procedure` + una consulta nativa parametrizada, sin concatenación) son dos rutinas distintas (`sp_registrar_decision_verificacion`, `fn_listar_cola_verificacion`) que **no aparecen en el catálogo documentado**. El criterio **P1** exige explícitamente "≥6 procedimientos almacenados en `db/procs/` invocados con `@Procedure`... y trazados en la matriz con columna `tipo_acceso`" — hoy no se cumple. El propio `ADR-006` lo admite textualmente ("ningún procedimiento almacenado de negocio existe en el repositorio"), aunque ese texto no se ha actualizado desde julio y ya no refleja ni siquiera el estado parcial actual.

4. **No existe el documento académico final (`docs/informe-final.pdf`/`.tex`) en ninguna forma, ni borrador.** Este bloque (B) pesa **40 % de la nota** (Eje 2) y es, según la propia guía, lo que distingue cualitativamente la Entrega Final de las anteriores. Lo más cercano en el repo es `Entrega_3.pdf` (documento de la entrega anterior, 32 páginas) y un paper LaTeX de alcance acotado sobre seguridad de base de datos en `docs/latex_seguridad_bd/` — ninguno de los dos es ni pretende ser el informe IMRaD consolidado que exige la guía.

5. **`docs/checklists/` no existe.** Cero checklists (Ralph et al. 2021, PRISMA 2020, FAIR, INCOSE) en ninguna forma, ni borrador.

6. **Cifras desactualizadas en `README.md` y `DATA-DICTIONARY.md`.** El README muestra JaCoCo "23.0 % (no alcanzado)" cuando el reporte archivado más reciente (`docs/mediciones/jacoco/`) marca 72.0 % líneas / 62.5 % ramas; y muestra SUS "71.75 (n=10)" cuando el CSV real ya tiene n=16 (media 76.88). Es una corrección barata, pero mientras no se haga, cualquier evaluador que solo lea el README subestima el trabajo ya hecho.

---

## Tabla de brechas por bloque

Leyenda: ✅ existe y cumple · 🟡 existe mas incompleto/desactualizado · ❌ no existe

### Bloque 0 — Aplicación íntegra de observaciones acumuladas

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| `docs/observaciones/OBSERVACIONES.md` con código único, fuente, criterio, texto, decisión | ✅ | 25 observaciones (OBS-01 a OBS-16 del docente; OBS-AUTO-01 a 09 de revisión técnica interna) |
| Trazabilidad de cada observación a commit hash | 🟡 | la mayoría cita hash, pero localizado post hoc con `git log -S` (no por mensaje de commit referenciando el código OBS-XX); 7 observaciones siguen "pendiente" o "por confirmar" |
| % de observaciones resueltas | 🟡 | 17/25 resueltas (68 %), 1 parcial, 7 pendientes — **por debajo del 70 %** que activa la regla transversal #10 (criterio P0 = Insuficiente) |
| Tags `v0.7.0`, `v0.7.1`, `v0.9.0-rc` conservados + `v1.0.0` sobre el commit de cierre | 🟡/❌ | los tres primeros existen; `v1.0.0` no existe |
| Anexo A del documento académico con tabla-resumen de observaciones | ❌ | depende del documento académico final, que no existe |

### Bloque A.1 — Cierre del producto software: propiedades funcionales y de calidad

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| JaCoCo ≥70 % líneas y ramas en dominio/servicios/controladores, reporte HTML+XML archivado con fecha ISO 8601 | 🟡 | `docs/mediciones/jacoco/`: 72.0 % líneas / 62.5 % ramas (agregado global, no desglosado por capa); `pom.xml` configura el plugin solo para reportar, sin `jacoco:check` que falle el build bajo el umbral |
| k6 50 VUs/30 s, ≥5 corridas independientes, p95 ≤200 ms con cache caliente y ≤500 ms con cache frío | 🟡 | 6 corridas archivadas (3 frías + 3 calientes) con umbrales cumplidos en el reporte, pero el script `k6/script.js` que las generó **no está versionado** en el repo, y `docs/mediciones/perf/REPORTE-PERF.md` admite que el escenario "frío" no produjo un cache-miss real, invalidando la comparación frío/caliente tal como está diseñada |
| Lighthouse ≥3 corridas por perfil (mobile y desktop), umbrales Performance≥80/Accessibility≥90/Best Practices≥90/SEO≥90 | 🟡 | solo perfil **mobile**, 2 corridas (antes/después de una mejora), sin ninguna corrida de escritorio; los 4 umbrales sí se cumplen en la corrida "después" (92/100/100/100) |
| 6 controles OWASP mínimos + escaneo automático (ej. OWASP ZAP baseline) archivado | 🟡/❌ | 6 controles evidenciados con curl reproducible (A01, A02, A03, A05, A07, A09; A04/A06/A08/A10 fuera de alcance declarado); **no hay ningún escaneo ZAP** — no existe carpeta `docs/mediciones/sec/zap/` ni reporte alguno |
| Análisis estático (spotbugs/find-sec-bugs u equivalente) sobre concatenación SQL | ❌ | no configurado en `pom.xml`; no hay reporte en `docs/mediciones/sec/static-analysis/` (carpeta inexistente) |
| Tag `v1.0.0` + imagen Docker en GHCR con digest sha256 declarado en README | ❌ | sin tag; ningún workflow de `.github/workflows/` publica a un registro de contenedores (solo build local en CI) |
| CI con ≥3 ejecuciones consecutivas exitosas y badge en README | 🟡 | badge y workflow (`ci.yml`) existen y compilan/testean el backend, pero el pipeline no ejecuta k6, Lighthouse, ZAP, análisis estático ni `scripts/audit-sql-dynamic.sh` — solo tests unitarios y `scripts/validate-traceability.sh` |

### Bloque A.2 — Consolidación de la estrategia híbrida de acceso a datos

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| ≥6 procedimientos/funciones en `db/procs/`, uno por categoría funcional | 🟡 | 6 archivos `.sql` existen en disco (consultas multi-tabla, cálculos agregados, reportes, actualizaciones masivas, validaciones cruzadas, generación de códigos), pero **gitignored** (ver hallazgo bloqueante #2) |
| Invocación exclusivamente vía `@Procedure`/`@NamedStoredProcedureQuery` (JPA 2.1), prohibida la concatenación en `createNativeQuery` | ❌ | ninguno de los 6 procedimientos catalogados se invoca desde código Java; las 2 rutinas que sí se invocan (correctamente, sin concatenación) no están en el catálogo |
| `docs/basedatos/CATALOGO-SP.md` con nombre, categoría, propósito, parámetros IN/OUT/INOUT, cursores, tablas afectadas | 🟡 | documento completo y bien estructurado, pero documenta rutinas no usadas por el código y omite las 2 que sí se usan |
| ADR-006 formal (plantilla Nygard) con alternativas descartadas y consecuencias | 🟡 | `docs/adr/adr-006-estrategia-acceso-datos.md` existe y sigue la plantilla, pero está desactualizado desde la Tercera Entrega: dice textualmente que "ningún procedimiento almacenado de negocio existe en el repositorio" |
| `docs/trazabilidad/matriz.csv` con columna `tipo_acceso` (CRUD-ORM / SP) por requisito | ✅ | existe; de 37 filas, 30 marcadas `CRUD-ORM`, 2 marcadas `SP`, 5 sin valor |
| Regla estática en CI que rechace concatenación sospechosa (spotbugs/find-sec-bugs) | ❌ | no configurado |
| Revisión automática de `db/procs/*.sql` contra `EXECUTE IMMEDIATE`/`sp_executesql` (`scripts/audit-sql-dynamic.sh`) | 🟡 | el script existe y está bien escrito, integrado al `Makefile` (`make audit`), pero **no se ejecuta en CI** pese a que su propio encabezado dice que sí |

### Bloque A.3 — Culminación de la ingeniería de requisitos

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| `docs/requisitos/SRS-v1.0.0.pdf` con firma de aprobación del docente-director | ❌ | el SRS actual (`docs/requisitos/SRS.md` y `SRS.pdf`) se autoidentifica como **versión v0.9.0-rc — Tercera Entrega**; no hay ningún bloque de firma o aprobación en el documento |
| `docs/requisitos/historico/` con versiones previas del SRS | ❌ | no existe la carpeta |
| Cada requisito satisface INCOSE v4 (C1–C9) y el conjunto (C10–C15); checklist INCOSE como anexo | ❌ | "INCOSE" aparece solo como referencia bibliográfica en el encabezado del SRS; no hay ningún checklist real aplicado requisito por requisito |
| `docs/requisitos/CHANGELOG-REQ.md` con métricas de estabilidad (tasa de cambio) | 🟡 | el changelog existe (2 versiones registradas, formato Keep a Changelog), pero no calcula ninguna tasa de estabilidad |
| Prioridad MoSCoW y estado "verificado" por requisito | ✅ | presente en `SRS.md` (`Prioridad: Must/Should/Could`) y en `matriz.csv` (columna `estado`, valores incluyendo `verificado`) |
| Historias de usuario en formato Connextra + INVEST + Gherkin, trazadas a prueba automatizada | ✅ | `docs/requisitos/historias/` (HU-01 a HU-23), formato conforme confirmado por muestreo, con trazabilidad explícita a `REQ-F-NNN` |
| Casos de uso en plantilla de Cockburn niveles 1–4, trazados a flujo y prueba de integración | ✅ | `docs/requisitos/casos-de-uso/` (CU-01 a CU-23), formato conforme confirmado por muestreo |
| `docs/requisitos/elicitacion/` con evidencia de entrevistas, talleres, prototipos | ❌ | no existe la carpeta ni evidencia equivalente en otra ubicación |

### Bloque A.4 — Puesta en producción del sistema

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| URL pública HTTPS declarada en README y portada del PDF final | ❌ | no hay URL pública declarada en ningún archivo; `artisync/docker-compose.azure.yml` configura un despliegue en Azure, pero sin dominio publicado |
| `/actuator/health` en estado UP para todos los componentes | ✅ | `spring-boot-starter-actuator` configurado, expuesto (`health,info,metrics`), usado como healthcheck del contenedor backend |
| Usuario demo preconfigurado y documentado | ✅ | `README.md` publica credenciales de arranque (`admin@artisync.com`) |
| `docs/despliegue/DEPLOYMENT.md`, `RUNBOOK.md`, `BACKUP.md` | ❌ | la carpeta `docs/despliegue/` no existe; el único artefacto relacionado es un PDF suelto en la raíz del repo ("Documentacion sobre respaldos.pdf") que no sigue la estructura ni el contenido pedido |
| ADR-007 sobre estrategia de despliegue | 🟡 | existe pero numerado como `adr-005` (no `adr-007`, que en este repo corresponde a almacenamiento de archivos); además desactualizado — afirma que "el directorio Frontend/ no existe aún", lo cual ya no es cierto |

### Bloque B — Documento técnico académico final del PFC (40 % de la nota — Eje 2)

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| PDF de 35–60 páginas, estructura IMRaD ampliada (~15 capítulos obligatorios), con fuente LaTeX y `.bib` | ❌ | **no existe en ninguna forma, ni borrador.** `Entrega_3.pdf` (32 páginas) es el documento de la entrega anterior; `docs/latex_seguridad_bd/` es un paper LaTeX de alcance acotado (seguridad de BD en PostgreSQL), no el informe consolidado del proyecto completo |
| Portada institucional con ORCID de cada integrante y DOI Zenodo | ❌ | depende del documento inexistente; no hay evidencia de registro ORCID de los integrantes en ningún archivo |
| Resúmenes estructurados en español e inglés (200–250 palabras, 5 elementos) + palabras clave controladas | ❌ | no existe |
| Índices, lista de siglas | ❌ | no existe |
| Cap. 1 Introducción (contexto cuantificado, RQs, objetivos, contribuciones) | ❌ | no redactado |
| Cap. 2 Marco teórico | ❌ | no redactado |
| Cap. 3 Trabajos relacionados (búsqueda estructurada, diagrama PRISMA, tabla comparativa ≥8 filas) | ❌ | no redactado |
| Cap. 4 Ingeniería de requisitos (capítulo propio, obligatorio, no debe fusionarse con diseño) | ❌ | no redactado |
| Cap. 5 Materiales y métodos (DSR de Peffers, GQM, muestreo, protocolo, análisis estadístico) | ❌ | no redactado |
| Cap. 6 Diseño del sistema y arquitectura (resumen de las 6 ADRs, tabla ISO/IEC 25010, matriz de trazabilidad) | ❌ | no redactado (los insumos —ADRs, matriz— sí existen por separado) |
| Cap. 7 Implementación (3–6 listados de código, subsección de acceso a datos híbrido) | ❌ | no redactado |
| Cap. 8 Evaluación empírica y resultados (por bloque: RQ, métrica, datos crudos, estadística, interpretación) | ❌ | no redactado (los datos crudos y reportes narrativos por bloque sí existen sueltos en `docs/mediciones/`) |
| Cap. 9 Discusión (responde RQ por RQ, compara con trabajos relacionados) | ❌ | no redactado |
| Cap. 10 Amenazas a la validez (su ausencia anula automáticamente el criterio D5 según regla transversal #5) | ❌ | no redactado |
| Cap. 11 Trabajo futuro | ❌ | no redactado |
| Cap. 12 Conclusiones | ❌ | no redactado |
| Declaraciones obligatorias (disponibilidad de código/datos/materiales, ética, CRediT, financiamiento, uso de IA, agradecimientos) | ❌ | no existen como sección del documento (aunque hay insumos parciales reutilizables: `CONTRIBUTORS.md` con roles CRediT, plantilla de consentimiento) |
| Bibliografía ≥30 referencias verificadas, ≥20 de alto impacto (JCR Q1–Q2 / ICSE / FSE / ASE / MSR / EASE / ESEM) | ❌ | no hay archivo `.bib` consolidado del proyecto |
| Anexos (cadena de búsqueda, protocolo SUS, capturas de CI, checklist Ralph et al., matriz completa) | ❌ | no existe |

### Bloque C — Evidencia empírica con análisis estadístico riguroso

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| Media, DT e IC 95 % en todas las mediciones cuantitativas | 🟡 | presente en los reportes de SUS y k6; no verificado de forma sistemática en Lighthouse ni JaCoCo |
| Test inferencial no paramétrico + tamaño de efecto para comparaciones (ej. cache frío vs. caliente) | ❌ | no reportado; y la comparación frío/caliente de k6 es metodológicamente inválida tal como está construido el script actual (ver hallazgo bloqueante #6 y detalle en A.1) |
| Corrección por comparaciones múltiples cuando aplique (Holm/Benjamini-Hochberg) | ❌ | no aplica todavía porque no hay tests inferenciales reportados |
| Gráficos vectoriales regenerables por script versionado, con paleta accesible a daltonismo | 🟡 | existen scripts puntuales de análisis SUS con gráfico de caja; no hay un pipeline unificado de generación de figuras (`scripts/gen-figuras.py` no existe) |

### Bloque D — Reproducibilidad absoluta y publicabilidad del artefacto

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| Objetivo `make all` de un solo comando (contenedores → semillas → tests → benchmarks → reportes → PDF) | ❌ | `Makefile` raíz tiene `up`, `down`, `test`, `bench`, `audit`, `clean`, `sus` — **no existe `all`**; además `bench` depende de un script k6 (`k6/catalogo-load.js`) que no está versionado en el repo |
| Imagen Docker en GHCR, tag `v1.0.0`, digest sha256 declarado en README + CITATION.cff + portada del PDF | ❌ | ningún workflow publica a GitHub Container Registry; solo se pinean por digest las imágenes de terceros (`postgres:16`, `redis:7-alpine`), no la imagen propia del proyecto |
| Cuadernos ejecutables (Jupyter/RMarkdown) con salidas archivadas | ❌ | no existe ningún `.ipynb` ni `.Rmd` en todo el repositorio |
| `docs/entorno/versions.txt` con versiones exactas de herramientas | ❌ | la carpeta `docs/entorno/` no existe |
| Semillas aleatorias declaradas en cada script y en el README | 🟡 | no verificado de forma sistemática en los scripts existentes |
| Depósito en Zenodo del software con DOI propio | ✅ | declarado en `README.md` y `CITATION.cff` (`10.5281/zenodo.21730559`) — pero corresponde aún a la versión v0.9.0-rc |
| Depósito en Zenodo del dataset de mediciones, DOI separado y licencia propia (ej. CC BY 4.0) | ❌ | no hay evidencia de un segundo DOI para el dataset |
| `CITATION.cff` v1.2.0 con todos los campos exigidos (incl. ORCID de cada autor y `preferred-citation`) | 🟡 | formato y campos base correctos (`cff-version`, `title`, `authors`, `version`, `date-released`, `license`, `repository-code`, `doi`, `keywords`), pero **sin ORCID de ningún autor** y **sin bloque `preferred-citation`**; versión todavía v0.9.0-rc |

### Bloque E — Cumplimiento de estándares de reporte

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| Checklist del estándar empírico ACM SIGSOFT principal (Ralph et al. 2021) según tipo de estudio | ❌ | `docs/checklists/` no existe |
| Checklist PRISMA 2020 (si aplica revisión de trabajos relacionados) | ❌ | no existe |
| Checklist FAIR (software + datos + metadatos) | ❌ | no existe |
| Checklist INCOSE (si aplica) | ❌ | no existe |

### Bloque F — Paquete de datos, materiales y metadatos

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| `docs/mediciones/DATA-DICTIONARY.md` cubriendo el 100 % de las variables crudas | ✅ | existe, cubre 17 variables (perf, SUS, Lighthouse, JaCoCo, controles OWASP) — pero con cifras desactualizadas (ver hallazgo bloqueante #6) |
| `docs/mediciones/DATA-PROVENANCE.md` (qué script/commit generó cada tabla/figura) | ❌ | no existe como archivo separado; hay notas sueltas de procedencia al final de `DATA-DICTIONARY.md`, no un documento dedicado |
| Estructura `docs/mediciones/sec/{owasp,zap,static-analysis}/` | ❌ | `docs/mediciones/sec/` es una carpeta plana sin subcarpetas; no hay contenido de ZAP ni de análisis estático en ninguna ubicación |
| Datos crudos SUS con N≥15 participantes | ✅ | `docs/mediciones/sus/sus-raw.csv` con 16 participantes (P01–P16) |

### Bloque G — Ética, consentimiento y disclosure

| Requisito de la guía | Estado | Evidencia |
|---|---|---|
| `docs/etica/ETHICS.md` (dónde se resguardan los consentimientos firmados, fuera del repo) | ❌ | no existe (ya señalada como brecha por la propia bitácora en OBS-15) |
| Plantilla de consentimiento informado + resguardo técnico fuera del repositorio público | ✅ | `docs/etica/consentimientos/plantilla.md`, con un `.gitignore` propio que bloquea la subida accidental de consentimientos firmados; participantes referenciados solo por código (P01, P02…) |
| Declaración de uso de asistentes de IA generativa (fase, propósito, revisión del equipo) | ❌ | no existe ningún archivo `ai-disclosure.md` ni declaración equivalente en el repositorio |

### Entregables consolidados (sección 4 de la guía)

| Entregable | Estado | Evidencia |
|---|---|---|
| Repositorio con tag `v1.0.0` y hash declarado en portada del PDF | ❌ | tag inexistente; PDF inexistente |
| DOI del software y DOI del dataset (dos DOIs distintos en Zenodo) | 🟡 | solo el DOI del software existe |
| PDF del documento académico final (35–60 páginas) con LaTeX y `.bib` en la misma carpeta | ❌ | no existe |
| Colección Postman con ≥25 peticiones cubriendo éxito/validación/autorización/no encontrado | ❌ | `Pruebas.postman_collection.json` tiene **10** peticiones; cubre éxito, CRUD básico y un caso "no encontrado", pero no hay peticiones explícitas de autorización denegada (403) ni de validación fallida (400) |
| Cuadernos de análisis (Jupyter/RMarkdown) con salidas ejecutadas | ❌ | no existen |
| Video corto (5–7 min) de `make all` desde clonación limpia, enlazado desde README | ❌ | no existe; tampoco existe el objetivo `make all` que debería demostrar |
| Slides para la defensa oral | 🟡 | solo existe un deck LaTeX/Beamer de alcance acotado (seguridad de BD), no una presentación de todo el proyecto |

---

## Lo que ya está sólido

Para no perder de vista el trabajo real ya hecho al leer tantas brechas:

- **Ingeniería de requisitos operativa**: historias de usuario (Connextra + INVEST + Gherkin) y casos de uso (Cockburn niveles 1–4) completos y trazados a requisitos, en `docs/requisitos/historias/` y `docs/requisitos/casos-de-uso/`.
- **Matriz de trazabilidad** (`docs/trazabilidad/matriz.csv`) con 37 requisitos y columna `tipo_acceso` ya incorporada.
- **7 ADRs presentes** en `docs/adr/` siguiendo mayormente la plantilla de Nygard (2 de ellos desactualizados y con numeración distinta a la que espera la guía, pero con contenido sustantivo).
- **Diagramas C4 niveles 1–3** documentados en `docs/diagramas/`, con DSL de Structurizr embebido en Markdown (aunque no como archivo `.dsl` independiente ni generado automáticamente en CI).
- **Mediciones empíricas reales y archivadas** (no simuladas) para JaCoCo, k6, Lighthouse, los 6 controles OWASP evaluados y SUS — cada una con brechas puntuales documentadas arriba, pero con datos crudos genuinos detrás.
- **`CONTRIBUTORS.md`** con roles explícitos según la taxonomía CRediT.
- **`docs/observaciones/OBSERVACIONES.md`** con 68 % de observaciones históricas ya resueltas y trazabilidad razonable (aunque post hoc).
- **Seguridad operacional del despliegue**: `/actuator/health` expuesto, usuario demo documentado, imágenes base (Postgres/Redis) pineadas por digest sha256.

---

## Nota metodológica

Este informe fue generado mediante auditoría automatizada (lectura de archivos, `grep`, `git tag`/`git log`) sobre el estado del repositorio al 2026-08-16, commit `6f8c09c`. Cualquier cambio posterior al repositorio no está reflejado aquí. Se recomienda volver a ejecutar esta auditoría inmediatamente antes de la etiqueta `v1.0.0` para confirmar qué brechas siguen abiertas.
