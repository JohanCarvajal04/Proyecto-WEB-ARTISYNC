# Plan de Ejecución — Mediciones Bloque C (Tercera Entrega)

> **Propósito.** Traducir `guia-mediciones-tercera-entrega.md` (genérica, escrita para Linux y
> con endpoints de ejemplo) a un plan ejecutable **contra el estado real de este repositorio**,
> en **Windows 11 / PowerShell**, con Spring Boot 4.1 + Angular 22 + Postgres 16 + Redis 7.
>
> **Regla transversal heredada de la guía:** ningún archivo crudo (`.json`, `.csv`, `.xml`, `.txt`)
> se edita a mano después de generado. Si un valor sale mal, se vuelve a correr la prueba.
>
> - Fecha de redacción del plan: 2026-07-30
> - Rama base: `main` — commit de referencia: `b614cfd`
> - Responsable de consolidación: equipo Artisync (3 integrantes)

---

## 0. Resumen ejecutivo

La guía asume un sistema que **este repositorio todavía no tiene del todo**. Antes de medir hay que
cerrar 7 brechas técnicas (§2). Medir sin cerrarlas produce evidencia que documenta fallos, no
cumplimiento — y en 3 casos (A01, A07, cobertura) directamente **no se puede generar el archivo**.

| Bloque | Carpeta destino | ¿Se puede medir hoy? | Bloqueo |
|---|---|---|---|
| C.1 Rendimiento | `docs/mediciones/perf/` | Parcial | k6 no instalado; sin datos semilla; sin caché Redis (no hay escenario frío/caliente) |
| C.2 Seguridad | `docs/mediciones/sec/` | Parcial | A01 falla (IDOR real); A02 sin TLS; A07 sin rate limit |
| C.3 Usabilidad (SUS) | `docs/mediciones/sus/` | Sí | Depende de personas — **empezar hoy** |
| C.4 Lighthouse | `docs/mediciones/lighthouse/` | Sí | Requiere `docker compose up` completo |
| C.5 Cobertura | `docs/mediciones/jacoco/` | No | Plugin JaCoCo ausente en `pom.xml`; falta línea base 1B |
| C.6 Diccionario | `docs/mediciones/DATA-DICTIONARY.md` | Al final | Depende de los 5 anteriores |

**Orden de ejecución recomendado:** C.3 (reclutamiento, hoy) → Fase 1 (arreglos de código) →
C.5 → C.1 → C.2 → C.4 → C.6.

---

## 1. Diferencias entre la guía y el repositorio real

Verificado leyendo el código el 2026-07-30. **Estas correcciones son obligatorias**: copiar los
comandos de la guía tal cual produce 404 en casi todos los casos.

| # | La guía dice | La realidad en este repo | Acción |
|---|---|---|---|
| 1 | `GET /api/servicios?page=0&size=20` | El catálogo paginado es `GET /api/v1/catalogo?page=0&size=20` ([CatalogoControlador.java:21](artisync/Backend/src/main/java/uteq/edu/ec/artisync/controller/catalogo/CatalogoControlador.java:21)). `/api/v1/servicios` existe pero sin listado paginado global | Usar `/api/v1/catalogo` en k6 |
| 2 | `POST /api/servicios/buscar` para inyección | No existe. La búsqueda por texto es `GET /api/v1/catalogo?q=...` vía `Specification` (JPA, parametrizado) | A03 se prueba sobre `?q=` |
| 3 | Espera `422` con `ProblemDetails` | [ManejadorGlobalExcepciones.java](artisync/Backend/src/main/java/uteq/edu/ec/artisync/exception/ManejadorGlobalExcepciones.java) devuelve `400` con DTO propio `RespuestaError`, no `ProblemDetail` RFC 9457 | Documentar `400` como resultado esperado, o migrar a `ProblemDetail` (§2.6) |
| 4 | `GET /api/pedidos/{id}` debe dar `403` a otro usuario | `obtenerPedidoPorId` **no valida propiedad** — cualquier autenticado lee cualquier pedido (`PedidoControlador` usa `@PreAuthorize("isAuthenticated()")` y el service solo hace `findById`) | **IDOR real. Corregir antes de medir** (§2.1) |
| 5 | `https://localhost:8443` para TLS | `server.port=8080`, sin `server.ssl.*` en `application.properties` | Añadir perfil `medicion` con TLS (§2.2) |
| 6 | Intento #6 de login → `429` | No hay rate limiting en ninguna parte del backend (grep de `429`/`bucket4j`/`RateLimit` = 0 resultados) | Implementar filtro con Redis (§2.3) |
| 7 | `docker compose logs api` | El servicio se llama `backend` (contenedor `pfc_backend`) | `docker compose logs backend` |
| 8 | `make up` | No existe `Makefile` | `docker compose up -d --build` desde `artisync/` |
| 9 | JaCoCo ya configurado | `pom.xml` **no tiene** `jacoco-maven-plugin` | Añadirlo (§2.5) |
| 10 | Cache caliente vs. fría en el diccionario | No hay `@EnableCaching` ni `@Cacheable` en el código; Redis solo se usa para sesiones/blacklist | Implementar caché de catálogo (§2.4) o justificar un solo escenario |
| 11 | Comandos bash (`for i in {1..6}`, `sleep 60`) | Entorno Windows/PowerShell | Comandos traducidos en cada sección de este plan |
| 12 | `mvn clean test` | No hay `mvn` en PATH; sí existe el wrapper `./mvnw` | Usar `.\mvnw.cmd` |

