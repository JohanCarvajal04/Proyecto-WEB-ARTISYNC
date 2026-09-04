# Diccionario de Datos — Mediciones Entrega Final (v1.0.0)

| Variable | Descripción | Tipo | Unidad | Fuente | Rango/umbral esperado | Valor medido |
|---|---|---|---|---|---|---|
| p95_latency_hot | Percentil 95 de latencia, cache caliente | float | ms | `perf/k6-run{1,2,3}.json` | < 200 | 50.17 |
| p95_latency_cold | Percentil 95 de latencia, cache recién vaciado | float | ms | `perf/k6-cold-run{1,2,3}.json` | < 500 | 39.14 (ver advertencia metodológica en `perf/REPORTE-PERF.md`: el script actual no aísla un miss real por iteración) |
| error_rate_5xx | Tasa de respuestas ≥500 | float | % | `perf/k6-run{1,2,3}.json` + `perf/k6-cold-run{1,2,3}.json` | = 0 | 0.00% |
| throughput_rps | Throughput agregado | float | req/s | `perf/k6-console-run*.txt` | — | ≈48.5–49.0 |
| mannwhitney_u_catalogo | Estadístico U de Mann-Whitney, catálogo caliente vs. frío | float | — | `perf/salida-inferencial.txt` (T-15, `analisis-inferencial.py`) | — | U=13,841,240; p=9.68×10⁻²⁰⁰ |
| a12_catalogo | Â₁₂ de Vargha-Delaney, catálogo caliente vs. frío | float | — (0-1) | `perf/salida-inferencial.txt` | — | 0.684 (efecto mediano) — significativo pero sin interpretación causal, ver limitación metodológica de `REPORTE-PERF.md` |
| p95_latency_auth_hot | Percentil 95 de latencia, endpoint protegido, backend "caliente" | float | ms | `perf/k6-auth-run{1..5}.json` | — | 45.05 |
| p95_latency_auth_cold | Percentil 95 de latencia, endpoint protegido, backend recién reiniciado | float | ms | `perf/k6-auth-cold-run{1..5}.json` | — | 113.73 |
| error_rate_auth | Tasa de respuestas ≥500, endpoint protegido | float | % | `perf/k6-auth-run{1..5}.json` + `perf/k6-auth-cold-run{1..5}.json` | = 0 | 0.00% |
| mannwhitney_u_auth | Estadístico U de Mann-Whitney, endpoint protegido caliente vs. frío | float | — | `perf/salida-inferencial.txt` | — | U=16,848,748; p<10⁻³⁰⁰ |
| a12_auth | Â₁₂ de Vargha-Delaney, endpoint protegido caliente vs. frío | float | — (0-1) | `perf/salida-inferencial.txt` | — | 0.308 (efecto mediano, sentido opuesto al del catálogo) |
| sus_score_mean | Puntaje SUS promedio | float | puntos (0-100) | `sus/sus-raw.csv` | > 68 | **61.25** (n=16, DT 22.08, IC 95% [49.49, 73.01], Bangor D, no supera el umbral — ver corrección de integridad de datos del 2026-09-03 en `sus/REPORTE-SUS.md` y `sus/PLAN-MEJORA-SUS.md`) |
| sus_q1 | Ítem 1 del SUS — "Creo que me gustará usar este sistema con frecuencia." (positivo, impar) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q1` | 1-5 | media cruda 3.75 (n=16, min 1, max 5) |
| sus_q2 | Ítem 2 del SUS — "Encontré el sistema innecesariamente complejo." (negativo, par — se invierte al calcular el puntaje) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q2` | 1-5 | media cruda 2.62 (n=16, min 1, max 4) |
| sus_q3 | Ítem 3 del SUS — "Me pareció que el sistema era fácil de usar." (positivo, impar) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q3` | 1-5 | media cruda 3.56 (n=16, min 1, max 5) |
| sus_q4 | Ítem 4 del SUS — "Creo que necesitaría el apoyo de una persona técnica para poder usar este sistema." (negativo, par) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q4` | 1-5 | media cruda 2.56 (n=16, min 1, max 5) |
| sus_q5 | Ítem 5 del SUS — "Encontré que las diversas funciones de este sistema estaban bastante bien integradas." (positivo, impar) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q5` | 1-5 | media cruda 3.50 (n=16, min 1, max 5) |
| sus_q6 | Ítem 6 del SUS — "Me pareció que había demasiada inconsistencia en este sistema." (negativo, par) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q6` | 1-5 | media cruda 2.69 (n=16, min 1, max 5) |
| sus_q7 | Ítem 7 del SUS — "Imagino que la mayoría de las personas aprenderían a usar este sistema muy rápidamente." (positivo, impar) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q7` | 1-5 | media cruda 3.81 (n=16, min 2, max 5) |
| sus_q8 | Ítem 8 del SUS — "Encontré el sistema muy engorroso o difícil de usar." (negativo, par) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q8` | 1-5 | media cruda 2.69 (n=16, min 1, max 5) |
| sus_q9 | Ítem 9 del SUS — "Me sentí muy confiado/a al usar el sistema." (positivo, impar) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q9` | 1-5 | media cruda 3.38 (n=16, min 1, max 5) |
| sus_q10 | Ítem 10 del SUS — "Necesité aprender muchas cosas antes de poder empezar a usar este sistema." (negativo, par) | int | puntos (escala Likert 1-5) | `sus/sus-raw.csv`, columna `Q10` | 1-5 | media cruda 2.94 (n=16, min 1, max 5); ítem con peor contribución normalizada (2.06/4), ver `sus/REPORTE-SUS.md` |
| lh_performance_mobile | Score Lighthouse Performance, perfil mobile, despliegue público, 3 rutas × 3 corridas (T-P4), post-remediación | int | puntos (0-100) | `lighthouse/lhci-20260904-1545-mobile-prod-{explorar,explorar_creadores,auth_login}-run{1,2,3}.report.json` | ≥ 80 | `/explorar`: 93/95/95 · `/explorar/creadores`: 93/95/94 · `/auth/login`: 87/94/94 — línea base (10:20, antes de remediar): `/explorar` 93/95/95 · `/creadores` 97/96/96 · `/login` 82/80/83 |
| lh_performance_desktop | Score Lighthouse Performance, perfil desktop, despliegue público, 3 rutas × 3 corridas (T-P4), post-remediación | int | puntos (0-100) | `lighthouse/lhci-20260904-1545-desktop-prod-{explorar,explorar_creadores,auth_login}-run{1,2,3}.report.json` | ≥ 80 | `/explorar`: **94/95/92** (antes 73/74/75, bajo el umbral — CLS 0.315→0.000, LCP 1.9s→1.43s tras reservar espacio de imagen y priorizar carga, ver `lighthouse/REPORTE-LIGHTHOUSE.md` § "Resultados de remediación") · `/explorar/creadores`: 99/99/99 · `/auth/login`: 99/100/100 |
| lh_accessibility | Score Lighthouse Accessibility, despliegue público, 3 rutas × 3 corridas c/u (mobile y desktop coinciden por ruta), post-remediación | int | puntos (0-100) | `lighthouse/lhci-20260904-1545-{mobile,desktop}-prod-*-run{1,2,3}.report.json` | ≥ 90 | `/explorar`: **93** (antes 89, bajo el umbral) · `/explorar/creadores`: **92** (antes 87, bajo el umbral) · `/auth/login`: 93 — causas ya corregidas: `color-contrast` (slate-400/300→500/600), `heading-order` (h2 sr-only insertado), `image-redundant-alt` (alt="" en imagen redundante con h3 visible); `color-contrast` en `text-teal-600` detectado y corregido después de esta corrida, pendiente de re-medir |
| lh_cls_desktop_explorar | Cumulative Layout Shift, `/explorar`, perfil desktop, despliegue público | float | — (0 = sin salto) | `lighthouse/lhci-20260904-1545-desktop-prod-explorar-run{1,2,3}.report.json`, `audits['cumulative-layout-shift'].numericValue` | < 0.1 (bueno) | **0.000** (las 3 corridas) — antes 0.315 (10:20, ver Adenda OBS-P4-01) |
| lh_lcp_desktop_explorar | Largest Contentful Paint, `/explorar`, perfil desktop, despliegue público | float | s | `lighthouse/lhci-20260904-1545-desktop-prod-explorar-run{1,2,3}.report.json`, `audits['largest-contentful-paint'].numericValue` | < 2.5 (bueno) | 1.43 / 1.35 / 1.61 — antes 1.9s (10:20) |
| lh_best_practices | Score Lighthouse Best Practices, despliegue público, 3 rutas × 3 corridas c/u | int | puntos (0-100) | `lighthouse/lhci-20260904-1545-{mobile,desktop}-prod-*-run{1,2,3}.report.json` | ≥ 90 | 96 (las 3 rutas, ambos perfiles, las 3 corridas) — sin cambio respecto a la línea base |
| lh_seo | Score Lighthouse SEO, despliegue público, 3 rutas × 3 corridas c/u | int | puntos (0-100) | `lighthouse/lhci-20260904-1545-{mobile,desktop}-prod-*-run{1,2,3}.report.json` | ≥ 90 | 100 (las 3 rutas, ambos perfiles, las 3 corridas) — sin cambio respecto a la línea base |
| lh_performance_mobile_localhost_2026-08-17 | (Histórico, ya no citado como evidencia de cumplimiento) Score Performance mobile, localhost, solo portada, 3 corridas | int | puntos (0-100) | `lighthouse/lhci-20260817-0315-mobile-run{1,2,3}.json` | ≥ 80 | 81 / 81 / 80 — reemplazado por `lh_performance_mobile` (T-P4, OBS-P4-01: exige URL pública y más de una ruta); se conserva el archivo, no se borra |
| lh_performance_desktop_localhost_2026-08-17 | (Histórico, ya no citado como evidencia de cumplimiento) Score Performance desktop, localhost, solo portada, 3 corridas | int | puntos (0-100) | `lighthouse/lhci-20260817-0320-desktop-run{1,2,3}.json` | ≥ 80 | 100 / 100 / 100 — reemplazado por `lh_performance_desktop` |
| lh_publico_20260904-1020_baseline | (Histórico, línea base pre-remediación — ya no citado como evidencia de cumplimiento) Scores de la primera corrida contra el despliegue público, antes de corregir CLS/LCP/accesibilidad | int | puntos (0-100) | `lighthouse/lhci-20260904-1020-{mobile,desktop}-prod-*-run{1,2,3}.report.json` | ≥80 perf / ≥90 resto | `/explorar` desktop perf **73/74/75** (falla), accessibility `/explorar` **89**/`/creadores` **87** (fallan) — reemplazado por `lh_performance_desktop`/`lh_accessibility` (corrida 1545); se conservan los archivos como evidencia del proceso, no se borran |
| jacoco_lines_pct | Cobertura de líneas | float | % | `jacoco/report.xml` | ≥ 70 | **72.0** (Medición v1.0.0. Histórico Entrega 3: 23.0, ver `jacoco/REPORTE-JACOCO.md`) |
| jacoco_branches_pct | Cobertura de ramas | float | % | `jacoco/report.xml` | ≥ 70 | **62.5** (Medición v1.0.0. Histórico Entrega 3: 13.8) |
| jacoco_complexity_pct | Cobertura de complejidad ciclomática | float | % | `jacoco/report.xml` | — (no exigido por la guía) | **56.5** (Medición v1.0.0. Histórico Entrega 3: 16.8) |
| owasp_a01_status | Resultado control A01 (control de acceso) | string (pass/fail) | — | `sec/owasp/a01-control-acceso.txt` | pass | pass |
| owasp_a02_status | Resultado control A02 (TLS) | string (pass/fail) | — | `sec/owasp/a02-tls.txt` | pass | pass |
| owasp_a03_status | Resultado control A03 (inyección) | string (pass/fail) | — | `sec/owasp/a03-inyeccion.txt` | pass | pass |
| owasp_a05_status | Resultado control A05 (cabeceras) | string (pass/fail) | — | `sec/owasp/a05-cabeceras.txt` | pass | pass |
| owasp_a07_status | Resultado control A07 (rate limiting) | string (pass/fail) | — | `sec/owasp/a07-rate-limit.txt` | pass | pass |
| owasp_a09_status | Resultado control A09 (logging/monitoreo) | string (pass/fail) | — | `sec/owasp/a09-logging.txt` | pass | pass |
| zap_baseline_result | Resultado agregado del escaneo OWASP ZAP baseline | string | FAIL-NEW/WARN-NEW/PASS | `sec/zap/` | 0 FAIL-NEW | 0 FAIL-NEW · 8 WARN-NEW · 59 PASS (ver `sec/REPORTE-SEC.md`) |
| static_analysis_sql_findings | Hallazgos de inyección SQL (SpotBugs + find-sec-bugs) | int | conteo | `sec/static-analysis/` | 0 | 0 |

## Depósito Zenodo del dataset

Este dataset (todo el contenido de `docs/mediciones/`) está depositado por separado
del software en Zenodo, con DOI propio y licencia Creative Commons Attribution 4.0
International (CC BY 4.0): **[`10.5281/zenodo.22236251`](https://doi.org/10.5281/zenodo.22236251)**
(publicado 01-09-2026, siguiendo el principio de citación independiente de software y
datos, Bloque D.3 de la guía de la Entrega Final).

## Procedencia de Datos

> [!NOTE]
> Las notas metodológicas y de trazabilidad de estas mediciones (incluyendo la justificación de datos y advertencias de muestreo) han sido extraídas a su propio documento formal. Por favor refiérase a **[DATA-PROVENANCE.md](DATA-PROVENANCE.md)** para conocer el origen exacto y limitaciones de cada variable de este diccionario.
