# Reporte de Cobertura de Código — JaCoCo

- Fecha: 2026-07-30
- Commit: rama `entrega-3/mediciones-bloque-c`
- Comando: `./mvnw.cmd -B clean test` (plugin `jacoco-maven-plugin` 0.8.13, añadido en esta
  entrega — antes no existía en `pom.xml`, ver `OBS-09`)
- Artefactos crudos: [`report.xml`](report.xml), [`html/index.html`](html/index.html)

## Resultado global

| Métrica | Cobertura |
|---|---|
| Lines | 666 / 2893 = **23.0%** |
| Branches | 110 / 796 = **13.8%** |
| Complexity | 175 / 1039 = **16.8%** |

## Comparación con la Entrega 1B

No se pudo calcular: el repositorio no conserva otra rama ni un punto de comparación separado
para reconstruir el estado de la Entrega 1B de forma confiable en este entorno (se intentó vía
`git worktree` contra el commit histórico correspondiente y se descartó ese enfoque). Según
`docs/observaciones/OBSERVACIONES.md` (OBS-09), en la Entrega 1B la única prueba existente era
`ArtisyncApplicationTests` (carga de contexto autogenerada), por lo que la cobertura efectiva de
esa entrega era cercana a 0% — se documenta como referencia cualitativa, no como número medido.

| Métrica | Entrega 1B (referencia cualitativa) | Entrega 3 (medido) | Tendencia |
|---|---|---|---|
| Lines | ~0% (solo test de contexto) | 23.0% | ↑ |
| Branches | ~0% | 13.8% | ↑ |
| Complexity | ~0% | 16.8% | ↑ |

## Desglose por huecos de cobertura conocidos

Los módulos `pedido`, `legal` y `catalogo` no tienen pruebas unitarias de servicio (solo
`seguridad`, `comunicacion` y `social` las tienen — ver los 18 archivos bajo
`Backend/src/test/java`). Esto concentra la mayor parte de las líneas sin cubrir; ver el detalle
por paquete en [`html/index.html`](html/index.html).

## Nota de cumplimiento

El umbral de referencia mencionado en la guía (≥60%) **no se cumple** en esta entrega — se
reporta el número real medido, sin ajustar el umbral ni excluir paquetes para maquillar el
resultado. Queda como trabajo pendiente ampliar la cobertura de pruebas unitarias en los tres
módulos señalados.
