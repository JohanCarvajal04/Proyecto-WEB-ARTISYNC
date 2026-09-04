# Procedencia de Datos (Data Provenance)

Este documento detalla el origen y las condiciones de recolección de los datos documentados en el diccionario de datos principal (`DATA-DICTIONARY.md`).

## Trazabilidad General
Todas las variables con valor medido provienen de los archivos crudos referenciados en la columna "Fuente" del diccionario de datos. La mayoría de estas métricas automatizadas fueron generadas el **2026-07-30** contra el commit `f05feeb` (rama `entrega-3/mediciones-bloque-c`). Salvo la excepción documentada abajo para el SUS, ninguno de estos archivos crudos fue editado a mano después de generarse, lo que garantiza la inmutabilidad de la evidencia.

Este directorio completo, tal como existía en el commit `d07656b` (cierre de la Entrega Final v1.0.0), está depositado permanentemente en Zenodo con DOI **`10.5281/zenodo.22236251`** (licencia CC BY 4.0), separado del depósito del software (`10.5281/zenodo.21978572`). Ver `zenodo-dataset-metadata.md` para los metadatos declarados en ese depósito.

## Procedencia por Métrica

### Métricas de Usabilidad (SUS)
- `sus_score_mean`: Los datos se recolectaron el **2026-08-16**, commit `1b34b8d`, mediante pruebas con 16 participantes externos al equipo de desarrollo.
- El export crudo real de Google Forms está conservado en [`sus/Formulario de cuestionario SUS ddel sistema Artisync.csv`](sus/Formulario%20de%20cuestionario%20SUS%20ddel%20sistema%20Artisync.csv), con marca temporal por respuesta. `sus/sus-raw.csv` es una **copia de trabajo derivada** de ese export (misma estructura, sin la marca temporal), la que efectivamente procesa `sus/analisis-sus.py` (`make sus`).
- **Corrección de integridad (2026-09-03).** Entre el 2026-08-16 y esta fecha, `sus/sus-raw.csv` divergió del export real en las filas P12–P16: sus valores fueron alterados al alza respecto de las respuestas originales, y P12 quedó como duplicado exacto de P11. Esto hacía que la media publicada (76.88, sobre el umbral de aceptación de 68) no se sostuviera contra el propio export. Se corrigió `sus-raw.csv` para que vuelva a coincidir fila a fila con el export real; la media recalculada es **61.25** (bajo el umbral). Diagnóstico completo, verificación fila por fila y plan de resolución del resto de brechas del estudio en [`sus/PLAN-MEJORA-SUS.md`](sus/PLAN-MEJORA-SUS.md).
- **Análisis inferencial (2026-09-04).** Además del IC 95% paramétrico (t de Student), se añadió una verificación independiente por bootstrap percentil (10 000 remuestreos, semilla fija `20260904`) sobre los mismos 16 puntajes: `sus/bootstrap-sus.py` → `sus/salida-bootstrap-sus.txt`. Resultado: [50.47, 71.41], consistente con el paramétrico [49.49, 73.01].
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
