# Diccionario de Datos — Mediciones Tercera Entrega

| Variable | Descripción | Tipo | Unidad | Fuente | Rango/umbral esperado | Valor medido |
|---|---|---|---|---|---|---|
| p95_latency_hot | Percentil 95 de latencia, cache caliente | float | ms | `perf/k6-run{1,2,3}.json` | < 200 | 50.17 |
| p95_latency_cold | Percentil 95 de latencia, cache recién vaciado | float | ms | `perf/k6-cold-run{1,2,3}.json` | < 500 | 39.14 (ver advertencia metodológica en `perf/REPORTE-PERF.md`: el script actual no aísla un miss real por iteración) |
| error_rate_5xx | Tasa de respuestas ≥500 | float | % | `perf/k6-run{1,2,3}.json` + `perf/k6-cold-run{1,2,3}.json` | = 0 | 0.00% |
| throughput_rps | Throughput agregado | float | req/s | `perf/k6-console-run*.txt` | — | ≈48.5–49.0 |
| sus_score_mean | Puntaje SUS promedio | float | puntos (0-100) | `sus/sus-raw.csv` | > 68 | **76.88** (n=16, DT 14.48, IC 95% [69.16, 84.59], Bangor B, ver `sus/REPORTE-SUS.md`) |
| lh_performance | Score Lighthouse Performance | int | puntos (0-100) | `lighthouse/lhci-20260730-2103-mejorado.json` | ≥ 80 | **92** (antes 56, ver `lighthouse/PLAN-MEJORA-LIGHTHOUSE.md`) |
| lh_accessibility | Score Lighthouse Accessibility | int | puntos (0-100) | `lighthouse/lhci-20260730-2103-mejorado.json` | ≥ 90 | 100 |
| lh_best_practices | Score Lighthouse Best Practices | int | puntos (0-100) | `lighthouse/lhci-20260730-2103-mejorado.json` | ≥ 90 | 100 |
| lh_seo | Score Lighthouse SEO | int | puntos (0-100) | `lighthouse/lhci-20260730-2103-mejorado.json` | ≥ 90 | **100** (antes 82) |
| jacoco_lines_pct | Cobertura de líneas | float | % | `jacoco/report.xml` | ≥ 60 | 23.0 |
| jacoco_branches_pct | Cobertura de ramas | float | % | `jacoco/report.xml` | ≥ 60 | 13.8 |
| jacoco_complexity_pct | Cobertura de complejidad ciclomática | float | % | `jacoco/report.xml` | ≥ 60 | 16.8 |
| owasp_a01_status | Resultado control A01 (control de acceso) | string (pass/fail) | — | `sec/owasp/a01-control-acceso.txt` | pass | pass |
| owasp_a02_status | Resultado control A02 (TLS) | string (pass/fail) | — | `sec/owasp/a02-tls.txt` | pass | pass |
| owasp_a03_status | Resultado control A03 (inyección) | string (pass/fail) | — | `sec/owasp/a03-inyeccion.txt` | pass | pass |
| owasp_a05_status | Resultado control A05 (cabeceras) | string (pass/fail) | — | `sec/owasp/a05-cabeceras.txt` | pass | pass |
| owasp_a07_status | Resultado control A07 (rate limiting) | string (pass/fail) | — | `sec/owasp/a07-rate-limit.txt` | pass | pass |
| owasp_a09_status | Resultado control A09 (logging/monitoreo) | string (pass/fail) | — | `sec/owasp/a09-logging.txt` | pass | pass |

## Procedencia de Datos

> [!NOTE]
> Las notas metodológicas y de trazabilidad de estas mediciones (incluyendo la justificación de datos y advertencias de muestreo) han sido extraídas a su propio documento formal. Por favor refiérase a **[DATA-PROVENANCE.md](DATA-PROVENANCE.md)** para conocer el origen exacto y limitaciones de cada variable de este diccionario.