---

## 2. Fase 1 — Arreglos de código previos a medir

> Cada arreglo va en su propio commit con el código `OBS-NN` correspondiente de
> [OBSERVACIONES.md](docs/observaciones/OBSERVACIONES.md), para mantener la trazabilidad exigida.

### 2.1 A01 — Cerrar el IDOR en pedidos `[OBS-08]` — **crítico** — ✅ implementado y verificado

`PedidoServicioImpl.obtenerPedidoPorId` y `TicketRevisionServicioImpl.listarTicketsPorPedido`
ahora reciben el `idUsuario` autenticado y validan que sea el cliente del pedido, el creador del
servicio, o `ADMIN`; si no, lanzan `AccessDeniedException` → `403` (mapeado en
`ManejadorGlobalExcepciones`). `ChatControlador`/`ChatServiceImpl` se dejaron sin tocar a
petición explícita — sigue pendiente si se decide cerrarlo. `EntregableControlador` ya validaba
pertenencia antes de esta sesión.

**Verificado end-to-end contra datos reales** (no solo con mocks): con el token de `userA`,
`GET /api/v1/pedidos/1` (su propio pedido) → `200`; `GET /api/v1/pedidos/2` (pedido de `userB`)
→ `403`. Evidencia capturada en [`sec/a01-control-acceso.txt`](sec/a01-control-acceso.txt).

### 2.2 A02 — Habilitar TLS en un perfil de medición — ✅ implementado y verificado

Certificado autofirmado generado con `keytool` (`artisync-medicion.p12`, en `.gitignore`, nunca
commiteado). En vez de `server.ssl.*` (que reemplazaría el único conector y apagaría el 8080 que
usan el resto de las mediciones), se implementó
[`TlsMedicionConfig.java`](artisync/Backend/src/main/java/uteq/edu/ec/artisync/config/TlsMedicionConfig.java):
un `WebServerFactoryCustomizer<TomcatServletWebServerFactory>` activo solo bajo el perfil
`medicion` que **suma** un conector HTTPS en 8443 (`TLSv1.3`, cipher `TLS_AES_256_GCM_SHA384`)
sin tocar el 8080. Se activa vía override
[`docker-compose.medicion.yml`](../../artisync/docker-compose.medicion.yml)
(`SPRING_PROFILES_ACTIVE=medicion` + monta el `.p12`), solo durante la ventana de auditoría A02:

```bash
docker compose -f docker-compose.yml -f docker-compose.medicion.yml up -d --build backend
# ... correr la evidencia A02/A05 sobre :8443 ...
docker compose -f docker-compose.yml -f docker-compose.medicion.yml stop backend && docker compose up -d backend
```

**Verificado con `openssl s_client`:** `Protocol: TLSv1.3`, `Cipher: TLS_AES_256_GCM_SHA384`. Log
de arranque confirma `Tomcat started on ports 8080 (http), 8443 (https)`. **HSTS confirmado**
sobre `:8443` (`Strict-Transport-Security: max-age=31536000 ; includeSubDomains`) — no aparece
sobre HTTP plano por diseño de Spring Security, como se documentó originalmente.

