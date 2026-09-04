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
- Segunda corrida de consistencia sobre el estado "después": resuelto en la sección siguiente
  (Entrega Final: 3 corridas mobile + 3 desktop).

## Entrega Final — perfil mobile (3 corridas) y perfil desktop (3 corridas)

- Fecha: 2026-08-17
- Comando: `make lighthouse` (ver `Makefile`). Corre `@lhci/cli` 0.15.1 dentro de un contenedor
  efímero `node:20-bookworm-slim` + `chromium`, en vez de invocar `npx` directo en la máquina
  del desarrollador — `chrome-launcher` (dependencia de Lighthouse) tiene un bug de limpieza de
  directorio temporal en Windows (`EPERM` al borrar el perfil de Chrome) que aborta la corrida
  antes de escribir el reporte. El contenedor comparte el namespace de red del propio contenedor
  `frontend` (`--network container:pfc_frontend`) para que `http://localhost:4200/` (la URL ya
  configurada en `lighthouserc.mobile.json`/`lighthouserc.desktop.json`, sin modificar) resuelva
  igual que en la máquina del desarrollador — usar el nombre DNS interno de compose (`frontend`)
  en su lugar rompe la detección de "contexto seguro" de Lighthouse para `localhost` y produce
  falsos negativos en `is-on-https`/`redirects-http` que no reflejan la aplicación real (se
  descartó una primera corrida contaminada por este efecto).
- Objetivo: `http://localhost:4200/`, contenedor `docker-compose.lighthouse.yml` (build de
  producción de Angular servido por nginx — no `ng serve`).
- Configuración: `lighthouserc.mobile.json` (formFactor mobile, throttling simulado 1638.4
  Kbps/150ms RTT/4× CPU, `numberOfRuns: 3`) y `lighthouserc.desktop.json` (formFactor desktop,
  throttling simulado 10240 Kbps/40ms RTT/1× CPU, `numberOfRuns: 3`).

### Resultados — mobile (3/3 corridas, `lhci-20260817-0315-mobile-run{1,2,3}`)

| Categoría | Run 1 | Run 2 | Run 3 | Umbral | Cumple |
|---|---|---|---|---|---|
| Performance | 81 | 81 | 80 | ≥ 80 | ✅ Sí |
| Accessibility | 93 | 93 | 93 | ≥ 90 | ✅ Sí |
| Best Practices | 96 | 96 | 96 | ≥ 90 | ✅ Sí |
| SEO | 100 | 100 | 100 | ≥ 90 | ✅ Sí |

`lhci autorun` terminó con **"All results processed!"** y código de salida 0 en ambos perfiles.

### Resultados — desktop (3/3 corridas, `lhci-20260817-0320-desktop-run{1,2,3}`)

| Categoría | Run 1 | Run 2 | Run 3 | Umbral | Cumple |
|---|---|---|---|---|---|
| Performance | 100 | 100 | 100 | ≥ 80 | ✅ Sí |
| Accessibility | 93 | 93 | 93 | ≥ 90 | ✅ Sí |
| Best Practices | 96 | 96 | 96 | ≥ 90 | ✅ Sí |
| SEO | 100 | 100 | 100 | ≥ 90 | ✅ Sí |

### Métricas de carga (run 1 de cada perfil)

| Métrica | Mobile | Desktop |
|---|---|---|
| First Contentful Paint | 1.9 s | 0.4 s |
| Largest Contentful Paint | 3.0 s | 0.6 s |
| Speed Index | 1.9 s | 0.5 s |
| Total Blocking Time | 70 ms | 0 ms |
| Cumulative Layout Shift | 0.231 | 0.021 |

### Hallazgos reales que impiden el 100/100 (no se maquillaron los números)

- **Accessibility 93/100 en ambos perfiles**: `target-size` — algunos elementos táctiles no
  cumplen el tamaño/espaciado mínimo recomendado.
- **Best Practices 96/100 en ambos perfiles**: `errors-in-console` — al cargar la página sin
  sesión iniciada, el navegador registra un `403` de red en `GET /api/auth/refresh` (comportamiento
  esperado del interceptor JWT al intentar refrescar un token que no existe para un visitante
  anónimo, pero Lighthouse lo cuenta como error de consola). Candidato a mejora: que el frontend
  evite disparar ese refresh cuando no hay sesión activa.
