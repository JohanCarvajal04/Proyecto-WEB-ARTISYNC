# Procedencia de Datos (Data Provenance)

Este documento detalla el origen y las condiciones de recolección de los datos documentados en el diccionario de datos principal (`DATA-DICTIONARY.md`).

## Trazabilidad General
Todas las variables con valor medido provienen de los archivos crudos referenciados en la columna "Fuente" del diccionario de datos. La mayoría de estas métricas automatizadas fueron generadas el **2026-07-30** contra el commit `f05feeb` (rama `entrega-3/mediciones-bloque-c`). Ninguno de estos archivos crudos fue editado a mano después de generarse, garantizando la inmutabilidad de la evidencia.

## Procedencia por Métrica

### Métricas de Usabilidad (SUS)
- `sus_score_mean`: Los datos se recolectaron el **2026-08-16**, commit `1b34b8d`, mediante pruebas con 16 participantes externos al equipo de desarrollo.
- Los resultados se obtuvieron a partir de un export crudo de Google Forms conservado en `sus/sus-raw.csv`, procesado por `sus/analisis-sus.py` (`make sus`).
- **Limitación metodológica:** El cuestionario aplicado utilizó una traducción española estándar del SUS de Brooke, que difiere textualmente (aunque no semánticamente, conservando el mismo orden y polaridad) de la especificada originalmente en `sus/instrucciones-formulario.md`. El impacto de esta variación está documentado en `sus/REPORTE-SUS.md`.

### Métricas de Rendimiento (Lighthouse)
- `lh_*`: Se realizaron tres rondas de medición:
  1. La evaluación original (`lhci-20260730-2009.json`), ejecutada sobre el commit `f05feeb`.
  2. La evaluación posterior a las optimizaciones (`lhci-20260730-2103-mejorado.json`), documentada en `lighthouse/PLAN-MEJORA-LIGHTHOUSE.md`.
  3. Las 6 corridas de la Entrega Final (3 mobile + 3 desktop, `lhci-20260817-*-{mobile,desktop}-run{1,2,3}.json`), generadas el **2026-08-17** en el commit `69e43b8` vía `make lighthouse` (ver detalle de la corrida en `lighthouse/REPORTE-LIGHTHOUSE.md`).
- El diccionario de datos reporta los valores de la ronda 3 (Entrega Final, mobile + desktop), que es la vigente. El histórico completo y la comparación se encuentran en `lighthouse/REPORTE-LIGHTHOUSE.md`.

### Métricas de Carga y Estrés (k6)
- `p95_latency_cold`: Cuenta con un valor medido (39.14 ms), pero presenta una **limitación metodológica** documentada en `perf/REPORTE-PERF.md` (el script no aísla de forma determinista un *miss* real por cada iteración). Aunque no se descarta el resultado bruto, no se debe interpretar como evidencia definitiva de que el caché frío es más rápido que el caliente.

### Cobertura de Código (JaCoCo)
- `jacoco_*_pct`: la medición vigente (72.0 % líneas / 62.5 % ramas) se generó el **2026-08-16**, commit `11ac931`, con `./mvnw.cmd -B clean test` (522 pruebas, 0 fallos). Reemplaza la medición del 2026-07-30 (23.0 % / 13.8 %), conservada como histórico en `jacoco/REPORTE-JACOCO.md`. No existe una comparación numérica directa frente a la Entrega 1B debido a la refactorización arquitectónica; las razones de ese alcance están especificadas en el mismo reporte.

### Seguridad — Escaneo ZAP y análisis estático (Entrega Final, A.2.3)
- `zap_baseline_result`: generado el **2026-08-16**, commit `8b88512`, con `make audit-zap` (`zap-baseline.py` contra `http://localhost:4200`, imagen `ghcr.io/zaproxy/zaproxy:stable`). Reporte crudo en `sec/zap/`, narrativa en `sec/REPORTE-SEC.md`.
- `static_analysis_sql_findings`: generado el **2026-08-16**, commit `8b88512`, con `make audit` (SpotBugs 4.8.6.6 + find-sec-bugs 1.13.0 dentro de un contenedor `maven:3.9-eclipse-temurin-21`). Reporte crudo en `sec/static-analysis/`.
