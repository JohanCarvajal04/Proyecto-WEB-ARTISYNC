# Reporte de Análisis Externo SSL Labs — REQ-NF-001a/b/c

- Fecha: 2026-09-10 (zona horaria del entorno de prueba: UTC-05:00; timestamps crudos en UTC)
- Commit base: `932632d` (rama `feat/ia-verificacion-asistida`)
- Herramienta: [Qualys SSL Labs API v3](https://github.com/ssllabs/ssllabs-scan/blob/master/ssllabs-api-docs-v3.md) (`api.ssllabs.com`, `engineVersion: 2.4.3`, `criteriaVersion: 2009q`)
- Dominio analizado: `artisync-frontend.onrender.com` (único dominio público declarado en `render.yaml`)
- Dirección IP resuelta: `216.24.57.15`

## Nota sobre el punto de terminación TLS

La respuesta HTTP incluye `Server: cloudflare` — el dominio está detrás de un proxy/CDN de Cloudflare
delante del servicio de Render, no expone directamente el certificado del backend de Render. El análisis
mide el TLS tal como lo ve un cliente público real (que es lo que exige el criterio de aceptación del
requisito), pero el certificado/la configuración TLS observada es la de ese borde de Cloudflare, no una
configuración gestionada directamente por el equipo del proyecto en `application.properties`/`TlsMedicionConfig`
(esa configuración solo aplica al conector HTTPS local de medición, ver `docs/mediciones/sec/owasp/a02-tls.txt`).

## Comando ejecutado

```
$ curl -s "https://api.ssllabs.com/api/v3/analyze?host=artisync-frontend.onrender.com&all=done&publish=off&fromCache=off"
# poll cada 15s hasta status=READY (7 intentos, ~105s)
$ curl -s "https://api.ssllabs.com/api/v3/analyze?host=artisync-frontend.onrender.com&all=done"
```

Respuesta completa archivada en [`ssllabs-api-response-20260910.json`](ssllabs-api-response-20260910.json).

## REQ-NF-001a — Redirección forzada a HTTPS

```
$ curl -sS -i http://artisync-frontend.onrender.com
```

```
HTTP/1.1 301 Moved Permanently
Location: https://artisync-frontend.onrender.com/
Server: cloudflare
```

Transcripción completa en [`redirect-https-20260910.txt`](redirect-https-20260910.txt).

**Umbral esperado:** toda petición HTTP debe redirigir a HTTPS — **Resultado: cumple** (`301` con
`Location` a `https://`).

## REQ-NF-001b — Rechazo de TLS < 1.2

Del JSON de SSL Labs (`endpoints[0].details.protocols`), los únicos protocolos que el servidor acepta son:

| Protocolo | Versión |
|---|---|
| TLS | 1.2 |
| TLS | 1.3 |

SSL Labs prueba activamente SSLv2, SSLv3, TLS 1.0 y TLS 1.1 contra el servidor real y solo lista en este
array los protocolos que el servidor efectivamente acepta — ninguno de los protocolos obsoletos aparece
en la respuesta, es decir, el servidor los rechazó durante el análisis. `vulnBeast: false`,
`drownVulnerable: null` (no aplica, sin soporte SSLv2), `supportsRc4: false`.

**Umbral esperado:** SSLv2/SSLv3/TLS 1.0/TLS 1.1 rechazados — **Resultado: cumple**.

## REQ-NF-001c — Preferencia de TLS 1.3

La negociación de versión de protocolo en TLS es dirigida por el cliente (el cliente ofrece las
versiones que soporta en orden, el servidor elige la más alta que ambos comparten) — no existe un
concepto de "preferencia del servidor" independiente para la versión de protocolo como sí existe para
el orden de cifrados dentro de una misma versión. Se verifica entonces que, ofreciendo TLS 1.3, el
servidor lo negocia (en vez de degradar a 1.2 innecesariamente):

```
$ echo | openssl s_client -connect artisync-frontend.onrender.com:443 -servername artisync-frontend.onrender.com
New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
Protocol: TLSv1.3
```

Además, el JSON de SSL Labs confirma el grupo de cifrados TLS 1.3 (`protocol: 772`) publicado con los
3 conjuntos estándar (`TLS_AES_128_GCM_SHA256`, `TLS_AES_256_GCM_SHA384`, `TLS_CHACHA20_POLY1305_SHA256`),
y `forwardSecrecy: 4` (secreto perfecto hacia adelante con todos los navegadores simulados por SSL Labs).

**Nota de honestidad:** no se pudo verificar el rechazo explícito de TLS 1.1 con `openssl s_client` local
(`OpenSSL 3.5.6` no soporta ofrecer TLS 1.1 como cliente — `no protocols available` es un límite del
cliente local, no evidencia del servidor). La evidencia de rechazo de protocolos obsoletos para
REQ-NF-001b proviene exclusivamente del propio motor de sondeo de SSL Labs, que sí es capaz de ofrecerlos.

**Umbral esperado:** el servidor negocia TLS 1.3 cuando el cliente lo soporta — **Resultado: cumple**.

## Resultado global

**Grade SSL Labs: A+** (`gradeTrustIgnored: A+`). HSTS presente y con `includeSubDomains`
(`max-age=31536000`), consistente con la cabecera ya documentada en `a02-tls.txt`. Sin vulnerabilidades
conocidas detectadas (`heartbleed`, `poodle`, `freak`, `logjam`: todos `false`).

| Requisito | Resultado |
|---|---|
| REQ-NF-001a (redirección forzada a HTTPS) | Cumple |
| REQ-NF-001b (rechazo de TLS < 1.2) | Cumple |
| REQ-NF-001c (preferencia de TLS 1.3) | Cumple |
