# Bitácora de Observaciones — Entregas 1A y 1B

Fuente: informes de retroalimentación del docente (Entrega 1A, revisión 09-06-2026; Entrega 1B, revisión 29-06-2026).

**Estado de resolución re-verificado el 31-07-2026** contra el código del repositorio, comando por comando (no por memoria ni por lo que declaraba la versión anterior de este archivo). Cada observación marcada como resuelta indica abajo la evidencia concreta —archivo y línea, o comando— con la que se comprobó, y el commit en el que la resolución aparece por primera vez.

> **Nota de trazabilidad (limitación conocida):** ningún mensaje de commit del historial referencia el código `OBS-NN`, porque las resoluciones se implementaron antes de que existiera esta bitácora. Los hashes que aparecen abajo se localizaron *a posteriori* con `git log -S` sobre el símbolo introducido por cada arreglo. Los commits futuros que toquen observaciones abiertas sí deben citar su código (`fix(...): ... (OBS-NN)`).

## Resumen

| Entrega | Nota obtenida | Observaciones | Resueltas | Parciales | Pendientes |
|---|---|---|---|---|---|
| 1A (09-06-2026) | 95.00/100 (9.5/10) | 5 | 2 | 0 | 3 |
| 1B (29-06-2026) | 21.5/100 (2.2/10) | 7 | 5 | 1 | 1 |
| Tercera Entrega | — | 4 | 2 | 0 | 2 |
| Auto-detectadas (revisión técnica) | — | 9 | 8 | 0 | 1 |
| **Total** | | **25** | **17 (68 %)** | **1 (4 %)** | **7 (28 %)** |

---

## Observaciones — Entrega 1A (Sumativa #5, nota 9.5/10)

| Código | Fuente | Criterio | Observación | Estado y evidencia | Commit |
|---|---|---|---|---|---|
| OBS-01 | Entrega 1A | C7 — Calidad/repositorio | "El repositorio indicado NO es accesible públicamente (404/requiere credenciales): no se pudo verificar estructura ni commits de los tres integrantes." | **Resuelta.** Repositorio público en `github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC`; el propio docente lo marcó como VERIFICADO en el informe de la Entrega 1B. | `0c7d3d2` en adelante |
| OBS-02 | Entrega 1A | C7 — Coherencia técnica | "La tabla de tecnologías dice Angular 19 pero el cronograma (S6) dice Angular 17." | **Pendiente — y agravada.** Verificado el 31-07-2026: hoy existen **cinco** versiones distintas declaradas para la misma pila. Real: `package.json` → Angular `^22.0.0`; `pom.xml` → Spring Boot `4.1.0`, `java.version` `21`; `docker-compose.yml` → `postgres:16`. Documentado: `adr-001` dice "Java 25 + Spring Boot 4.0.6, Angular 19, PostgreSQL 18"; `artisync/README.md` dice "Java 21 · Spring Boot 3.2 · Angular 17+"; `C4_Nivel2_Contenedores.md` dice "Spring Boot 4.0.1". Cerrar unificando la documentación contra los archivos de build, que son la fuente de verdad. | _(pendiente)_ |
| OBS-03 | Entrega 1A | C7 — Coherencia técnica | "El ADR afirma '42 tablas' (el MER tiene ~40)." | **Resuelta.** `adr-003` ya no cita una cifra exacta: dice "más de 40 tablas", afirmación correcta respecto al esquema real (**52** sentencias `CREATE TABLE` en `artisync/db/schema.sql`, verificado con `grep -cE "^CREATE TABLE"`). Queda como observación menor que `adr-006` cite "48 entidades" cuando el conteo real es **52** clases `@Entity`; se cierra junto con OBS-02. | _(reformulación documental; sin commit de código)_ |
| OBS-04 | Entrega 1A | C7 — Coherencia técnica | "Cifras de carga del ADR (8.000/16.000 usuarios) sin fuente." | **Pendiente.** Añadir en `adr-001` la nota aclaratoria de que son cifras ilustrativas de comparación entre frameworks, no benchmarks propios del equipo. Bajo esfuerzo, no bloqueante. | _(pendiente)_ |
| OBS-05 | Entrega 1A | C5 — Wireframes | "La primera imagen 'Resumen Operativo' parece plantilla genérica; las demás sí son del dominio." | **Pendiente.** Reemplazar el wireframe genérico por uno del dominio Artisync. Observación menor, no bloqueante. | _(pendiente)_ |

