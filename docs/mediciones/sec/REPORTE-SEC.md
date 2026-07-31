# Reporte de Auditoría de Seguridad OWASP — Bloque C.2

- Fecha: 2026-07-30
- Commit del sistema evaluado: `entrega-3/mediciones-bloque-c` (base `f05feeb` + arreglos de esta sesión)
- Entorno: `docker compose up -d --build` (A02 requiere además el override
  `docker-compose.medicion.yml`, ver [`PLAN-MEDICIONES.md §2.2`](../PLAN-MEDICIONES.md))

| Control | Evidencia | Resultado observado | Cumple |
|---|---|---|---|
| A01 — Control de acceso roto | [`a01-control-acceso.txt`](a01-control-acceso.txt) | `userA` (token propio) solicita el pedido de `userB` (`GET /api/v1/pedidos/2`) → `403 Forbidden` | ✅ Sí |
| A02 — Fallas criptográficas | [`a02-tls.txt`](a02-tls.txt) | Conector HTTPS adicional en 8443 (perfil `medicion`) negocia `TLSv1.3`, cipher `TLS_AES_256_GCM_SHA384` (AEAD) | ✅ Sí |
| A03 — Inyección | [`a03-inyeccion.txt`](a03-inyeccion.txt) | `GET /api/v1/catalogo?q=test' OR '1'='1` → `200` con `content: []` (consulta parametrizada vía JPA `Specification`); sin `500` ni filtración de datos | ✅ Sí |
| A05 — Configuración incorrecta de seguridad | [`a05-cabeceras.txt`](a05-cabeceras.txt) | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy`, `Referrer-Policy`, `Permissions-Policy` presentes sobre HTTP plano. `Strict-Transport-Security` **no aparece en este archivo** porque se capturó sobre `:8080` (HTTP) — Spring Security nunca la envía en texto plano; confirmada por separado sobre `:8443` en la misma sesión (ver nota abajo) | ✅ Sí (con la aclaración de HSTS) |
| A07 — Fallas de identificación y autenticación | [`a07-rate-limit.txt`](a07-rate-limit.txt) | 6 intentos de login con contraseña incorrecta: intentos 1–5 → `401`, intento **6 → `429`** con `Retry-After` | ✅ Sí |
| A09 — Fallas de registro y monitoreo | [`a09-logging.txt`](a09-logging.txt) | Logs con `evento=LOGIN`, `resultado=EXITOSO`/`FALLIDO`, `correo`, `ip` y `sub` (id de usuario, solo en éxito) para ambos casos | ✅ Sí |

## Nota sobre A05 / HSTS

`a05-cabeceras.txt` se generó con `curl -s -D - -X GET http://localhost:8080/api/v1/catalogo`
(el endpoint es `permitAll` solo para `GET`; un `HEAD` con `curl -I` cae en `anyRequest().authenticated()`
y devuelve `401` — no confundir con una brecha real, es un detalle del método HTTP usado en la prueba).
Sobre HTTP plano nunca aparece `Strict-Transport-Security`, porque Spring Security solo la
emite en respuestas servidas por HTTPS. Se verificó en la misma sesión, sobre el conector TLS de
8443, que la cabecera sí se emite (`Strict-Transport-Security: max-age=31536000 ;
includeSubDomains`), confirmando que la configuración en `SecurityConfig.java` funciona
correctamente — solo no se ve en este `.txt` concreto porque ese archivo es deliberadamente el
del endpoint plano.

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