- **CLS 0.231 en mobile** (por encima del umbral "bueno" de 0.1, aunque no impide superar el
  umbral de Performance ≥ 80): contribuye a que el perfil mobile puntúe 80–81 en vez de más alto;
  el perfil desktop no lo sufre de forma perceptible (0.021) porque el layout no se recalcula de
  la misma manera en viewport ancho.

Ninguno de estos tres hallazgos hace fallar los umbrales exigidos por la guía de la Entrega Final
(Performance ≥ 80, Accessibility/Best Practices/SEO ≥ 90), pero quedan documentados como mejoras
reales pendientes en vez de ocultarse.

### Reportes archivados

- Mobile: [`run1`](lhci-20260817-0315-mobile-run1.html) / [`run2`](lhci-20260817-0315-mobile-run2.html)
  / [`run3`](lhci-20260817-0315-mobile-run3.html) (`.json` homónimos en esta misma carpeta)
- Desktop: [`run1`](lhci-20260817-0320-desktop-run1.html) / [`run2`](lhci-20260817-0320-desktop-run2.html)
  / [`run3`](lhci-20260817-0320-desktop-run3.html) (`.json` homónimos en esta misma carpeta)

## Nota de trazabilidad

Estos cambios modifican el frontend medido. Los bloques C.1 (k6), C.2 (OWASP) y C.5 (JaCoCo) son
independientes del frontend y no se ven afectados. El bloque C.3 (SUS) sí depende de esta versión
— si las sesiones de usabilidad se corren después de este commit, deben evaluar esta versión
mejorada, no la original de 56/100 en Performance.

## Adenda OBS-P4-01 (2026-09-04) — despliegue público, 3 rutas

La corrida de la Entrega Final (sección anterior) auditó **localhost, solo la portada**. La guía
(§4.5) exige 3 corridas por perfil contra el **despliegue público**, sobre **más de una ruta**, con
`requestedUrl` apuntando a la URL pública real. Esta adenda **reemplaza** esa corrida como
evidencia oficial de cumplimiento del bloque P4 (los 6 JSON de 2026-08-17 se conservan en el
repositorio como evidencia histórica del proceso de optimización, no se borran, pero ya no se citan
como el cumplimiento del criterio).

- **Objetivo:** `https://artisync-frontend.onrender.com` (despliegue real en Render, verificado
  accesible y sirviendo datos reales del catálogo — no una copia local).
- **Rutas auditadas** (`collect.url` en `lighthouserc.mobile.json`/`lighthouserc.desktop.json`,
  las 3 públicas sin parámetro dinámico dependiente del seed): `/explorar` (portada del catálogo),
  `/explorar/creadores` (listado de creadores), `/auth/login` (login).
- **Comando:** `make lighthouse` — ya no depende de `docker-compose.lighthouse.yml` ni de
  `--network container:pfc_frontend` (innecesario contra una URL HTTPS pública real; Lighthouse
  detecta "contexto seguro" de forma nativa).
- **3 corridas × 3 rutas × 2 perfiles = 18 auditorías reales**, `requestedUrl`/`finalUrl` apuntando
  a `artisync-frontend.onrender.com` (verificable en cada JSON), 175 auditorías individuales por
  reporte (JSON completo, no solo las 4 puntuaciones de categoría).

### Resultados — mobile (`lhci-20260904-1020-mobile-prod-*-run{1,2,3}`)

| Ruta | Performance | Accessibility | Best Practices | SEO |
|---|---|---|---|---|
| `/explorar` | 93 / 95 / 95 ✅ | 89 / 89 / 89 ❌ (<90) | 96 / 96 / 96 ✅ | 100 / 100 / 100 ✅ |
| `/explorar/creadores` | 97 / 96 / 96 ✅ | 87 / 87 / 87 ❌ (<90) | 96 / 96 / 96 ✅ | 100 / 100 / 100 ✅ |
| `/auth/login` | 82 / 80 / 83 ✅ | 93 / 93 / 93 ✅ | 96 / 96 / 96 ✅ | 100 / 100 / 100 ✅ |