### 2.3 A07 — Rate limiting en `/api/auth/login` — ✅ implementado y verificado

`LoginRateLimitFilter` (`OncePerRequestFilter`, registrado antes del filtro JWT) usa
`StringRedisTemplate.opsForValue().increment()` + `expire()` sobre `rl:login:{ip}`, ventana 60s,
límite 5 intentos, responde `429` con `Retry-After`. Fail-open si Redis no responde (a diferencia
de la blacklist JWT, que es fail-closed).

**Verificado con datos reales:** 6 intentos de login con contraseña incorrecta → intentos 1–5
devuelven `401`, intento **6 devuelve `429`**. Clave confirmada en Redis
(`redis-cli TTL rl:login:{ip}` ≈ 17s de los 60s tras varios intentos). Evidencia en
[`sec/a07-rate-limit.txt`](sec/a07-rate-limit.txt).

### 2.4 Caché de catálogo (habilita el escenario "frío vs caliente") — ✅ implementado

`@EnableCaching` + `RedisCacheManager` (cache `"catalogo"`, TTL configurable vía
`app.cache.catalogo.ttl-seconds`, default 60 s) en [RedisConfig.java](artisync/Backend/src/main/java/uteq/edu/ec/artisync/config/RedisConfig.java).
`@Cacheable(cacheNames = "catalogo")` sobre `buscarCatalogoServicios(...)` sin `key` explícita —
el `SimpleKeyGenerator` por defecto arma la clave con todos los parámetros del método
(categoría, subcategoría, precioMin/Max, etiquetas, `q`, sort, page, size), que es la "clave
compuesta por los filtros" que pide la guía. `@CacheEvict(allEntries = true)` en `crearServicio`,
`actualizarServicio` y `eliminarServicio` (consecuencia negativa ya anticipada en
`docs/adr/adr-004-estrategia-cache.md`). `RespuestaServicioResumido` y `RespuestaEtiqueta` se
marcaron `implements Serializable` (serialización JDK por defecto de Spring Data Redis; evita el
problema conocido de Jackson con `PageImpl`, que ya es `Serializable` de fábrica).

**Verificado end-to-end** contra el stack real (`docker compose`, no solo tests con mocks):
`redis-cli FLUSHALL` → 1ª llamada a `/api/v1/catalogo` crea la clave
`catalogo::SimpleKey [...]` con `TTL` ≈ 60 s → llamadas siguientes la reutilizan → `POST
/api/v1/servicios/creador/{id}` la vacía inmediatamente (evicción confirmada con `redis-cli KEYS
catalogo*` devolviendo vacío tras el alta).

**Matiz para `REPORTE-PERF.md`:** el script de k6 pega siempre a la misma URL
(`?page=0&size=20`), así que en una corrida "caliente" de 30s solo la primera de ~1500
peticiones toca Postgres; el resto mide Redis GET + deserialización. Aclarar esto explícitamente
para que el p95 bajo no se lea como manipulado.

### 2.5 JaCoCo en `pom.xml` `[OBS-09]` — ✅ implementado y verificado

Plugin 0.8.13 añadido a `pom.xml`. `./mvnw.cmd clean test` genera `target/site/jacoco/jacoco.xml`
e `index.html` sin conflicto con Lombok/Surefire. Copiado a `docs/mediciones/jacoco/`.
**Resultado real medido:** Lines 23.0%, Branches 13.8%, Complexity 16.8% — por debajo del umbral
de referencia (≥60%), reportado tal cual en `jacoco/REPORTE-JACOCO.md` sin ajustar el umbral. La
comparación numérica contra la Entrega 1B se descartó (no hay otra rama ni forma confiable de
reconstruir ese estado en este entorno); se documentó como referencia cualitativa únicamente.

### 2.6 (Opcional) Migrar errores a `ProblemDetail`

La guía espera cuerpos RFC 9457. Migrar `ManejadorGlobalExcepciones` a `ProblemDetail` mejora el
resultado de A03/A01, pero toca DTOs consumidos por el frontend. **Recomendación: no hacerlo
ahora**; documentar en `REPORTE-SEC.md` que el contrato de error es `RespuestaError` (formato
propio, estable y documentado en OpenAPI) y que el criterio real —no filtrar datos ni devolver
500— sí se cumple.

### 2.7 Datos semilla para la medición — ✅ implementado

