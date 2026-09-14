# Evidencia en vivo: cabeceras de seguridad y cookie de refresh (P8)

**Fecha:** 2026-09-13/14 · **Objetivo:** cerrar el punto 8 de la rúbrica del
examen final ("Cabeceras de seguridad y cookie segura"), que quedó en *A
medias* porque no se pudo confirmar el atributo `Secure` de la cookie de
refresh.

## 1. Garantía a nivel de código

`AuthController.escribirCookie` (`artisync/Backend/src/main/java/uteq/edu/ec/artisync/controller/seguridad/AuthController.java`)
construye la cookie de refresh y la de `preAuth2fa` con:

```java
ResponseCookie.from(nombre, valor)
        .httpOnly(true)
        .secure(cookieSecure)   // @Value("${app.security.cookie-secure:true}")
        .path("/api/v1/auth")
        .maxAge(maxAgeSegundos)
        .sameSite("Strict")     // ADR-002
        .build();
```

- **`Secure`**: el valor por defecto de la propiedad es `true` (falla seguro).
  `application.properties`: `app.security.cookie-secure=${APP_COOKIE_SECURE:true}`.
  `render.yaml` (servicio `artisync-backend`) fija explícitamente
  `APP_COOKIE_SECURE=true`. Solo `.env`/`.env.example` (desarrollo local, HTTP)
  lo desactivan.
- **`SameSite=Strict`**: coincide con ADR-002. Es seguro porque tanto en
  Docker Compose (`nginx.conf`) como en Render (`nginx.render.conf.template`)
  el navegador solo habla con el origen del frontend — nginx reenvía
  `/api`, `/actuator` y `/ws` al backend por su hostname interno — así que la
  petición del navegador nunca es cross-site.
- **`HttpOnly`**, **`Path=/api/v1/auth`** y **`Max-Age`** (604800 s refresh,
  300 s preAuth2fa) están fijados sin condicionales.

## 2. Evidencia de tests

`AuthControllerTest` (`artisync/Backend/src/test/java/.../AuthControllerTest.java`)
verifica los cinco atributos sobre las cookies reales que produce el
controlador, tanto en `login`, `refresh` como `logout`, y confirma además que
`Secure` se desactiva correctamente cuando `cookieSecure=false` (caso de
desarrollo local). 1276/1276 tests del backend en verde tras el cambio
(`SameSite` pasó de `Lax` a `Strict`).

## 3. Cabeceras de seguridad en vivo (frontend, verificado)

```
$ curl -sI https://artisync-frontend.onrender.com
HTTP/1.1 200 OK
content-security-policy: default-src 'self'; script-src 'self'; script-src-attr 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'
permissions-policy: geolocation=(), microphone=(), camera=()
referrer-policy: strict-origin-when-cross-origin
strict-transport-security: max-age=31536000; includeSubDomains
x-content-type-options: nosniff
x-frame-options: DENY
```

Capturado el 2026-09-14 01:29 UTC. Coincide con lo que exige el punto 8 de la
rúbrica (HSTS, CSP, X-Frame-Options, X-Content-Type-Options).

## 4. Pendiente: `Set-Cookie` autenticado en vivo

Al intentar `POST /api/v1/auth/login` contra `artisync-frontend.onrender.com`
(que reenvía a `artisync-backend`) para capturar el `Set-Cookie` real, el
backend respondió:

```
HTTP/1.1 502 Bad Gateway
x-render-routing: no-deploy
```

Esto indica que el servicio `artisync-backend` en Render **no tiene un
despliegue activo** en este momento (no es un cold-start del plan gratuito,
que respondería con demora pero sin `no-deploy`). No es una regresión de este
cambio — el frontend estático sí responde con normalidad.

**Esta captura queda pendiente para la Fase 7 del plan** (redespliegue final),
momento en el que el backend deberá estar activo con la totalidad de los
cambios de las fases 2 a 6. Se actualizará este documento con la salida real
de:

```bash
curl -sD - -o /dev/null -X POST https://artisync-frontend.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@artisync.com","contrasena":"ArtisyncAdmin2026!"}'
```

(cuenta semilla documentada en `README.md` §"Credenciales de arranque",
pensada para este tipo de verificación).

## 5. Captura real (14-09-2026, backend activo) — SameSite todavía sin actualizar

El backend volvió a estar activo y respondió con el `Set-Cookie` real:

```
$ curl -sD - -o /dev/null -X POST https://artisync-frontend.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@artisync.com","contrasena":"ArtisyncAdmin2026!"}'
HTTP/1.1 200 OK
...
Set-Cookie: refreshToken=<jwt>; Path=/api/v1/auth; Max-Age=604800;
  Expires=Mon, 21 Sep 2026 06:24:14 GMT; Secure; HttpOnly; SameSite=Lax
```

Capturado 2026-09-14 06:24 UTC. Confirma `Secure`, `HttpOnly`, `Path` y
`Max-Age` en producción — pero `SameSite=Lax`, no `Strict`.

**Causa:** Render despliega `origin/main`, que sigue apuntando a `a92629cd`
— el mismo commit evaluado por el docente, anterior al cambio de la Fase 2
de este plan (`SameSite("Lax")` → `SameSite("Strict")` en
`AuthController.escribirCookie`). El trabajo de las fases 2 a 6 vive en la
rama local `examen-final-100`, que todavía no se ha empujado a `origin` ni
fusionado a `main`; por eso el backend en vivo no refleja el cambio.

**Pendiente, bloqueado en esta sesión:** empujar `examen-final-100` (o
fusionarla a `main`) y disparar el redespliegue en Render requiere acceso
autenticado a Render que esta sesión no tiene, además de ser una acción con
efecto en un sistema compartido (push público + despliegue de producción)
que corresponde confirmar explícitamente antes de ejecutarla. Una vez
autorizado y desplegado, repetir el `curl` de arriba y reemplazar esta
sección con `SameSite=Strict`.
