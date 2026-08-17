# Checklist FAIR

*Findable, Accessible, Interoperable, and Reusable data (Wilkinson et al. 2016).*

**Alcance:** el paquete de datos y metadatos de mediciones empíricas bajo `docs/mediciones/` (rendimiento, seguridad, usabilidad, cobertura, calidad web), documentado en `docs/mediciones/DATA-DICTIONARY.md` y `docs/mediciones/DATA-PROVENANCE.md`.

**Fecha de evaluación:** 2026-08-17. **Evaluado contra:** commit `6af8595`.

---

## 1. Findable (Encontrable)

- [ ] ¿Los datos y metadatos tienen identificadores únicos (ej. DOIs si aplica, o rutas claras)?
  **Parcial.** El software en su conjunto tiene DOI de Zenodo (`10.5281/zenodo.21730559`, declarado en `CITATION.cff` y en el README), pero corresponde a la versión `v0.9.0-rc`, no a `v1.0.0`. **El dataset de mediciones no tiene un DOI propio** — la guía exige un depósito Zenodo separado para datos, con licencia propia, siguiendo el principio de citación independiente de software y datos (Bloque D.3). Las rutas dentro del repositorio (`docs/mediciones/<bloque>/<archivo>`) sí son claras, estables y referenciadas de forma consistente en `DATA-DICTIONARY.md`, pero eso no sustituye un identificador persistente externo.

- [x] ¿Están los datos descritos con metadatos ricos (DATA-DICTIONARY.md)?
  **Sí.** `docs/mediciones/DATA-DICTIONARY.md` documenta 17 variables con nombre, descripción, tipo, unidad, fuente (archivo crudo), umbral esperado y valor medido — cubre perf, SUS, Lighthouse, JaCoCo y los 6 controles OWASP.

- [ ] ¿Están registrados o indexados en un recurso buscable?
  **Parcial.** El repositorio en sí es público e indexado por GitHub (buscable por texto/código); el software tiene registro en Zenodo. El **dataset específico no está registrado como entidad independiente** en ningún índice (ni Zenodo propio, ni un repositorio de datos como Figshare/OSF) — solo existe como directorio dentro del repositorio de código.

## 2. Accessible (Accesible)

- [x] ¿Se pueden recuperar los datos mediante un protocolo estandarizado (ej. Git/HTTPS)?
  **Sí.** Repositorio público en GitHub, clonable vía `git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC` (HTTPS) o SSH.

- [x] ¿El protocolo es abierto, gratuito y universalmente implementable?
  **Sí.** Git + HTTPS + GitHub — sin autenticación, sin costo, sin credenciales propietarias para lectura pública.

- [ ] ¿Se proveen metadatos accesibles incluso si los datos ya no están disponibles?
  **No.** `DATA-DICTIONARY.md` y `DATA-PROVENANCE.md` viven en el mismo repositorio que los datos crudos que describen — si el repositorio deja de estar disponible, ambos desaparecen juntos. No existe un registro de metadatos independiente y persistente (ej. un depósito Zenodo con metadatos que sobreviva aunque el repositorio Git se elimine). Esto está directamente ligado a la ausencia del DOI de dataset del ítem anterior.

## 3. Interoperable (Interoperable)

- [ ] ¿Los datos y metadatos usan vocabularios formales, accesibles, compartidos y ampliamente aplicables?
  **No.** Los nombres de variables en `DATA-DICTIONARY.md` (`sus_score_mean`, `lh_performance`, `jacoco_lines_pct`, etc.) son convenciones internas del proyecto, consistentes entre sí pero no basadas en un vocabulario o esquema formal compartido por la comunidad (ej. no usan un schema.org, DCAT o vocabulario específico de ingeniería de software empírica).

- [x] ¿Están en formatos estándar (ej. CSV, JSON) en lugar de formatos propietarios (ej. XLSX cerrado)?
  **Sí.** Verificado por inspección de `docs/mediciones/`: los datos crudos usan CSV (`sus/sus-raw.csv`, `sus/perfil-participantes.csv`), JSON (k6, Lighthouse, ZAP), XML (JaCoCo, SpotBugs) y texto plano (evidencia OWASP). No se encontró ningún archivo en formato propietario cerrado (XLSX, DOCX) dentro del paquete de datos de mediciones.

- [ ] ¿Incluyen referencias calificadas a otros datos o metadatos?
  **Parcial.** `DATA-PROVENANCE.md` enlaza cada métrica a su archivo crudo y (para la tanda del 30 de julio) a un commit hash específico, en prosa narrativa — es una referencia real pero informal, no una referencia calificada en el sentido FAIR estricto (no usa URIs tipados ni un vocabulario de procedencia como PROV-O). Además, como ya señala `INFORME-BRECHAS-ENTREGA-FINAL.md`, las remediciones del 16 de agosto (SUS n=16, JaCoCo 72%) no tienen commit hash citado en `DATA-PROVENANCE.md` — la referencia existe pero está incompleta para los datos más recientes.

## 4. Reusable (Reutilizable)

- [x] ¿Están descritos de forma plural y precisa con atributos relevantes?
  **Sí.** Cada variable en `DATA-DICTIONARY.md` tiene tipo, unidad, umbral y fuente — suficiente para que un tercero entienda qué representa y cómo se generó sin leer el código.

- [ ] ¿Tienen una licencia de uso clara y accesible (ej. MIT, CC-BY)?
  **Parcial.** El repositorio completo tiene licencia MIT (`LICENSE`, raíz del repo) — cubre el **software**. La guía de la Entrega Final exige explícitamente que el **dataset** de mediciones tenga su propia licencia declarada (recomienda CC BY 4.0 o equivalente), separada de la licencia del software, como parte del depósito Zenodo del dataset (Bloque D.3). Esa licencia específica para datos **no existe** — MIT no es la licencia recomendada para datos de investigación.

- [x] ¿Se asocian con su procedencia detallada (DATA-PROVENANCE.md)?
  **Sí, con matices.** `docs/mediciones/DATA-PROVENANCE.md` existe y documenta procedencia por bloque de métrica (SUS, Lighthouse, k6, JaCoCo) con fecha de recolección y limitaciones metodológicas declaradas honestamente. Como se anota en Interoperable arriba, la cobertura de commit hash/script no es 100% para las mediciones más recientes.

- [ ] ¿Cumplen con los estándares comunitarios relevantes al dominio?
  **Parcial.** El paquete sigue prácticas generales sólidas de reproducibilidad computacional (datos crudos inmutables, scripts versionados para SUS) pero no declara conformidad explícita con un estándar comunitario de datos de ingeniería de software empírica (ej. no hay un `codemeta.json`, no se sigue un esquema de replication package estandarizado como el de ACM). El checklist Ralph et al. 2021 (`ralph-2021-checklist.md`) cubre la dimensión metodológica; este ítem se refiere específicamente a estandarización de metadatos de datos, que sigue siendo informal.

---

## Resumen

**4 de 11 ítems cumplidos completamente**, 6 parciales, 1 no cumplido. El hallazgo transversal que explica la mayoría de los ítems parciales/no cumplidos es el mismo: **no existe un DOI ni licencia de dataset independiente del software** (Bloque D.3 de la guía — depósito Zenodo del dataset con DOI y licencia CC BY 4.0 propios). Resolver ese punto único destrabaría directamente Findable #1 y #3, Accessible #3, e Interoperable #3 y Reusable #2. El resto (vocabularios formales, estándares comunitarios) son mejoras de mayor esfuerzo, razonables como trabajo futuro. No se marcan ítems como cumplidos sin evidencia verificada en el repositorio.