### Resultados — desktop (`lhci-20260904-1020-desktop-prod-*-run{1,2,3}`)

| Ruta | Performance | Accessibility | Best Practices | SEO |
|---|---|---|---|---|
| `/explorar` | **73 / 74 / 75 ❌ (<80)** | 89 / 89 / 89 ❌ (<90) | 96 / 96 / 96 ✅ | 100 / 100 / 100 ✅ |
| `/explorar/creadores` | 99 / 99 / 99 ✅ | 87 / 87 / 87 ❌ (<90) | 96 / 96 / 96 ✅ | 100 / 100 / 100 ✅ |
| `/auth/login` | 99 / 99 / 99 ✅ | 93 / 93 / 93 ✅ | 96 / 96 / 96 ✅ | 100 / 100 / 100 ✅ |

`lhci autorun` terminó con código de salida 1 en ambos perfiles (assertion failure), no 0 — se
documenta tal cual, sin maquillar: **los umbrales de Performance (`/explorar` desktop) y de
Accessibility (`/explorar` y `/explorar/creadores`, ambos perfiles) ya no se cumplen contra el
despliegue público**, a diferencia de la corrida de localhost de agosto. Es un hallazgo nuevo y
real, no un error de medición — los 3 hallazgos que ya documentaba la sección anterior
(`target-size`, `errors-in-console`, CLS elevado) seguían presentes y se confirmaron de nuevo aquí.

### Causas raíz identificadas (no solo el número, el diagnóstico)

- **Accessibility 87-89/100** (antes 93/100 en local): 3 auditorías nuevas en rojo —
  `color-contrast` (contraste de texto/fondo insuficiente en algún elemento del catálogo/listado de
  creadores), `heading-order` (jerarquía de encabezados no secuencial) e
  `image-redundant-alt` (`alt` redundante con el texto visible de la imagen). Ninguna aparecía en
  la corrida de localhost — hipótesis más probable: la corrida de agosto se hizo contra el seed de
  desarrollo (200 servicios de relleno con texto genérico), y el contenido real sembrado en
  producción expone patrones de marcado que el contenido de prueba no ejercitaba. `/auth/login`
  (formulario estático, sin contenido dinámico del catálogo) sigue en 93 — refuerza que la causa es
  el contenido real del catálogo/creadores, no una regresión de la plantilla base.
- **Performance 73-75/100 en `/explorar` desktop** (el resto de rutas/perfiles sigue ≥80): impulsado
  por **Cumulative Layout Shift 0.315** (umbral "bueno" es <0.1) y **Largest Contentful Paint 1.9s**
  (score 0.66). Oportunidades detectadas en el propio reporte: `uses-responsive-images` (43 KiB),
  `unused-javascript` (55 KiB), `modern-image-formats` (62 KiB), `uses-rel-preconnect` (~300ms),
  `prioritize-lcp-image` (~220ms) — consistente con las imágenes de `picsum.photos` (servidas sin
  dimensiones fijas ni `srcset`) del catálogo real causando el salto de layout, algo que el mismo
  seed de prueba con datos más uniformes no exhibía de forma tan marcada. Curiosamente `/explorar`
  en mobile (93-95) no sufre esto con la misma severidad — el viewport más angosto y el throttling
  ya fuerzan un layout más simple, enmascarando parte del CLS que sí se nota a resolución desktop.

Estos 2 hallazgos (accesibilidad y CLS/LCP en `/explorar`) quedan como **trabajo futuro real**
detectado por auditar el despliegue público en vez de un entorno de desarrollo controlado — el tipo
de hallazgo que la guía busca precisamente al exigir medir contra producción.

### Reportes archivados

- Mobile: `lhci-20260904-1020-mobile-prod-{explorar,explorar_creadores,auth_login}-run{1,2,3}.report.{json,html}`
- Desktop: `lhci-20260904-1020-desktop-prod-{explorar,explorar_creadores,auth_login}-run{1,2,3}.report.{json,html}`
- Manifiestos: `lhci-20260904-1020-{mobile,desktop}-prod-manifest.json`
