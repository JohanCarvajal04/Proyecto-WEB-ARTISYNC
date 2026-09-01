# Plan de Mejora — Lighthouse ≥90 en las 4 categorías

> Basado en el análisis del reporte real [`lhci-20260730-2009.json`](lhci-20260730-2009.json)
> (Lighthouse 12.6.1, perfil móvil, `throttlingMethod: simulate` a 1638.4 Kbps / 150 ms RTT /
> 4× CPU slowdown). Todos los números de esta página salen de ese archivo, no de estimaciones
> genéricas.

## Estado actual

| Categoría | Score | Meta | Brecha |
|---|---|---|---|
| Performance | **56** | ≥90 | −34 |
| Accessibility | **100** | ≥90 | ✅ ya cumple |
| Best Practices | **100** | ≥90 | ✅ ya cumple |
| SEO | **82** | ≥90 | −8 |

Solo hay que mover **Performance** y **SEO**. Accessibility y Best Practices ya están en 100 y el
plan no debe romperlos.

---

## 1. Diagnóstico de Performance

### Métricas medidas

| Métrica | Valor | Score |
|---|---|---|
| First Contentful Paint | **9.5 s** | 0 |
| Largest Contentful Paint | **9.9 s** | 0 |
| Speed Index | 9.5 s | 0.11 |
| Time to Interactive | 9.9 s | 0.27 |
| Total Blocking Time | 0 ms | ✅ 1 |
| Cumulative Layout Shift | 0 | ✅ 1 |

Dato clave: **TBT y CLS ya son perfectos**. El problema no es JavaScript pesado bloqueando el hilo
principal ni saltos de layout — es **puramente tiempo de transferencia de red**. Eso simplifica el
plan: no hace falta refactorizar la aplicación, hace falta dejar de mandar bytes innecesarios.

### Causa raíz: el peso de la fuente de iconos

Descomposición de los 1 575 KB que descarga la página, con el tiempo de transferencia que implica
cada uno al ancho de banda simulado por Lighthouse (204 800 bytes/s):

| Recurso | Bytes | Segundos @1638 Kbps | % del peso |
|---|---:|---:|---:|
| **Material Symbols (woff2)** | **1 125 765** | **5.50 s** | **71 %** |
| `main-*.js` | 273 266 | 1.33 s | 17 % |
| `chunk-B32aTgIr.js` | 49 734 | 0.24 s | 3 % |
| Inter (woff2) | 48 464 | 0.24 s | 3 % |
| `styles-*.css` | 38 322 | 0.19 s | 2 % |
| Manrope (woff2) | 24 605 | 0.12 s | 2 % |
| `favicon.ico` | 15 330 | 0.07 s | 1 % |
| **TOTAL** | **1 575 486** | **7.69 s** | 100 % |

**Una sola fuente de iconos representa el 71 % del peso de la página y 5.5 de los 7.7 segundos de
descarga.** El `index.html` la pide así:

```html
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap" rel="stylesheet">
```

Los rangos `wght@100..700` y `FILL@0..1` piden la **fuente variable completa**, con los ~3 700
iconos del set y todos los ejes. La aplicación usa **44 iconos distintos**.

### Hallazgos secundarios

| Auditoría | Score | Impacto medido |
|---|---|---|
| `uses-text-compression` | 0 | **261 KB** desperdiciados — nginx no comprime nada |
| `render-blocking-resources` | 0 | 2 CSS de `fonts.googleapis.com` bloquean el render (1 020 ms + 238 ms) |
| `uses-long-cache-ttl` | 0.5 | 5 recursos con `cacheLifetimeMs=0` pese a tener hash en el nombre |
| `unused-javascript` | 0 | 125 KB de JS no usado en la carga inicial |
| `unminified-javascript` | ✅ 1 | ya está minificado |
| `legacy-javascript` | ✅ 1 | sin polyfills innecesarios |
| `duplicated-javascript` | ✅ 1 | sin duplicación entre chunks |