Las migraciones Flyway no siembran **categorías, subcategorías, flujos de trabajo, etapas de
flujo ni su configuración** — sin eso, `crearPedido` revienta con "No hay flujos de trabajo
configurados" y ningún servicio puede crearse (`servicios.id_subcategoria` es `NOT NULL`). El
alcance real es mayor al que describe la guía. Se resolvió en dos piezas:

**A. [seed-medicion-referencia.sql](artisync/database/seed-medicion-referencia.sql)** — SQL puro
e idempotente (`ON CONFLICT DO NOTHING` / `WHERE NOT EXISTS`): 5 categorías, 10 subcategorías, 1
`Flujo Estándar de Medición` con 4 etapas (`Borrador Recibido` → `En Producción` → `En Revisión
del Cliente` → `Entrega Final`, última `es_etapa_final = TRUE`).

**B. Usuarios + perfil + servicios + pedidos** — híbrido API/SQL en vez de un único `.sql`, para
no reimplementar a mano en SQL reglas de negocio que ya existen en el código (hash BCrypt fuerza
12, validación de edad ≥18, patrón de contraseña, alta de `usuario_roles`, historial de estado
inicial del pedido):

1. Registro vía `POST /api/auth/registro` (perfil real, no reimplementado):
   `perf@test.com` (CLIENTE), `userA@test.com` (CLIENTE), `userB@test.com` (CLIENTE),
   `creador@test.com` (CREADOR — dueño de los 200 servicios; registrar con `rol: "CREADOR"`
   crea automáticamente su fila en `perfiles_creadores`). Contraseña común de prueba:
   `Medicion2026*`.
2. **[seed-medicion-servicios.sql](artisync/database/seed-medicion-servicios.sql)** — bloque
   `DO $$...$$` que resuelve el `id_perfil` de `creador@test.com` por correo y genera 200
   servicios `ACTIVO` en round-robin sobre las subcategorías de la parte A
   (`generate_series(1,200)`). No idempotente a propósito (pensado para volumen fresco).
3. Los 2 pedidos van por `POST /api/v1/pedidos` (no por SQL) — uno con el token de `userA` y
   otro con el de `userB`, contra dos servicios del creador — porque `crearPedido` también
   inserta el `historial_estados_pedido` inicial y replicarlo a mano en SQL no aporta nada.

**Verificado end-to-end:** 200 servicios visibles en `GET /api/v1/catalogo` (confirmado además
que los acentos se guardan correctamente en UTF-8 revisando los bytes en Postgres —
`python -m json.tool` en esta terminal Windows los mostraba mal solo como artefacto visual de
consola, no como error de datos). Pedido 1 = `userA`, pedido 2 = `userB`. Con esto ya se probó
en caliente el fix de A01 del §2.1: `userA` con su propio token obtiene `200` en
`GET /api/v1/pedidos/1` y `403` en `GET /api/v1/pedidos/2`.

Ejecución (orden importa):

```bash
docker compose exec -T postgres psql -U postgres -d artisyncbd < database/seed-medicion-referencia.sql

# Registrar los 4 usuarios (perf, userA, userB como CLIENTE; creador como CREADOR)
curl -s -X POST http://localhost:8080/api/auth/registro -H "Content-Type: application/json" \
  -d '{"nombres":"...","apellidos":"...","correo":"creador@test.com","contrasena":"Medicion2026*","fechaNacimiento":"1992-07-15","rol":"CREADOR","aceptaTerminos":true}'

docker compose exec -T postgres psql -U postgres -d artisyncbd < database/seed-medicion-servicios.sql

# 2 pedidos vía POST /api/v1/pedidos con los tokens de userA y userB
```

> **Nota de credenciales reales del `.env` de este proyecto:** el usuario/DB configurados son
> `postgres`/`artisyncbd`, no `pfc_user`/`pfc_db` como sugiere la guía genérica — usar los del
> `.env` local, no los de este documento.

---

## 3. Fase 0 — Preparación del entorno (Windows)

### 3.1 Instalar herramientas

```bash
winget install k6 --source winget
npm install -g @lhci/cli
```

Verificar y **anotar las versiones** (van en los encabezados de cada reporte):

```bash
k6 version; lhci --version; node --version; docker --version; python --version
```

### 3.2 Levantar el sistema

```bash
docker compose --project-directory artisync up -d --build
```