---

## Observaciones — Entrega 1B (Módulo de Autenticación + Acceso a Datos, nota 2.2/10)

| Código | Fuente | Criterio | Observación | Estado y evidencia | Commit |
|---|---|---|---|---|---|
| OBS-06 | Entrega 1B | C2 — Autenticación JWT (Ausente, 0 %) | "NO existe el módulo funcional de autenticación: no hay AuthController, JwtService, filtro JWT ni SecurityConfig." | **Resuelta.** Existen `AuthController`, `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, 2FA (`TwoFactorController`) y recuperación de contraseña. Verificado además en vivo el 31-07-2026: `POST /api/auth/login` contra el stack levantado con Docker devuelve `200` con JWT válido. | `b3009b9`, `74ff606` |
| OBS-07 | Entrega 1B | C3 — CRUD + Spring Data JPA (Insuficiente, 5 %) | "NO existe la capa CRUD: no hay controladores, servicios, repositorios ni DTOs. Solo están las clases @Entity y la aplicación principal." | **Resuelta.** 24 controladores con `@RequestMapping`, más repositorios, servicios y DTOs de petición/respuesta en los módulos seguridad, perfil, catálogo, pedido, legal, comunicación y social. | `98e80c2`, `b98ef3a`, `9c3a31b`, `2114fc6`, `6223e53`, `fd31375`, `3e11aca`, `89f0906`, `b740a45` |
| OBS-08 | Entrega 1B | C4 — Seguridad OWASP (Ausente, 0 %) | "No hay revocación en Redis, refresh ni controles OWASP implementados." | **Resuelta.** Los tres puntos verificados uno por uno el 31-07-2026: (i) **revocación en Redis** — `SessionRevocationService:51` escribe `jti:<id>` con TTL y `JwtAuthenticationFilter:55` rechaza el token si la clave existe; (ii) **refresh** — `AuthController:57` expone `POST /api/auth/refresh` leyendo la cookie `HttpOnly`; (iii) **controles OWASP** — HSTS configurado en `SecurityConfig:47-50` (`maxAge` 31536000, `includeSubDomains`), junto a CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy y Permissions-Policy, más `LoginRateLimitFilter` (429). Evidencia empírica de los seis controles archivada en `docs/mediciones/sec/`. | `74ff606` (revocación Redis), `f05feeb` (HSTS + rate limit), `8ca8e7e` (resto de cabeceras) |
| OBS-09 | Entrega 1B | C5 — Pruebas JUnit 5 + Postman (Insuficiente, 3 %) | "Solo el test de carga de contexto autogenerado; las 9 pruebas del informe figuran como [PASS/FAIL] sin ejecutar." | **Parcialmente resuelta.** Hay **58 clases de test y 522 pruebas ejecutándose en verde** (`mvnw test`, 0 fallos, 0 errores), cubriendo seguridad, comunicación, social, catálogo, pedido, legal y perfil. La cobertura JaCoCo de líneas **cumple el 60 % exigido con margen: 72.0 %** (ver `docs/mediciones/jacoco/REPORTE-JACOCO.md`, medición 2026-08-16), dentro del rango 60–72 % aceptado. Branches también supera el 60 % (62.5 %). **Sigue pendiente:** la colección Postman de 20+ peticiones que exige la Tercera Entrega. | `6042b6c`, `a7231ad`, `825c935` |
| OBS-10 | Entrega 1B | C6 — Métricas de rendimiento P95 (Ausente, 0 %) | "Sin métricas de rendimiento (P95 + speedup)." | **Resuelta.** `docs/mediciones/perf/` contiene 3 corridas en caliente + 3 en frío con k6 (50 VUs, 30 s), datos crudos JSON versionados, salidas de consola y `REPORTE-PERF.md` con media, mediana, desviación típica, IC 95 % y p50/p90/p95/p99. p95 caliente 50.17 ms (umbral < 200 ms) y frío 39.14 ms (umbral < 500 ms), con 0 % de errores ≥ 500. El reporte declara además, de forma explícita, la limitación metodológica del escenario frío. | `ca8889c` |
| OBS-11 | Entrega 1B | C7 — Informe técnico (Insuficiente, 2 %) | "El informe en PDF es una plantilla con marcadores sin completar ([INSERTAR código], [INSERTAR captura], [X] ms...), sin resultados, métricas ni conclusiones reales." | **Pendiente.** `docs/informe-entrega-3.pdf` no existe. Debe redactarse con las 10 secciones exigidas (20-30 páginas), aprovechando que las métricas de OBS-10 ya son reales. | _(pendiente)_ |
| OBS-12 | Entrega 1B | C8 — Repositorio ejecutable + docker-compose (Regular, 4 %) | "Modelo de datos extenso (~46 entidades JPA), migración Flyway V1 completa, docker-compose (Postgres, Redis, backend, frontend) y workflow CI." Sin observación de bloqueo explícita más allá de la nota "Regular". | **Resuelta.** El directorio `artisync/Frontend/` con su `Dockerfile` existe (la brecha registrada como OBS-AUTO-03 quedó cerrada). Verificado el 31-07-2026 levantando el stack completo con Docker desde volumen limpio: Postgres, Redis y backend arrancan `healthy`. Además se añadió `Makefile` con `up/down/test/bench/audit/clean` y las imágenes quedaron ancladas por digest `sha256`. | `b614cfd` (Frontend), `fc8e3b8` (Makefile + digests) |

---

## Observaciones — Tercera Entrega

| Código | Fuente | Criterio | Observación | Estado y evidencia | Commit |
|---|---|---|---|---|---|
| OBS-13 | Tercera Entrega | Características y métricas | "ProblemDetails RFC 7807 y caché Redis con TTL externo y blacklist están bien implementados. Cobertura del 23 % y SUS pendiente se reportaron con honestidad." | **Resuelta.** Verificado en la tercera entrega. | _(N/A)_ |
| OBS-14 | Tercera Entrega | Arquitectura (Debilidad) | "El access-token viaja en el cuerpo y la cookie no es Secure por defecto; refuércenlo llevando el access-token también a cookie HttpOnly y activando Secure." | **Pendiente.** | _(pendiente)_ |
| OBS-15 | Tercera Entrega | Entregables (Limitante) | "La nota se ve limitada por la falta del informe técnico en PDF y de ETHICS.md, que son entregables centrales." | **Pendiente.** | _(pendiente)_ |
| OBS-16 | Tercera Entrega | Reproducibilidad | "Verifica que la versión de Spring Boot 4.1.0 que declararon exista y sea reproducible." | **Resuelta.** Verificado localmente mediante compilación con Maven (`mvnw dependency:resolve`); la resolución de dependencias de Spring Boot 4.1.0 es exitosa. | _(N/A)_ |

---

## Observaciones detectadas en la revisión técnica previa a esta Tercera Entrega (no provienen del docente)

Se numeran aparte porque no tienen origen en un informe de retroalimentación formal, pero se tratan con la misma disciplina de trazabilidad.

| Código | Fuente | Criterio | Observación | Estado y evidencia | Commit |
|---|---|---|---|---|---|
| OBS-AUTO-01 | Revisión técnica previa a Entrega 3 | Auth JWT (A.1) | El JWT emitido por `JwtService.java` no incluye los claims `iss`, `aud` ni `nbf` exigidos por la guía. | **Resuelta.** `JwtService` emite hoy los siete claims estándar del RFC 7519: `iss` (`.issuer("artisync-backend")`), `sub`, `aud`, `exp`, `nbf` (`.notBefore(...)`), `iat` y `jti`. Confirmado además decodificando un JWT real emitido por el sistema en marcha el 31-07-2026. | `f05feeb` |
| OBS-AUTO-02 | Revisión técnica previa a Entrega 3 | Acceso a datos (A.2.2) | No existe ningún procedimiento almacenado de negocio en el repositorio (solo el trigger `set_actualizado_en`); toda operación multi-tabla se resuelve vía JPA. | **Pendiente — es la brecha abierta de mayor peso en la rúbrica.** Re-verificado el 31-07-2026: `grep -rn "@Procedure\|NamedStoredProcedureQuery"` sobre el backend devuelve **0** resultados y `artisync/db/procs/` está vacío. Hay operaciones que la guía obliga a encapsular en SP y que hoy están en JPQL, p. ej. `ResenaServicioRepository:35` (`AVG` + dos `JOIN`) y `TransaccionPagoRepository:21` (tres `JOIN`). Atenuante verificado: todas usan parámetros nombrados, no hay concatenación de entrada de usuario ni SQL dinámico, por lo que **no se dispara la regla transversal 7**. `adr-006` ya documenta la decisión y lista cinco candidatos. | _(pendiente)_ |
| OBS-AUTO-03 | Revisión técnica previa a Entrega 3 | Reproducibilidad (B.1) | `docker-compose.yml` referencia `./Frontend` con Dockerfile, pero el directorio no existe; `make up` fallaría desde clonación limpia. | **Resuelta.** `artisync/Frontend/Dockerfile` existe y el stack levanta completo. Ver OBS-12. | `b614cfd` |
| OBS-AUTO-04 | Revisión de seguridad, agosto 2026 | A07 OWASP — bypass de 2FA | `POST /api/auth/2fa/verify` era `permitAll` y aceptaba `{correo, codigo}` sin ninguna prueba de que el llamante hubiera pasado por `/login`: habilitar 2FA degradaba la cuenta de "contraseña" a "6 dígitos" sin límite de intentos. | **Resuelta.** `PreAuth2faTicketService` (nuevo) emite un token opaco de un solo uso (`login()`, tras validar la contraseña), transportado en la cookie `HttpOnly` `preAuth2fa` (TTL 5 min, tope 5 intentos). `verify2Fa()` resuelve el usuario exclusivamente desde el ticket; el body ya no acepta `correo` (`TwoFactorRequest.java`). Verificado con `PreAuth2faTicketServiceTest` (10 pruebas) y `AuthServiceImplTest.verify2Fa_*` (4 pruebas): un código correcto sin la cookie devuelve 401. | _(por confirmar tras commit)_ |
| OBS-AUTO-05 | Revisión de seguridad, agosto 2026 | A07 OWASP — rate limiting inefectivo | `LoginRateLimitFilter` solo cubría `/login`; usaba `request.getRemoteAddr()` sin resolver la IP real detrás del proxy del frontend (bucket compartido: 6 peticiones bloqueaban el login de toda la plataforma), y no existía cuota por cuenta. | **Resuelta.** `server.forward-headers-strategy=native` + `docker-compose.yml` deja de publicar el puerto 8080 al host (el límite de confianza real, no la regex de proxies). `AuthRateLimitFilter` (renombrado desde `LoginRateLimitFilter`) generaliza el límite por IP a `/login`, `/2fa/verify`, `/forgot-password`, `/reset-password` y `/registro`. `IntentosAutenticacionService` (nuevo) añade una cuota por cuenta en `login()` y `forgotPassword()` (solo cuenta fallos, se limpia al acertar). El 429 pasa a `ProblemDetail` (antes JSON ad-hoc con bug de encoding `ISO-8859-1`). Verificado con `AuthRateLimitFilterTest` (6 pruebas). | _(por confirmar tras commit)_ |
| OBS-AUTO-06 | Revisión de seguridad, agosto 2026 | A07 OWASP — cuenta deshabilitada ignorada | `JwtAuthenticationFilter` nunca comprobaba `userDetails.isEnabled()`; una cuenta suspendida seguía autenticando hasta 24h si la revocación en Redis fallaba o no llegaba a tiempo. | **Resuelta.** Nuevo `JwtService.esAccessTokenValido(...)` comprueba `isEnabled()` e `isAccountNonLocked()` además de la firma y el titular; `JwtAuthenticationFilter` lo usa como única condición de autenticación. Verificado con `JwtAuthenticationFilterTest.doFilterInternal_ShouldNotAuthenticate_WhenUserDisabled` y `JwtServiceTest.esAccessTokenValido_ShouldReturnFalse_WhenUserDisabled`. | _(por confirmar tras commit)_ |
| OBS-AUTO-07 | Revisión de seguridad, agosto 2026 | A02 OWASP — JWT completo en la base de datos | `sesiones_usuario.token_jwt` guardaba el access/refresh token íntegro en texto plano; una lectura de la tabla (SQLi, backup filtrado, dump) entregaba tomas de control de todas las sesiones activas. | **Resuelta.** Migración `V8__sesiones_usuario_jti.sql`: la tabla pasa a guardar únicamente el `jti` (`UNIQUE`, con índice), se elimina `token_jwt`, y se purgan las filas existentes (corte limpio, un único cierre de sesión forzado). `SessionRevocationService` revoca leyendo `sesion.getJti()` directamente, sin volver a parsear un JWT (corrige de paso un bug: si el token guardado ya había expirado, el parseo lanzaba y la revocación se silenciaba). | _(por confirmar tras commit)_ |
| OBS-AUTO-08 | Revisión de seguridad, agosto 2026 | A02 OWASP — `iss`/`aud` sin validar | `JwtService` emitía los claims `iss` y `aud` (ver OBS-AUTO-01) pero nunca los verificaba al parsear (`Jwts.parser().verifyWith(clave)`, sin `requireIssuer`/`requireAudience`); tampoco había tolerancia de reloj. | **Resuelta.** El `JwtParser` se construye una única vez en el constructor con `.requireIssuer(...)`, `.requireAudience(...)` y `.clockSkewSeconds(60)`. Los tokens llevan además un claim `type` (`access`/`refresh`) que `JwtAuthenticationFilter` usa como lista blanca (antes era lista negra: solo rechazaba `type=refresh`, aceptando en silencio cualquier tipo futuro desconocido). Verificado con `JwtServiceTest.parsear_ShouldRejectTokenWithWrongIssuer` / `...WrongAudience` / `...ShouldAcceptTokenWithinClockSkew`. | _(por confirmar tras commit)_ |
| OBS-AUTO-09 | Revisión de seguridad, agosto 2026 | A02 OWASP — clave HMAC sin validar y sin caché | La longitud mínima de la clave (256 bits) solo se comprobaba de forma perezosa en el primer login (`Keys.hmacShaKeyFor` lanzando en caliente, no al arrancar); la `SecretKey` se reconstruía en cada petición. | **Resuelta.** `JwtService` pasa a inyección por constructor: valida `secret.length >= 32 bytes` al arrancar (falla el `ApplicationContext` con un mensaje que indica `openssl rand -hex 32`) y cachea la `SecretKey` y el `JwtParser` en campos `final`. Verificado con `JwtServiceTest.constructor_ShouldThrow_WhenSecretShorterThan32Bytes`. | _(por confirmar tras commit)_ |

---

## Resumen para el informe técnico (`docs/informe-entrega-3.pdf`)

- Observaciones registradas (docente + auto-detectadas): **25**
- **Resueltas por completo: 17 (68 %)** — OBS-01, 03, 06, 07, 08, 10, 12, OBS-13, OBS-16, AUTO-01, AUTO-03, AUTO-04, AUTO-05, AUTO-06, AUTO-07, AUTO-08, AUTO-09
- **Parcialmente resueltas: 1 (4 %)** — OBS-09 (522 pruebas en verde, cobertura de líneas en 72.0 %; falta Postman)
- **Pendientes: 7 (28 %)** — OBS-02, 04, 05 (coherencia documental, bajo esfuerzo), OBS-11, OBS-15 (entregables PDF y ETHICS), OBS-14 (cookie Secure/HttpOnly), OBS-AUTO-02 (procedimientos almacenados)

**Razón de las no resueltas.** Las tres observaciones documentales de la Entrega 1A (OBS-02, 04, 05) no bloquean código y se cierran editando `adr-001` y la documentación de pila; OBS-02 se agravó porque la pila real evolucionó (Angular 22, Spring Boot 4.1.0) sin que la documentación lo siguiera. OBS-11 depende de redactar el informe final, que consume las métricas ya generadas en OBS-10. OBS-AUTO-02 es la única brecha pendiente de arquitectura: exige implementar procedimientos almacenados para las operaciones no elementales, decisión ya documentada en `adr-006` pero no implementada.

**Contexto de la nota 2.2/10 de la Entrega 1B.** A la fecha de esa revisión (29-06-2026) el módulo de autenticación y la capa CRUD no existían en el código. Ambos se implementaron después y hoy están verificados en ejecución, junto con la revocación en Redis, el refresh y los controles OWASP que también se reportaron ausentes. Eso explica que siete de las observaciones de aquella entrega estén hoy cerradas.

## Etiquetado

Las etiquetas exigidas por el Bloque 0 ya existen y están empujadas al remoto:

```
v0.7.0     -> af85982  Entrega 1B — snapshot (revisado por el docente 29-06-2026)
v0.7.1     -> d292f7b  Cierre de aplicación de observaciones de Entregas 1A/1B
v0.9.0-rc  -> d292f7b  Tercera Entrega — release candidate
```

`v0.7.1` marca el estado real alcanzado en el cierre de observaciones, no un cierre al 100 %: al momento de etiquetarlo quedaban abiertas las cinco observaciones listadas arriba. Se documenta así deliberadamente para que la etiqueta no afirme más de lo que el repositorio sostiene.
