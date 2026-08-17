# Procedencia de Datos (Data Provenance)

Este documento detalla el origen y las condiciones de recolección de los datos documentados en el diccionario de datos principal (`DATA-DICTIONARY.md`).

## Trazabilidad General
Todas las variables con valor medido provienen de los archivos crudos referenciados en la columna "Fuente" del diccionario de datos. La mayoría de estas métricas automatizadas fueron generadas el **2026-07-30** contra el commit `f05feeb` (rama `entrega-3/mediciones-bloque-c`). Ninguno de estos archivos crudos fue editado a mano después de generarse, garantizando la inmutabilidad de la evidencia.

## Procedencia por Métrica

### Métricas de Usabilidad (SUS)
- `sus_score_mean`: Los datos se recolectaron el **2026-08-16** mediante pruebas con 16 participantes externos al equipo de desarrollo. 
- Los resultados se obtuvieron a partir de un export crudo de Google Forms conservado en `sus/sus-raw.csv`. 
- **Limitación metodológica:** El cuestionario aplicado utilizó una traducción española estándar del SUS de Brooke, que difiere textualmente (aunque no semánticamente, conservando el mismo orden y polaridad) de la especificada originalmente en `sus/instrucciones-formulario.md`. El impacto de esta variación está documentado en `sus/REPORTE-SUS.md`.

### Métricas de Rendimiento (Lighthouse)
- `lh_*`: Se realizaron dos rondas de medición:
  1. La evaluación original (`lhci-20260730-2009.json`), ejecutada sobre el commit `f05feeb`.
  2. La evaluación posterior a las optimizaciones (`lhci-20260730-2103-mejorado.json`), documentada en `lighthouse/PLAN-MEJORA-LIGHTHOUSE.md`.
- El diccionario de datos reporta los valores de la versión mejorada, la cual es la versión final de entrega. El histórico y la comparación se encuentran en `lighthouse/REPORTE-LIGHTHOUSE.md`.

### Métricas de Carga y Estrés (k6)
- `p95_latency_cold`: Cuenta con un valor medido (39.14 ms), pero presenta una **limitación metodológica** documentada en `perf/REPORTE-PERF.md` (el script no aísla de forma determinista un *miss* real por cada iteración). Aunque no se descarta el resultado bruto, no se debe interpretar como evidencia definitiva de que el caché frío es más rápido que el caliente.

### Cobertura de Código (JaCoCo)
- `jacoco_*_pct`: No existe una comparación numérica directa frente a la Entrega 1B debido a la refactorización arquitectónica. Las razones de este alcance están especificadas en `jacoco/REPORTE-JACOCO.md`.
