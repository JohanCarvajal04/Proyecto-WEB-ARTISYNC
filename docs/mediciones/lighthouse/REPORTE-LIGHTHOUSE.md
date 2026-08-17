# Reporte de Accesibilidad y Calidad Web — Lighthouse

- Lighthouse version: 12.6.1 (vía `@lhci/cli` 0.15.1)
- Configuración: `formFactor: mobile`, `throttlingMethod: simulate`, 1638.4 Kbps / 150ms RTT /
  4× CPU slowdown, contra el **contenedor** de `docker compose`
  (`http://localhost:4200`, nginx sirviendo el build de producción de Angular) — no contra
  `ng serve`.

## Resumen — antes / después de aplicar el plan de mejora

Ver [`PLAN-MEJORA-LIGHTHOUSE.md`](PLAN-MEJORA-LIGHTHOUSE.md) para el diagnóstico completo y el
detalle de cada fase implementada.

| Categoría | Antes (2026-07-30) | Después (2026-07-30, mismo día) | Umbral | Cumple |
|---|---|---|---|---|
| Performance | 56/100 | **92/100** | ≥ 80 | ✅ Sí |
| Accessibility | 100/100 | 100/100 | ≥ 90 | ✅ Sí |
| Best Practices | 100/100 | 100/100 | ≥ 90 | ✅ Sí |
| SEO | 82/100 | **100/100** | ≥ 90 | ✅ Sí |

`lhci autorun` terminó con **"All results processed!"** y código de salida 0 — las 4 categorías
cumplen su umbral sin haber tocado `lighthouserc.json`.

- Corrida "antes" (commit `f05feeb`, sin las mejoras de fuentes/gzip/SEO):
  [`lhci-20260730-2009.json`](lhci-20260730-2009.json) / [`.html`](lhci-20260730-2009.html)
- Corrida "después" (rama `entrega-3/mediciones-bloque-c`, con las 4 fases del plan aplicadas):
  [`lhci-20260730-2103-mejorado.json`](lhci-20260730-2103-mejorado.json) /
  [`.html`](lhci-20260730-2103-mejorado.html)

## Métricas de carga — antes / después

| Métrica | Antes | Después |
|---|---|---|
| First Contentful Paint | 9.5 s (score 0) | **2.1 s** (score 0.82) |
| Largest Contentful Paint | 9.9 s (score 0) | **2.8 s** (score 0.84) |
| Speed Index | 9.5 s (score 0.11) | **2.1 s** (score 0.99) |
| Time to Interactive | 9.9 s (score 0.27) | **2.8 s** (score 0.97) |
| Total Blocking Time | 0 ms (score 1) | 160 ms (score 0.93) |
| Cumulative Layout Shift | 0 (score 1) | 0 (score 1) |
| Peso total transferido | 1 554 KiB | **200 KiB** (−87%) |

TBT subió levemente (0 → 160 ms) porque ahora el hilo principal sí tiene trabajo real que hacer
en el primer segundo (antes estaba ocioso esperando 7+ segundos de red); sigue con score alto
(0.93) y no afecta el resultado.

## Qué cambió (resumen técnico — detalle completo en `PLAN-MEJORA-LIGHTHOUSE.md`)

| Auditoría | Antes | Después |
|---|---|---|
| `render-blocking-resources` | score 0 | **score 1** |
| `uses-text-compression` | score 0 (261 KB desperdiciados) | **score 1** |
| `uses-long-cache-ttl` | score 0.5 | **score 1** (0 recursos sin cache-control) |
| `unused-javascript` | score 0 (125 KB) | score 0 (30 KB — mejoró por el efecto del gzip sobre el mismo bundle; no se tocó el código, queda como deuda técnica según el plan) |
| `meta-description` | score 0 | **score 1** |
| `robots-txt` | score 0 | **score 1** |

Causa raíz identificada: la fuente Material Symbols se pedía completa (rangos de peso/relleno
variables, ~3 700 iconos, 1.1 MB) cuando la aplicación usa 44. Ese único recurso era el 71% del
peso de la página. Subsetearla y auto-hospedar las 3 fuentes (Material Symbols, Inter, Manrope)
junto con activar gzip en nginx explica la mayor parte de la mejora.

## Accessibility y Best Practices

Se mantuvieron en 100/100 — el plan de mejora no las tocó, y se verificó que no se rompieron.

## Verificación funcional del subset de iconos

Antes de remedir, se verificó en el navegador (contra el contenedor, no una build local) que los
44 iconos subseteados siguen renderizando como glifo y no como texto literal: `document.fonts.check('24px "Material Symbols Outlined"')` → `true`, y una comprobación geométrica
(ancho del `<span>` ≈ `font-size`, no el ancho mucho mayor que tendría el texto plano) en las
vistas de login y registro. Ver el detalle de la lista de 44 iconos y el riesgo del subset en
`PLAN-MEJORA-LIGHTHOUSE.md` (fase 1).

## Trabajo pendiente (no bloqueante, documentado en el plan)

- `unused-javascript` (30 KB reales tras gzip): requeriría revisar lazy loading de rutas —
  descartado en el plan por relación esfuerzo/beneficio desfavorable una vez aplicadas las otras
  fases.
- Segunda corrida de consistencia sobre el estado "después": pendiente (opcional, no bloqueante
  para el checklist del Bloque C).

## Nota de trazabilidad

Estos cambios modifican el frontend medido. Los bloques C.1 (k6), C.2 (OWASP) y C.5 (JaCoCo) son
independientes del frontend y no se ven afectados. El bloque C.3 (SUS) sí depende de esta versión
— si las sesiones de usabilidad se corren después de este commit, deben evaluar esta versión
mejorada, no la original de 56/100 en Performance.