Esperar a que `pfc_backend` esté `healthy` (`docker compose ps`), luego cargar la semilla (§2.7).

### 3.3 Obtener tokens de prueba

```bash
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"correo\":\"perf@test.com\",\"contrasena\":\"Perf12345*\"}"
```

Guardar el `accessToken` en variables de entorno de la sesión (`$env:TOKEN`, `$env:TOKEN_USER_A`,
`$env:TOKEN_USER_B`). **Nunca** commitear tokens ni el `.env` real.

### 3.4 Congelar el commit de medición

Todas las mediciones deben salir del **mismo commit**. Anotarlo una vez y repetirlo en los 5
reportes:

```bash
git rev-parse --short HEAD
```

---

## 4. C.1 — Rendimiento (k6) → `docs/mediciones/perf/`

### 4.1 Script

Crear `k6/script.js` en la raíz del repo, idéntico al de la guía salvo la URL y la tolerancia de
umbral:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const token = __ENV.AUTH_TOKEN;
  const res = http.get(`${BASE_URL}/api/v1/catalogo?page=0&size=20`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(res, { 'status es 200': (r) => r.status === 200 });
  sleep(1);
}
```

> `vus: 50` y `duration: '30s'` **no se cambian entre corridas**: la comparabilidad de las 3
> corridas es lo que sostiene el IC 95%.

### 4.2 Ejecución (3 corridas, escenario caliente)

Antes de la corrida 1, un calentamiento que no se guarda (`k6 run --vus 5 --duration 10s`) para
llenar la caché y el pool JDBC.

```bash
mkdir -p docs/mediciones/perf
k6 run --out json=docs/mediciones/perf/k6-run1.json -e BASE_URL=http://localhost:8080 -e AUTH_TOKEN=$TOKEN k6/script.js
```

Esperar ~60 s entre corridas (en PowerShell: `Start-Sleep -Seconds 60`) y repetir para
`k6-run2.json` y `k6-run3.json`.

**Escenario frío** (si se implementó §2.4): `docker compose restart redis backend`, esperar
`healthy`, y correr sin calentamiento → `k6-cold-run1.json` … `k6-cold-run3.json`.

> Los `.json` de k6 son NDJSON y pesan decenas de MB con 50 VUs. Verificar `.gitignore` y, si el
> tamaño es un problema, comprimirlos (`.json.gz`) documentándolo en el reporte — pero **no
> recortarlos a mano**.

### 4.3 Análisis

Guardar el script de la guía tal cual en `docs/mediciones/perf/analisis-perf.py` (solo cambia el
separador de rutas, que `pathlib` ya resuelve) y ejecutarlo:

```bash
python docs/mediciones/perf/analisis-perf.py > docs/mediciones/perf/salida-analisis.txt
```

Ajuste necesario: en k6 ≥ 0.50 la métrica `http_req_failed` emite `value: 0/1` por punto — el
script ya lo contempla; verificar que `total` no salga 0 antes de dividir (si el catálogo está
vacío o el token expiró, `n=0` y el script revienta con `ZeroDivisionError`).

**Throughput**: no lo calcula el script; tomarlo de `http_reqs` (`count/rate`) del resumen que k6
imprime en consola — guardar esa consola en `docs/mediciones/perf/k6-console-run{1,2,3}.txt`.

### 4.4 Reporte

`docs/mediciones/perf/REPORTE-PERF.md` con el encabezado (fecha ISO con zona `-05:00`, commit,
versión de k6, configuración), la tabla de métricas de la guía, ambos escenarios (frío/caliente) y
el veredicto contra el umbral p95 < 200 ms. Si no cumple, justificar con la causa observada
(sin caché, N+1 en `Specification`, pool de conexiones) — un "no cumple" bien argumentado vale
más que un número maquillado.

**Artefactos:** `k6-run{1,2,3}.json`, `k6-console-run{1,2,3}.txt`, `analisis-perf.py`,
`salida-analisis.txt`, `REPORTE-PERF.md` (+ variantes `cold` si aplica).

---

## 5. C.2 — Seguridad OWASP → `docs/mediciones/sec/`

Ejecutar **después** de la Fase 1, con el sistema levantado. Cada comando deja su `.txt` crudo.

| Control | Comando (adaptado a este repo) | Archivo | Resultado esperado |
|---|---|---|---|
| A01 Control de acceso | `curl --include -H "Authorization: Bearer $TOKEN_USER_A" "$BASE/api/v1/pedidos/{id_pedido_de_B}"` | `a01-control-acceso.txt` | `403` tras §2.1 |
| A02 Fallas criptográficas | `curl -v --insecure "https://localhost:8443/actuator/health"` filtrando `SSL\|TLS\|cipher` | `a02-tls.txt` | `TLSv1.3`, `TLS_AES_256_GCM_SHA384` |
| A03 Inyección | `curl --include "$BASE/api/v1/catalogo?q=test%27%20OR%20%271%27%3D%271"` | `a03-inyeccion.txt` | `200` con `content: []` o `400`; **nunca** `500` ni filtración |
| A05 Config. incorrecta | `curl -I "$BASE/api/v1/catalogo"` (y sobre `https://localhost:8443` para ver HSTS) | `a05-cabeceras.txt` | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy`, `Referrer-Policy`, `Permissions-Policy`, `Strict-Transport-Security` |
| A07 Autenticación | Bucle de 6 logins con contraseña incorrecta | `a07-rate-limit.txt` | intento #6 → `429` |
| A09 Registro y monitoreo | `docker compose logs backend --since 5m` filtrando `login\|auth` | `a09-logging.txt` | entradas con ip, timestamp y `sub` en éxito y fallo |

