# Checklist de Validación Empírica (Ralph et al. 2021)

*Plantilla basada en los estándares empíricos de ACM SIGSOFT ("Empirical Standards for Software Engineering Research", Ralph et al. 2021).*

**Estándar aplicado:** *Engineering Research* — el PFC diseña y construye un artefacto de software (Artisync) y evalúa sus propiedades mediante múltiples métodos empíricos concurrentes (benchmark de rendimiento, auditoría de seguridad, estudio de usabilidad, cobertura de pruebas), en vez de un experimento controlado único con asignación aleatoria a grupos o un estudio de caso sobre una organización externa. Es consistente con la metodología Design Science Research de Peffers declarada para el capítulo de Materiales y Métodos del documento académico (Bloque B.7 de la guía).

**Fecha de evaluación:** 2026-08-17. **Evaluado contra:** commit `6af8595` (rama `feat/ia-verificacion-asistida`).

---

## 1. Diseño del Estudio

- [x] ¿Se establecieron claramente los objetivos de investigación?
  **Parcial-Sí.** No existe todavía un capítulo de Introducción con preguntas de investigación (RQ1, RQ2...) formales — el documento académico final (Bloque B) no existe (ver `docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`). Sin embargo, cada bloque de evaluación empírica declara un objetivo medible y explícito en su propio reporte: `docs/mediciones/perf/REPORTE-PERF.md` (p95 ≤200ms caliente/≤500ms frío), `docs/mediciones/sec/REPORTE-SEC.md` (6 controles OWASP + ZAP sin hallazgos altos), `docs/mediciones/sus/REPORTE-SUS.md` (SUS >68), `docs/mediciones/lighthouse/REPORTE-LIGHTHOUSE.md` (Performance≥80, resto≥90), `docs/mediciones/jacoco/REPORTE-JACOCO.md` (cobertura ≥70%). **Pendiente:** consolidar estos objetivos como preguntas de investigación formales en el Capítulo 1 del informe final.

- [x] ¿Se definió el contexto (participantes, tareas, entorno)?
  **Sí, por bloque.** SUS: 16 participantes externos al equipo, perfil declarado en `docs/mediciones/sus/perfil-participantes.csv` (edad, sexo, experiencia web, dispositivo). Rendimiento: 50 VUs / 30s contra `GET /api/v1/catalogo`, entorno `docker compose` local (`REPORTE-PERF.md`). Seguridad: `docker compose up -d --build`, ZAP baseline contra `http://localhost:4200` (`REPORTE-SEC.md`). Lighthouse: build de producción vía `docker-compose.lighthouse.yml`, perfiles mobile/desktop (`REPORTE-LIGHTHOUSE.md`).

- [x] ¿El diseño es adecuado para responder las preguntas de investigación?
  **Sí.** Los métodos elegidos (benchmark de carga, escaneo de seguridad dual manual+automático, cuestionario SUS estandarizado, cobertura de código, auditoría de accesibilidad/rendimiento web) son los instrumentos estándar de la comunidad de ingeniería de software empírica para cada propiedad de calidad evaluada (ISO/IEC 25010), consistente con lo declarado en la guía de la Entrega Final (Bloque B.7).

## 2. Recolección de Datos

- [x] ¿Se describen detalladamente los procedimientos de recolección?
  **Sí.** Cada `REPORTE-*.md` documenta el comando exacto ejecutado (ej. `make lighthouse`, `docker run ... zap-baseline.py -t http://localhost:4200`, `mvn spotbugs:spotbugs` dentro de un contenedor `maven:3.9-eclipse-temurin-21`) y la fecha/commit de la corrida.

- [x] ¿Se reportan las herramientas utilizadas?
  **Sí, con versión.** k6, `@lhci/cli` 0.15.1 / Lighthouse 12.6.1, OWASP ZAP (`ghcr.io/zaproxy/zaproxy:stable`), SpotBugs 4.8.6.6 + find-sec-bugs 1.13.0, JaCoCo 0.8.13, cuestionario SUS de Brooke (10 ítems, formulario Google Forms).

