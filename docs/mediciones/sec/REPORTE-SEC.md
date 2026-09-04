# Reporte de Auditoría de Seguridad OWASP — Bloque C.2

- Fecha de la auditoría original: 2026-07-30 · **Recaptura de los 6 controles: 2026-09-04**
- Commit del sistema evaluado en la recaptura: `a0132222b1be7bb1a3b06861c1b592c86d623f31`
- Entorno: `docker compose up -d --build` (A02 requiere además el override
  `docker-compose.medicion.yml`, ver [`PLAN-MEDICIONES.md §2.2`](../PLAN-MEDICIONES.md)). Todas
  las peticiones salvo A02 se hicieron contra `http://localhost:4200/api/...` (proxy del
  frontend) — el puerto 8080 del backend ya no se publica al host desde OBS-AUTO-05.

| Control | Evidencia | Resultado observado | Cumple |
|---|---|---|---|
| A01 — Control de acceso roto | [`a01-control-acceso.txt`](a01-control-acceso.txt) | Usuario `CLIENTE` sin rol/permiso de administrador solicita `GET /api/v1/admin/auditoria` → `403 Forbidden` | ✅ Sí |
| A02 — Fallas criptográficas | [`a02-tls.txt`](a02-tls.txt) | Conector HTTPS adicional en 8443 (perfil `medicion`) negocia `TLSv1.3`, cipher `TLS_AES_256_GCM_SHA384` (AEAD); HSTS confirmado en la misma respuesta | ✅ Sí |
| A03 — Inyección | [`a03-inyeccion.txt`](a03-inyeccion.txt) | `GET /api/v1/catalogo?q=test' OR '1'='1` → `200` con `content: []` (consulta parametrizada vía JPA `Specification`); sin `500` ni filtración de datos | ✅ Sí |
| A05 — Configuración incorrecta de seguridad | [`a05-cabeceras.txt`](a05-cabeceras.txt) | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy`, `Referrer-Policy`, `Permissions-Policy` presentes sobre HTTP plano. `Strict-Transport-Security` **no aparece en este archivo** porque se capturó sobre el proxy `:4200` (HTTP) — Spring Security nunca la envía en texto plano; confirmada en la misma sesión sobre `:8443` (ver `a02-tls.txt`) | ✅ Sí (con la aclaración de HSTS) |
| A07 — Fallas de identificación y autenticación | [`a07-rate-limit.txt`](a07-rate-limit.txt) | 11 intentos de login con contraseña incorrecta: intentos 1–5 → `401`; intento **6 → `429`** (cuota por cuenta, `Retry-After: 900`); intentos 10–11 → `429` (cuota por IP, `Retry-After: 60`) | ✅ Sí |
| A09 — Fallas de registro y monitoreo | [`a09-logging.txt`](a09-logging.txt) | Logs con `evento=LOGIN`, `resultado=EXITOSO`/`FALLIDO`, `correo`, `ip` y `sub` (id de usuario, solo en éxito) para ambos casos | ✅ Sí |

## A01 — Broken Access Control

Commit auditado: `a0132222b1be7bb1a3b06861c1b592c86d623f31` · Fecha: 2026-09-04T21:26:09Z
```
$ curl -sS -i -X GET "http://localhost:4200/api/v1/admin/auditoria" \
    -H "Authorization: Bearer <token-de-usuario-CLIENTE-sin-rol-ADMIN>"
HTTP/1.1 403 Forbidden
{"detail":"No tienes permisos suficientes para realizar esta acción","instance":"/api/v1/admin/auditoria","status":403,"title":"Forbidden","type":"https://artisync.dev/errors/acceso-denegado"}
```
Detalle y nota sobre el cambio de escenario respecto a la captura original (julio 2026, control
de acceso horizontal sobre un pedido) en [`a01-control-acceso.txt`](a01-control-acceso.txt).

## A02 — Fallas criptográficas (TLS)

Commit auditado: `a0132222b1be7bb1a3b06861c1b592c86d623f31` · Fecha: 2026-09-04T21:28:26Z
```
$ curl -sS -i --insecure -X GET https://localhost:8443/api/v1/catalogo
HTTP/1.1 200
Strict-Transport-Security: max-age=31536000 ; includeSubDomains
...
$ echo | openssl s_client -connect localhost:8443 -tls1_3
New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
```
Detalle completo en [`a02-tls.txt`](a02-tls.txt).

## A03 — Inyección

Commit auditado: `a0132222b1be7bb1a3b06861c1b592c86d623f31` · Fecha: 2026-09-04T21:26:16Z
```
$ curl -sS -i -X GET "http://localhost:4200/api/v1/catalogo?q=test%27%20OR%20%271%27%3D%271"
HTTP/1.1 200 OK
{"content":[],"empty":true,...,"totalElements":0,"totalPages":0}
```
Detalle en [`a03-inyeccion.txt`](a03-inyeccion.txt).

## A05 — Configuración incorrecta de seguridad / Nota sobre HSTS

Commit auditado: `a0132222b1be7bb1a3b06861c1b592c86d623f31` · Fecha: 2026-09-04T21:26:21Z
```
$ curl -sS -i -X GET "http://localhost:4200/api/v1/catalogo"
HTTP/1.1 200 OK
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; ...
```
Sobre HTTP plano nunca aparece `Strict-Transport-Security`, porque Spring Security solo la
emite en respuestas servidas por HTTPS. Confirmada en la misma sesión de recaptura sobre el
conector TLS de 8443 (`Strict-Transport-Security: max-age=31536000 ; includeSubDomains`, ver
`a02-tls.txt`), confirmando que la configuración en `SecurityConfig.java` funciona
correctamente — solo no se ve en este `.txt` concreto porque ese archivo es deliberadamente el
del endpoint plano. Detalle completo en [`a05-cabeceras.txt`](a05-cabeceras.txt).

## A07 — Fallas de identificación y autenticación (rate limit)

Commit auditado: `a0132222b1be7bb1a3b06861c1b592c86d623f31` · Fecha: 2026-09-04T21:26:30Z a 21:26:34Z
```
$ for i in $(seq 1 11); do
    curl -sS -i -X POST "http://localhost:4200/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"correo":"usera-owasp@test.com","contrasena":"WrongPass999"}'
  done