Bucle A07 en PowerShell (equivalente al `for i in {1..6}` de la guía):

```bash
for i in 1 2 3 4 5 6; do echo "Intento $i:"; curl --include -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"correo":"test@test.com","contrasena":"incorrecta"}'; echo; echo "---"; done > docs/mediciones/sec/a07-rate-limit.txt
```

> **A09 requiere trabajo previo.** Hoy `AuthServiceImpl` solo tiene 3 llamadas a `log` (2 de error,
> 1 de debug): no registra login exitoso ni fallido con ip/sub. Añadir en `AuthServiceImpl.login`
> un `log.info` estructurado con `evento`, `correo`, `ip` (desde `HttpServletRequest`), `resultado`
> y `sub` (id de usuario). Sin esto, `a09-logging.txt` sale vacío.

**Reporte:** `docs/mediciones/sec/REPORTE-SEC.md` con una fila por control
(control · archivo · evidencia observada · cumple/no cumple · remediación aplicada y su commit).
Los controles A04, A06, A08, A10 se declaran fuera de alcance justificadamente.

---

## 6. C.3 — Usabilidad SUS → `docs/mediciones/sus/`

**Empezar hoy**: es el único bloque que depende de terceros y el que puede retrasar la entrega.

1. **Instrumento** — Cuestionario Brooke de 10 ítems, Likert 1–5, en español, **redacción sin
   modificar** e idéntica para todos. Google Forms con salida a CSV.
2. **Consentimiento** — Cada participante firma `docs/etica/consentimientos/plantilla.md`
   (crear la plantilla si no existe). Los PDF firmados **no se suben**; en el repo solo van los
   códigos `P01`…`P10`.
3. **Participantes** — mínimo 10, externos al equipo de desarrollo.
4. **Tarea estándar** (idéntica para todos, adaptada al flujo real de Artisync):
   registro/login → explorar catálogo → crear un pedido → seguir su estado → cerrar sesión.
5. **Registro** — `sus-raw.csv` con cabecera `participante,Q1..Q10`, una fila por participante,
   exportado directamente del formulario (sin edición manual).
6. **Cálculo** — `docs/mediciones/sus/analisis-sus.py` (script de la guía, sin cambios) →
   `salida-sus.txt`.
7. **Reporte** — `REPORTE-SUS.md` con media, DT, IC 95 %, interpretación Bangor
   (>68 promedio; ≥80.3 grado A) y ficha metodológica (fecha, tarea, perfil de participantes,
   entorno de prueba).

> Con n=10, el IC 95 % será ancho (±10 puntos es normal). No es un defecto: se reporta y se
> comenta, no se disimula.

---

## 7. C.4 — Lighthouse → `docs/mediciones/lighthouse/`

`lighthouserc.json` va en `artisync/Frontend/` (raíz del frontend), tal cual la guía: 1 corrida,
throttling `simulate`, form factor `mobile`, `outputDir` apuntando a `../../docs/mediciones/lighthouse`.

