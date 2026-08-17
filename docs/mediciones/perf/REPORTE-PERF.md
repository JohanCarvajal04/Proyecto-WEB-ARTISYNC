# Reporte de Rendimiento — k6

- Fecha: 2026-07-30 (zona horaria del entorno de prueba: UTC-05:00)
- Commit base: `f05feeb` (rama `entrega-3/mediciones-bloque-c`)
- k6 version: v2.1.0 (go1.26.4, windows/amd64)
- Endpoint medido: `GET /api/v1/catalogo?page=0&size=20` (catálogo, con 202 servicios sembrados
  — ver `artisync/database/seed-medicion-servicios.sql`)
- Configuración: 50 VUs, 30s, 3 corridas independientes por escenario, ~60s de espera entre
  corridas. Caché de catálogo activo (`ADR-004`, TTL 60s).
- Script versionado (Entrega Final): [`k6/catalogo-load.js`](../../../k6/catalogo-load.js) —
  reconstrucción de esta configuración, añadida para que `make bench` sea reproducible desde una
  clonación limpia (ver nota de honestidad en el propio script sobre su relación con estos datos
  ya archivados).

## Escenario CALIENTE (calentamiento previo de 10s, cache ya poblado)

| Métrica | Valor |
|---|---|
| n total (3 corridas) | 4500 |
| Media | 21.38 ms |
| Mediana | 13.01 ms |
| Desviación típica | 32.71 ms |
| IC 95% | [20.42, 22.33] ms |
| p50 / p90 / p95 / p99 | 13.01 / 31.75 / 50.17 / 205.60 ms |
| Tasa de error ≥500 | 0.00% |
| Throughput | ≈48.5–49.0 req/s por corrida (`http_reqs`, ver consolas) |

**Umbral esperado:** p95 < 200ms — **Resultado: cumple** (umbral k6 `p(95)<200` pasó en las 3
corridas, ver `k6-console-run{1,2,3}.txt`).

## Escenario FRÍO (`redis-cli FLUSHALL` inmediatamente antes de cada corrida, sin calentamiento)

| Métrica | Valor |
|---|---|
| n total (3 corridas) | 4500 |
| Media | 13.62 ms |
| Mediana | 9.04 ms |
| Desviación típica | 14.52 ms |
| IC 95% | [13.20, 14.05] ms |
| p50 / p90 / p95 / p99 | 9.04 / 22.39 / 39.14 / 89.98 ms |
| Tasa de error ≥500 | 0.00% |

**Umbral esperado:** p95 < 500ms — **Resultado: cumple**.

## Advertencia metodológica — por qué "frío" no salió más lento que "caliente"

Los números anteriores muestran el escenario "frío" con **mejor** latencia que el "caliente", lo
cual no tiene una explicación causal razonable (Redis no puede ser más lento que Postgres para
esta consulta) y **no debe leerse como que el caché perjudica el rendimiento**. La explicación
real: el script de k6 golpea siempre la misma URL exacta (`?page=0&size=20`, sin variar
filtros), y `@Cacheable` usa esa combinación de parámetros como clave. Eso significa que, dentro
de una corrida de 30s con 1500 iteraciones, **como mucho 1 de esas 1500 peticiones es un miss
real contra Postgres** — el resto, en ambos escenarios "frío" y "caliente", terminan sirviéndose
desde Redis en cuanto la primera petición completa. La diferencia observada entre ambos
escenarios (21.38 ms vs. 13.62 ms de media) es ruido de ejecución — variabilidad de JIT/GC de la
JVM, carga del contenedor, no un efecto real de caché frío vs. caliente.

**Lo que sí se verificó de forma aislada y determinística** (no vía k6, sino con `curl` + `redis-cli`
durante la preparación del entorno): tras `FLUSHALL`, la primera petición a `/api/v1/catalogo`
crea la clave `catalogo::SimpleKey [...]` en Redis con TTL ≈60s, y la eliminación de esa clave al
crear/editar/eliminar un servicio (`@CacheEvict`) es inmediata — el mecanismo de caché funciona
correctamente, solo que el diseño del script de carga actual no lo ejercita de forma
representativa (todas las peticiones caen en la misma clave).

**Recomendación para una medición futura que sí aísle el efecto:** variar los parámetros de
`k6/script.js` por iteración (p. ej. página aleatoria entre 0 y 9, o filtro de categoría
aleatorio entre las 5 sembradas) para que cada iteración golpee una clave de caché distinta y el
ratio de hit/miss sea representativo de un catálogo con tráfico real. No se aplicó ese cambio en
esta sesión para no invalidar la comparabilidad de las 3 corridas ya ejecutadas bajo la misma
configuración.

## Artefactos

- `k6-run{1,2,3}.json` / `k6-console-run{1,2,3}.txt` — escenario caliente
- `k6-cold-run{1,2,3}.json` / `k6-console-cold-run{1,2,3}.txt` — escenario frío
- `analisis-perf.py` / `salida-analisis.txt` — análisis agregado