Intento 1-5: HTTP/1.1 401 Unauthorized
Intento 6-9: HTTP/1.1 429 Too Many Requests (retry-after: 900 — cuota por cuenta)
Intento 10-11: HTTP/1.1 429 Too Many Requests (retry-after: 60 — cuota por IP)
```
Recaptura que **reemplaza la evidencia marcada OBSOLETA** (OBS-AUTO-05, agosto 2026). Confirma
que `AuthRateLimitFilter` + la cuota por cuenta (`IntentosAutenticacionService`) sí bloquean
intentos repetidos, con cuerpo `ProblemDetail` en UTF-8 (ya no el bug de encoding ISO-8859-1 de
la captura anterior). Detalle completo en [`a07-rate-limit.txt`](a07-rate-limit.txt).

## A09 — Fallas de registro y monitoreo

Commit auditado: `a0132222b1be7bb1a3b06861c1b592c86d623f31` · Fecha: 2026-09-04T21:28:45Z
```
$ curl -sS -X POST "http://localhost:4200/api/v1/auth/login" -H "Content-Type: application/json" \
    -d '{"correo":"userb-owasp@test.com","contrasena":"WrongPass999"}'
$ curl -sS -X POST "http://localhost:4200/api/v1/auth/login" -H "Content-Type: application/json" \
    -d '{"correo":"userb-owasp@test.com","contrasena":"UserBPass123"}'
$ docker logs pfc_backend 2>&1 | grep "evento=LOGIN"
... evento=LOGIN resultado=FALLIDO correo=userb-owasp@test.com ip=172.18.0.5
... evento=LOGIN resultado=EXITOSO correo=userb-owasp@test.com ip=172.18.0.5 sub=12
```
Detalle en [`a09-logging.txt`](a09-logging.txt).

## Remediaciones aplicadas en esta entrega (con referencia a OBSERVACIONES.md)

- **OBS-08 (A01):** `PedidoServicioImpl.obtenerPedidoPorId` y
  `TicketRevisionServicioImpl.listarTicketsPorPedido` ahora validan pertenencia
  (cliente/creador/ADMIN) antes de responder, lanzando `AccessDeniedException` → `403`.
- **OBS-08 (A07):** `LoginRateLimitFilter` (Redis `INCR`+`EXPIRE`, 5 intentos/60s por IP,
  fail-open si Redis no responde).
- **A02:** conector HTTPS adicional (`TlsMedicionConfig`, perfil `medicion`) sin apagar el
  puerto 8080 usado por el resto de las mediciones.
- **A09:** logging estructurado de login exitoso/fallido en `AuthServiceImpl.login`.

## Fuera de alcance en esta auditoría

A04 (diseño inseguro), A06 (componentes vulnerables/desactualizados), A08 (fallas de integridad
de software/datos) y A10 (SSRF) no se auditaron en este bloque — no había herramienta ni
escenario de prueba definido para ellos en la guía de la Tercera Entrega. Quedan como trabajo
futuro fuera del alcance del Bloque C.2.

## Escaneo automático OWASP ZAP baseline (Entrega Final, A.1)

- Fecha: 2026-08-17
- Comando: `make audit-zap` (equivalente ejecutado manualmente: `docker run --rm
  -v "$(pwd)/docs/mediciones/sec/zap:/zap/wrk/:rw" --network host
  ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t http://localhost:4200 -r
  zap-baseline-20260817-0300.html -J zap-baseline-20260817-0300.json`)
- Objetivo: `http://localhost:4200` (frontend — el backend en `:8080` no se publica al host
  por diseño, ver nota de A07 más abajo en este documento)