Medir **contra el contenedor** (nginx sirviendo el build de producción en el puerto 4200), nunca
contra `ng serve`: el dev server no minifica y hunde el score de performance.

```bash
lhci autorun --config=artisync/Frontend/lighthouserc.json
```

Renombrar el JSON generado a `lhci-YYYYMMDD-HHMM.json` y repetir en una segunda fecha/hora para
mostrar consistencia. `REPORTE-LIGHTHOUSE.md` con los 4 scores por corrida y el plan de
remediación de los fallos de accesibilidad (contraste, `alt`, labels, orden de headings son los
hallazgos típicos en Angular + Tailwind).

**Riesgo conocido:** el assert de accesibilidad está en `error` con `minScore: 0.9`; si el frontend
no llega, `lhci autorun` sale con código ≠ 0 **pero el JSON ya quedó escrito** — ese JSON es
evidencia válida. No bajar el umbral para "aprobar"; se reporta el score real.

---

## 8. C.5 — Cobertura JaCoCo → `docs/mediciones/jacoco/`

Tras aplicar §2.5:

```bash
cd artisync/Backend && ./mvnw.cmd -B clean test
```

Copiar los artefactos generados:

```bash
mkdir -p docs/mediciones/jacoco && cp artisync/Backend/target/site/jacoco/jacoco.xml docs/mediciones/jacoco/report.xml && cp -r artisync/Backend/target/site/jacoco docs/mediciones/jacoco/html
```

### Línea base de la Entrega 1B

La entrega 1B se revisó el 29-06-2026; el commit correspondiente es **`04307f6`** (último de esa
fecha; el código de esa entrega es `b3009b9`). Para obtener el número histórico sin contaminar
`main`, usar un worktree desechable:

```bash
git worktree add ../artisync-1b 04307f6
```

Añadir el plugin JaCoCo **solo en ese worktree**, correr `./mvnw.cmd clean test`, anotar los
porcentajes y eliminar el worktree (`git worktree remove ../artisync-1b`).

> **Decisión final (2026-07-30):** se descartó este paso a pedido explícito — el repositorio de
> trabajo hoy solo tiene la rama `entrega-3/mediciones-bloque-c` (sin otras ramas para comparar).
> Se intentó vía `git worktree` contra el commit histórico `04307f6` y se abortó antes de
> completarlo. `jacoco/REPORTE-JACOCO.md` documenta la cobertura de la Entrega 1B como referencia
> **cualitativa** (a partir de lo ya consignado en `OBS-09`: solo existía
> `ArtisyncApplicationTests`, cobertura efectiva ≈0%), no como número remedido.

`REPORTE-JACOCO.md` con la tabla `Métrica | Entrega 1B | Entrega 3 | Tendencia` para lines,
branches y complexity, más el desglose por paquete (los módulos `pedido`, `legal` y `catalogo` hoy
no tienen ningún test — decir cuáles son los huecos es parte del reporte).

---

## 9. C.6 — Diccionario de datos → `docs/mediciones/DATA-DICTIONARY.md`

Se redacta **al final**, cuando los 5 bloques tengan formato definitivo. Base mínima (tabla de la
guía) más las variables que este plan añade:

| Variable | Fuente | Umbral |
|---|---|---|
| `p95_latency_hot` / `p95_latency_cold` | `perf/k6-run{1,2,3}.json` | < 200 / < 500 ms |
| `error_rate_5xx`, `throughput_rps` | `perf/k6-console-run*.txt` | = 0 / — |
| `sus_score_mean`, `sus_ci95` | `sus/sus-raw.csv` | > 68 |
| `lh_performance`, `lh_accessibility`, `lh_best_practices`, `lh_seo` | `lighthouse/lhci-*.json` | ≥ 80 / ≥ 90 / ≥ 90 / ≥ 90 |
| `jacoco_lines_pct`, `jacoco_branches_pct`, `jacoco_complexity_pct` | `jacoco/report.xml` | ≥ 60 % |
| `owasp_a01_status` … `owasp_a09_status` | `sec/a0*.txt` | pass |

Cada fila lleva descripción, tipo, unidad, fuente y rango/umbral. Una fila por variable
**realmente producida** — si no se implementó la caché, `p95_latency_cold` no va.

---

## 10. Estructura final esperada