El elemento LCP es un `<h1 class="font-headline">` de la pantalla de login — texto, no imagen. No
hay imagen que optimizar; el LCP está atado a cuándo termina de despejarse la red.

---

## 2. Plan de mejora por fases

Ordenado por impacto/esfuerzo. Las fases 1–3 son las que mueven el score; la 4 es cosmética
para SEO y la 5 es opcional.

| Fase | Acción | Bytes ahorrados | Esfuerzo | Riesgo |
|---|---|---:|---|---|
| **1** | Subsetear + auto-hospedar Material Symbols | ~1 115 KB | 1–2 h | Medio |
| **2** | Activar gzip en nginx | ~261 KB | 15 min | Bajo |
| **3** | Auto-hospedar Inter + Manrope | 0 KB (elimina 1.26 s de bloqueo) | 1 h | Bajo |
| **4** | `meta description` + `robots.txt` (SEO) | — | 15 min | Nulo |
| **5** | Reducir JS no usado (lazy loading) | ~125 KB | 4–8 h | Alto |

### Proyección aritmética

Aplicando fases 1–3, el peso transferido queda:

| Recurso | Antes | Después | Nota |
|---|---:|---:|---|
| Material Symbols | 1 125 765 | ~10 000 | subset de 44 iconos, ejes fijos |
| `main-*.js` | 273 266 | 83 360 | gzip (cifra exacta del propio reporte) |
| `chunk-B32aTgIr.js` | 49 734 | 11 543 | gzip |
| `styles-*.css` | 38 322 | 6 987 | gzip |
| `index.html` | 8 433 | 2 365 | gzip |
| Inter + Manrope | 73 069 | 73 069 | woff2 ya está comprimido; gzip no ayuda |
| `favicon.ico` | 15 330 | 15 330 | sin cambio |
| **TOTAL** | **1 575 KB** | **~203 KB** | **−87 %** |

Tiempo de transferencia: **7.69 s → ~1.0 s**, más ~1.26 s eliminados de bloqueo de render por
terceros. Eso deja FCP/LCP en el rango de 1.5–2.5 s en vez de 9.5/9.9 s.

> **Advertencia honesta:** el score de Lighthouse no es una función lineal de estos milisegundos,
> así que no puedo garantizar un número exacto de antemano. Lo que sí sostiene la aritmética es que
> las tres métricas hoy en score 0 (FCP, LCP, Speed Index) entran cómodamente en rango de score
> alto, y TBT/CLS ya están en 1. **Hay que volver a medir para afirmar el resultado**, no darlo por
> hecho.

---

## 3. Detalle de implementación

### Fase 1 — Material Symbols: de 1.1 MB a ~10 KB

Los 44 iconos que usa la aplicación (extraídos de las plantillas y de las propiedades `icon:` de
los `.ts`):

```
account_balance, add_circle, arrow_back, arrow_forward, block, check_circle, chevron_left,
chevron_right, close, content_copy, create, dashboard, delete, edit, error, folder_special,
gpp_bad, group, group_off, key, laptop_mac, lock, lock_person, lock_reset, logout,
mark_email_read, menu, no_accounts, notifications, person_add, phonelink_lock, rate_review,
receipt_long, save, search, settings, shield_lock, shield_person, support_agent, sync, tune,
verified, verified_user, warning
```

**Paso 1a (validación rápida, 1 línea).** Google Fonts acepta el parámetro `icon_names` y ejes
fijos en vez de rangos. Reemplazar el `<link>` de Material Symbols en `src/index.html` por:

```
https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0&icon_names=account_balance,add_circle,...&display=swap
```

Fijarse en `@24,400,0,0` (valores puntuales) en lugar de `@100..700,0..1` (rangos): eso solo ya
elimina la fuente variable completa. Sirve para confirmar el diagnóstico en minutos.

**Paso 1b (la solución real).** Descargar el `.woff2` que devuelve esa URL, guardarlo en
`public/fonts/material-symbols-subset.woff2`, quitar el `<link>` a Google y declarar la fuente en
`src/styles.css`:

```css
@font-face {
  font-family: 'Material Symbols Outlined';
  font-style: normal;
  font-weight: 400;
  font-display: block; /* 'block' evita ver el texto de la ligadura antes de que cargue */
  src: url('/fonts/material-symbols-subset.woff2') format('woff2');
}
```

`font-display: block` es deliberado aquí y distinto del `swap` de las fuentes de texto: con `swap`,
un icono no cargado se muestra como su nombre literal en texto ("account_balance"), que es peor que
un hueco de 100 ms.

> **Riesgo a gestionar:** el subset rompe cualquier icono que no esté en la lista. La aplicación
> usa iconos dinámicos (`{{item.icon}}`, y ternarios como `{{is2faEnabled() ? 'verified_user' :
> 'gpp_bad'}}`), así que la lista se armó rastreando también las propiedades `icon:` de los `.ts`.
> **Antes de dar la fase por cerrada hay que recorrer visualmente las 17 vistas que usan
> `material-symbols-outlined`** y confirmar que ningún icono aparece como texto. Si más adelante se
> añade un icono nuevo, hay que acordarse de sumarlo al subset — conviene dejar la lista en un
> comentario junto al `@font-face`.

### Fase 2 — gzip en nginx

`artisync/Frontend/nginx.conf` hoy no tiene ninguna directiva de compresión. Añadir dentro del
bloque `server`:

```nginx
gzip on;
gzip_vary on;
gzip_min_length 1024;
gzip_proxied any;
gzip_comp_level 6;
gzip_types
    text/plain text/css text/xml
    application/javascript application/json application/xml
    image/svg+xml;
```

Notas:
- **No incluir `font/woff2`**: woff2 ya está comprimido internamente; recomprimirlo gasta CPU y no
  ahorra bytes.
- Se usa gzip y no brotli a propósito: la imagen `nginx:alpine` del `Dockerfile` no trae el módulo
  brotli, y añadirlo obligaría a compilar nginx desde fuente. gzip captura la mayor parte del
  beneficio (los 261 KB que el propio reporte estima) sin tocar la imagen base.

### Fase 3 — Auto-hospedar Inter y Manrope

Elimina las dos peticiones a `fonts.googleapis.com` que Lighthouse marca como render-blocking
(1 020 ms + 238 ms). Aunque el `index.html` ya tiene `preconnect`, el CSS de Google sigue siendo un
recurso bloqueante en el camino crítico, y detrás de él vienen aún los `.woff2` desde
`fonts.gstatic.com` — dos orígenes de terceros antes de poder pintar.

1. Descargar los `.woff2` de Inter (400/500/600) y Manrope (600/700).
2. Guardarlos en `public/fonts/`.
3. Quitar del `index.html` los dos `<link>` a `fonts.googleapis.com` y los dos `preconnect` (ya no
   hacen falta).
4. Declarar los `@font-face` en `styles.css` con `font-display: swap` (aquí sí `swap`: es texto, y
   es preferible verlo en una fuente de reserva a no verlo).
5. Añadir en `index.html` un preload de la fuente del LCP:
   ```html
   <link rel="preload" href="/fonts/manrope-600.woff2" as="font" type="font/woff2" crossorigin>
   ```
   El elemento LCP es un `<h1 class="font-headline">`, que usa Manrope — precargarla ataca
   directamente la métrica peor puntuada.

### Fase 4 — SEO 82 → 100

Las dos auditorías que fallan:

**`meta-description`** — añadir en `src/index.html`:
```html
<meta name="description" content="ARTISYNC — plataforma que conecta clientes con creadores digitales para gestionar pedidos, contratos y entregas de trabajo creativo.">
```

**`robots-txt`** — crear `public/robots.txt`:
```
User-agent: *
Allow: /
```