- [ ] ¿Se mitigaron los sesgos en la recolección de datos?
  **No completamente.** Los participantes SUS son externos al equipo (mitiga sesgo de complacencia), pero no hay una técnica de muestreo declarada ni justificada siguiendo Baltes y Ralph (2022) — el tamaño de muestra (n=16) no tiene una justificación de poder estadístico documentada. Para rendimiento, el propio `REPORTE-PERF.md` admite una limitación metodológica: el escenario "frío" no aísla un *cache miss* real por iteración, lo que introduce un sesgo de medición reconocido pero no corregido. **Pendiente:** declarar técnica de muestreo y justificación de N en el capítulo de Materiales y Métodos.

## 3. Análisis de Datos

- [x] ¿Se documentó el proceso de análisis?
  **Sí.** `docs/mediciones/sus/analisis-sus.py` y `graficar-sus.py` calculan media/mediana/DT/IC 95% de forma reproducible (`make sus`); `REPORTE-PERF.md` documenta el cálculo de percentiles (p50/p90/p95/p99) desde los JSON crudos de k6.

- [ ] ¿Se aplicaron métodos estadísticos o cualitativos apropiados?
  **Parcial.** Estadística descriptiva completa (media, DT, IC 95%) está presente en SUS y k6. **Falta** todo test inferencial (ej. Wilcoxon para la comparación cache frío/caliente que la guía exige explícitamente en el Bloque C) y todo tamaño de efecto (Cliff's delta, r de rangos) en cualquiera de los bloques — no se encontró ninguno en el repositorio. Esta es una brecha ya identificada en `INFORME-BRECHAS-ENTREGA-FINAL.md` (Bloque C).

## 4. Reporte de Resultados

- [x] ¿Los resultados responden directamente a los objetivos propuestos?
  **Sí.** Cada reporte cierra con una tabla explícita de "umbral vs. resultado vs. cumple" (ver `REPORTE-LIGHTHOUSE.md`, `REPORTE-SEC.md`, `REPORTE-SUS.md`).

- [ ] ¿Se incluyeron métricas clave (ej. tamaños de efecto, p-valores, intervalos de confianza)?
  **Parcial.** IC 95% presente en SUS (`[69.16, 84.59]`, n=16) y en k6. **Ausentes:** tamaños de efecto y valores p en todos los bloques (ninguna comparación inferencial se ha corrido todavía — mismo hallazgo que el ítem anterior).

- [ ] ¿Se discutieron las limitaciones (amenazas a la validez)?
  **Parcial.** Existen notas de limitación metodológica puntuales y honestas dentro de reportes individuales (ej. `REPORTE-PERF.md` sobre el cache frío no aislado; `DATA-PROVENANCE.md` sobre la traducción del cuestionario SUS). **Falta** un capítulo consolidado de "Amenazas a la validez" que cubra las cuatro categorías exigidas (constructo, interna, externa, conclusión) — depende del documento académico final, que todavía no existe (Bloque B.12 de la guía).

## 5. Replicabilidad

- [x] ¿Se provee un paquete de datos (data package) con datos crudos?
  **Sí.** `docs/mediciones/` contiene datos crudos por bloque: `perf/k6-*.json`, `sec/owasp/*.txt`, `sec/zap/*.json`, `sec/static-analysis/*.xml`, `sus/sus-raw.csv`, `lighthouse/lhci-*.json`, `jacoco/report.xml`, catalogados en `DATA-DICTIONARY.md` y `DATA-PROVENANCE.md`.

- [ ] ¿Los scripts de análisis están disponibles?
  **Parcial.** Los scripts de SUS (`analisis-sus.py`, `graficar-sus.py`) están versionados y son ejecutables vía `make sus`. **Falta** un script consolidado de generación de figuras (`gen-figuras.py`, referenciado en la estructura esperada por la guía pero inexistente) y `DATA-PROVENANCE.md` admite que las remediciones del 16 de agosto (SUS, JaCoCo) no tienen un script ni commit hash específico citado — la trazabilidad script→dato no es completa para todas las métricas.

---

## Resumen

**9 de 12 ítems cumplidos o parcialmente cumplidos con evidencia real** (0 sin evaluar). Los tres ítems marcados `[ ]` (mitigación de sesgos, métodos estadísticos apropiados/inferenciales, discusión de amenazas a la validez) son brechas genuinas ya documentadas en `docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md` (Bloque C) y requieren trabajo adicional antes del cierre de la Entrega Final — no se marcan como resueltos para evitar una declaración de completitud falsa.