```
docs/mediciones/
├── PLAN-MEDICIONES.md            (este archivo)
├── DATA-DICTIONARY.md
├── perf/
│   ├── k6-run1.json  k6-run2.json  k6-run3.json
│   ├── k6-console-run1.txt … run3.txt
│   ├── analisis-perf.py  salida-analisis.txt
│   └── REPORTE-PERF.md
├── sec/
│   ├── a01-control-acceso.txt  a02-tls.txt  a03-inyeccion.txt
│   ├── a05-cabeceras.txt  a07-rate-limit.txt  a09-logging.txt
│   └── REPORTE-SEC.md
├── sus/
│   ├── sus-raw.csv  analisis-sus.py  salida-sus.txt
│   └── REPORTE-SUS.md
├── lighthouse/
│   ├── lhci-YYYYMMDD-HHMM.json (≥1, idealmente 2)
│   └── REPORTE-LIGHTHOUSE.md
└── jacoco/
    ├── report.xml  html/
    └── REPORTE-JACOCO.md
```

Fuera de `docs/mediciones/`: `k6/script.js`, `artisync/Frontend/lighthouserc.json`,
`artisync/database/seed-medicion.sql`, `docs/etica/consentimientos/plantilla.md`.

---

## 11. Reparto y secuencia sugerida

| Orden | Tarea | Depende de | Esfuerzo |
|---|---|---|---|
| 1 | Reclutar 10 participantes + montar formulario SUS | — | 30 min + días de espera |
| 2 | Fase 1: §2.1 A01, §2.3 A07, §2.5 JaCoCo, A09 logging | — | 1 día |
| 3 | §2.2 TLS + §2.4 caché + §2.7 semilla | 2 | medio día |
| 4 | C.5 JaCoCo (actual + línea base 1B) | 2 | 2 h |
| 5 | C.1 k6 (3+3 corridas + análisis) | 3 | 2 h (mucho es espera) |
| 6 | C.2 auditoría OWASP (6 archivos + reporte) | 3 | 2 h |
| 7 | C.4 Lighthouse (2 corridas) | 3 | 1 h |
| 8 | C.3 cálculo SUS + reporte | 1 | 1 h |
| 9 | C.6 diccionario + checklist final | 4–8 | 1 h |

---

## 12. Checklist de cierre del Bloque C

Estado al 2026-07-30, fin de la sesión de preparación de entorno (rama `entrega-3/mediciones-bloque-c`):

- [x] `perf/`: 3 JSON crudos (caliente) + 3 JSON crudos (frío) + consolas + script + análisis +
      reporte con fecha/commit/versión k6 — **corridas reales ejecutadas**, no smoke tests
- [x] `sec/`: 6 `.txt` (uno por control) + reporte consolidado con cumple/no cumple — **evidencia
      real capturada** (A01 403, A02 TLSv1.3, A03 sin filtración, A05 cabeceras, A07 429 en el
      intento 6, A09 con ip/sub en éxito y fallo)
- [ ] `sus/`: CSV con ≥10 filas + reporte con media/DT/IC95 % + códigos P01–P10 (sin PDFs) —
      **bloqueado**: depende de reclutar personas reales. Instrumento, consentimiento, script de
      análisis y guía paso a paso ya están listos (`sus/instrucciones-formulario.md`)
- [x] `lighthouse/`: 1 JSON generado contra el contenedor (no `ng serve`) — falta la 2ª corrida de
      consistencia (opcional, no bloqueante)
- [x] `jacoco/`: `report.xml` + `html/` generados y copiados — comparación con Entrega 1B
      **descartada** a pedido explícito (solo existe esta rama); reportada como referencia
      cualitativa únicamente
- [x] `DATA-DICTIONARY.md`: cubre las variables de las 5 carpetas; `sus_score_mean` queda
      marcada explícitamente como pendiente (no se inventó un valor)
- [x] Los reportes con datos reales citan el mismo commit (`f05feeb`, rama
      `entrega-3/mediciones-bloque-c`)
- [x] Ningún archivo crudo fue editado a mano después de generarse
- [x] Tokens, `.env`, certificado `.p12` (`Backend/.gitignore` actualizado) fuera del repositorio;
      no hay PDFs de consentimiento (aún no hay participantes)
- [ ] OBS-08, OBS-09 y OBS-10 actualizadas en `docs/observaciones/OBSERVACIONES.md` con el commit
      donde quedan resueltas — pendiente, no se tocó `OBSERVACIONES.md` en esta sesión