Aprovechar el mismo cambio para dos defectos evidentes que Lighthouse no marca como fallo pero
están mal:
- `<title>Frontend</title>` → debería ser el nombre real del producto.
- `<html lang="en">` → la interfaz está íntegramente en español; corresponde `lang="es"`. La
  auditoría `html-has-lang` pasa porque *hay* un `lang`, pero declarar el idioma equivocado afecta
  a lectores de pantalla y a la indexación.

### Fase 5 (opcional) — JS no usado

`main-*.js` tiene 99 KB sin usar y `chunk-B32aTgIr.js` otros 28 KB. Requiere revisar el
`app.routes.ts` y mover a `loadComponent`/`loadChildren` lo que no se necesita en la pantalla de
login.

**Recomendación: no hacerlo en esta iteración.** Es la fase con más riesgo de regresión funcional y
la que menos aporta una vez aplicadas las fases 1–3 (127 KB frente a los 1 115 KB de la fase 1).
Después de gzip, esos 125 KB pesan ~38 KB reales en la red. Dejarlo documentado como deuda técnica.

### Bonus de bajo esfuerzo — Cache-Control

No afecta a la primera carga (que es lo que mide Lighthouse), pero `uses-long-cache-ttl` está en 0.5
y suma al score de la categoría. Angular está configurado con `"outputHashing": "all"`, así que
todos los `.js`/`.css` llevan hash de contenido y es seguro cachearlos de forma agresiva:

```nginx
location ~* \.(?:js|css|woff2)$ {
    root /usr/share/nginx/html;
    add_header Cache-Control "public, max-age=31536000, immutable";
}

location = /index.html {
    root /usr/share/nginx/html;
    add_header Cache-Control "no-cache";
}
```

`index.html` **nunca** debe cachearse: es el archivo que apunta a los bundles con hash. Si se
cachea, el navegador seguirá pidiendo los bundles viejos tras un despliegue.

---

## 4. Verificación

Tras aplicar las fases, reconstruir el contenedor (la medición debe ser contra el contenedor, nunca
contra `ng serve`) y volver a medir:

```bash
docker compose -f artisync/docker-compose.yml up -d --build frontend
```

```bash
cd artisync/Frontend && lhci autorun --config=lighthouserc.json
```

Criterio de aceptación por fase:
- **Fase 1:** el `.woff2` de Material Symbols en el waterfall baja de 1.1 MB a <20 KB, y los 44
  iconos se ven correctamente en las 17 vistas.
- **Fase 2:** `uses-text-compression` pasa a score 1; las respuestas traen `Content-Encoding: gzip`.
- **Fase 3:** `render-blocking-resources` pasa a score 1; no hay peticiones a `fonts.googleapis.com`
  ni `fonts.gstatic.com` en el waterfall.
- **Fase 4:** SEO llega a 100.
- **Global:** Performance ≥90 **sin haber tocado los umbrales de `lighthouserc.json`**, y
  Accessibility/Best Practices siguen en 100.

---

## 5. Impacto sobre la trazabilidad de las mediciones

Estos cambios modifican el frontend, es decir, **el sistema medido**. Consecuencias para el
Bloque C:

- `lighthouse/REPORTE-LIGHTHOUSE.md` pasará a tener dos corridas contra **commits distintos**: la
  actual (`f05feeb`, Performance 56) y la posterior a las mejoras. Hay que reportar ambas con su
  commit, presentándolas como *antes/después* — que es una historia mejor que un único número, no
  un problema.
- Los bloques C.1 (k6), C.2 (OWASP) y C.5 (JaCoCo) miden **solo el backend** y no se ven afectados:
  no hay que repetirlos.
- El bloque C.3 (SUS) sí depende del frontend. Si las sesiones con participantes se corren
  *después* de estas mejoras, hay que dejar constancia en la ficha metodológica de que se evaluó la
  versión mejorada — mezclar participantes de antes y después invalidaría la media.

**Recomendación de secuencia:** aplicar las mejoras **antes** de reclutar a los participantes del
SUS, para que la prueba de usabilidad se haga sobre la versión que realmente se va a entregar y no
sobre una que tarda 9.5 s en pintar en móvil.