- Resultado: **`FAIL-NEW: 0` · `WARN-NEW: 8` · `PASS: 59`** — sin hallazgos altos/FAIL, cumple
  el umbral de la guía ("sin hallazgos altos").
- Reportes archivados: [`zap-baseline-20260817-0300.html`](zap/zap-baseline-20260817-0300.html) /
  [`.json`](zap/zap-baseline-20260817-0300.json)

Hallazgos WARN (todos de severidad media/baja, ninguno bloqueante):

| Regla ZAP | Hallazgo | Alcance observado |
|---|---|---|
| 10020 | Missing Anti-clickjacking Header | Respuestas estáticas servidas por nginx (`/`, `/sitemap.xml`) — no llevan `X-Frame-Options`/`frame-ancestors` |
| 10021 | X-Content-Type-Options Header Missing | Assets estáticos (`favicon.ico`, fuentes, `robots.txt`, CSS) |
| 10036 | Server Leaks Version Information via "Server" Header | nginx expone `Server: nginx/1.31.3` |
| 10038 | Content Security Policy (CSP) Header Not Set | Mismo alcance que 10020: solo en las respuestas de nginx, no en la API |
| 10049 | Storable and Cacheable Content | Assets con hash (`main-*.js`, fuentes) cacheados agresivamente — comportamiento intencional, ver `nginx.conf` |
| 10063 | Permissions Policy Header Not Set | Respuestas de nginx |
| 10109 | Modern Web Application (informativo) | Detecta que es una SPA — no es un hallazgo de seguridad |
| 90004 | Cross-Origin-Embedder-Policy Header Missing or Invalid | Respuestas de nginx |

**Nota importante de alcance**: las cabeceras de seguridad (`X-Content-Type-Options`,
`X-Frame-Options`, CSP, `Permissions-Policy`, etc.) que la evidencia A05 de este mismo documento
confirma presentes **sí existen en las respuestas de la API servidas por Spring Security**, pero
**no se replican en las respuestas estáticas que sirve nginx** (HTML/CSS/JS/fuentes del
frontend) — son dos servidores HTTP distintos (Spring Security vs. `nginx.conf`) y las cabeceras
de uno no se heredan al otro. Este escaneo evidencia esa brecha real y concreta: agregar las
mismas cabeceras en `nginx.conf` (bloque `location /` y `location ~* \.(js|css|...)`) cerraría
los 6 hallazgos WARN que no son informativos. Queda registrado como hallazgo pendiente, no
corregido en este cierre.

## Análisis estático de bytecode: SpotBugs + find-sec-bugs (Entrega Final, A.2.3)

- Fecha: 2026-08-17
- Comando: `make audit` (equivalente ejecutado manualmente dentro de un contenedor
  `maven:3.9-eclipse-temurin-21`, ver nota de plataforma abajo): `mvn -B spotbugs:spotbugs`
- Alcance: `artisync/Backend/pom.xml`, plugin `spotbugs-maven-plugin` 4.8.6.6 +
  `findsecbugs-plugin` 1.13.0, filtrado por `spotbugs-security-include.xml` a las reglas de
  inyección SQL (`SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE`, `SQL_INJECTION_JDBC`,
  `SQL_INJECTION_JPA`, `SQL_INJECTION_HIBERNATE`, `SQL_INJECTION_SPRING_JDBC`)
- Resultado: **0 hallazgos** — ningún patrón de concatenación SQL sospechosa detectado en el
  bytecode compilado de `artisync/Backend/src/main/java`.
- Reporte archivado: [`static-analysis/spotbugs-20260817-0250.xml`](static-analysis/spotbugs-20260817-0250.xml)
  (`<BugCollection>` sin instancias — `grep -c "<BugInstance" spotbugs-20260817-0250.xml` = 0)

**Nota de plataforma**: SpotBugs lee, además de las clases de la aplicación, las clases de
plataforma del JDK que ejecuta el proceso; su ASM embebido no soporta bytecode más nuevo que
Java 21 (falla con `Unsupported class file major version 69` si la máquina tiene un JDK más
nuevo por defecto, aunque el proyecto compile correctamente para `release 21`). Por eso el
target `audit` del `Makefile` corre `mvn spotbugs:spotbugs` dentro de un contenedor
`maven:3.9-eclipse-temurin-21` en vez de invocar `./mvnw` directo — reproducible sin importar
el JDK por defecto de la máquina del desarrollador.

Esto complementa, sin sustituir, la auditoría de texto de `scripts/audit-sql-dynamic.sh`
(`make audit`, sección superior de este documento): SpotBugs analiza bytecode con seguimiento
de flujo de datos entre métodos (detecta `StringBuilder`, JDBC `Statement` crudo, concatenación
multilínea) que un `grep` de una sola línea no puede ver.
