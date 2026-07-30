# Bitácora de Observaciones — Entregas 1A y 1B

Fuente: informes de retroalimentación del docente (Entrega 1A, revisión 09-06-2026; Entrega 1B, revisión 29-06-2026). Estado de resolución verificado contra el código del repositorio en su estado actual (previo a la Tercera Entrega).

> **Pendiente del equipo:** confirmar/completar la columna "Commit" con el hash corto exacto que cada integrante considere definitivo para cada resolución (aquí se referencia el commit donde la evidencia técnica aparece por primera vez, según `git log`). Verificar también que cada mensaje de commit futuro que toque estos temas incluya el código `OBS-NN` correspondiente.

## Resumen

| Entrega | Nota obtenida | Observaciones registradas | Resueltas | Pendientes |
|---|---|---|---|---|
| 1A (09-06-2026) | 95.00/100 (9.5/10) | 5 | 1 | 4 (bajo riesgo) |
| 1B (29-06-2026) | 21.5/100 (2.2/10) | 7 | 2 | 5 (2 parciales) |
| **Total** | | **12** | **3 (25%)** | **9 (75%)** |

---

## Observaciones — Entrega 1A (Sumativa #5, nota 9.5/10)

| Código | Fuente | Criterio | Observación | Decisión | Commit |
|---|---|---|---|---|---|
| OBS-01 | Entrega 1A | C7 — Calidad/repositorio | "El repositorio indicado NO es accesible públicamente (404/requiere credenciales): no se pudo verificar estructura ni commits de los tres integrantes." | **Resuelta.** Se corrigió la visibilidad del repositorio; hoy es accesible en `github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC` (confirmado como VERIFICADO por el propio docente en el informe de Entrega 1B). | `0c7d3d2` (Initial commit) en adelante, historial visible |
| OBS-02 | Entrega 1A | C7 — Coherencia técnica | "La tabla de tecnologías dice Angular 19 pero el cronograma (S6) dice Angular 17." | **Pendiente de decisión explícita del equipo**: fijar una única versión de Angular en toda la documentación y en `package.json` del frontend antes del cierre de la Tercera Entrega. | _(pendiente)_ |
| OBS-03 | Entrega 1A | C7 — Coherencia técnica | "El ADR afirma '42 tablas' (el MER tiene ~40)." | Se recomienda actualizar el ADR-001 o el conteo real de tablas para que ambos documentos coincidan (el esquema actual, `V1__schema_inicial.sql`, debe ser la fuente de verdad). | _(pendiente — corregir en ADR-001 actualizado, ver `docs/adr/`)_ |
| OBS-04 | Entrega 1A | C7 — Coherencia técnica | "Cifras de carga del ADR (8.000/16.000 usuarios) sin fuente." | Añadir cita o nota aclaratoria de que son cifras ilustrativas de comparación entre frameworks, no benchmarks propios, en la próxima revisión del ADR-001. | _(pendiente)_ |
| OBS-05 | Entrega 1A | C5 — Wireframes | "La primera imagen 'Resumen Operativo' parece plantilla genérica; las demás sí son del dominio." | Aceptada como observación menor; reemplazar el wireframe genérico por uno del dominio Artisync si se actualiza la documentación de UI en el frontend. | _(pendiente — no bloqueante)_ |

**Resultado de este bloque:** OBS-01 resuelta (repo accesible, confirmado por el docente mismo en Entrega 1B). OBS-02, 03, 04, 05 quedan pendientes de bajo riesgo — no bloquean código, son de coherencia documental. Se recomienda cerrarlas junto con la actualización del ADR-001 en esta entrega.

---

## Observaciones — Entrega 1B (Módulo de Autenticación + Acceso a Datos, nota 2.2/10)

| Código | Fuente | Criterio | Observación | Decisión | Commit |
|---|---|---|---|---|---|
| OBS-06 | Entrega 1B | C2 — Autenticación JWT (Ausente, 0%) | "NO existe el módulo funcional de autenticación: no hay AuthController, JwtService, filtro JWT ni SecurityConfig. Las dependencias (jjwt, security) están en el pom.xml, pero no hay código que las use." | **Resuelta.** Se implementó `AuthController`, `SecurityConfig` y posteriormente `JwtService` completo con filtro de autenticación, 2FA (`TwoFactorController`), recuperación de contraseña y cabeceras de seguridad (CSP, X-Frame-Options, Referrer-Policy, Permissions-Policy). | `b3009b9` (feat(backend): creación de AuthController/SecurityConfig), `74ff606` (Actualización completa de seguridad — JwtService) |
| OBS-07 | Entrega 1B | C3 — CRUD + Spring Data JPA (Insuficiente, 5%) | "NO existe la capa CRUD: no hay controladores, servicios, repositorios ni DTOs. Solo están las clases @Entity y la aplicación principal." | **Resuelta.** Se implementaron 23 controladores, repositorios y servicios distribuidos en los módulos seguridad, perfil, catálogo, pedido y legal, con DTOs de petición/respuesta. | `98e80c2` (repository catálogo), `b98ef3a` (repository pedido), `9c3a31b` (repository legal), `2114fc6`/`6223e53` (service pedido), `fd31375`/`3e11aca` (service legal), `89f0906` (controller pedido), `b740a45` (controller legal) |
| OBS-08 | Entrega 1B | C4 — Seguridad OWASP (Ausente, 0%) | "No hay revocación en Redis, refresh ni controles OWASP implementados." | **Parcialmente resuelta.** Las cabeceras CSP/X-Frame-Options/Referrer-Policy/Permissions-Policy ya están configuradas en `SecurityConfig.java`. **Sigue pendiente**: cabecera HSTS (`Strict-Transport-Security`) no está configurada; no se confirmó blacklist de JTI en Redis para logout ni flujo de refresh token operativo end-to-end; controles A01/A03/A07/A09 de la auditoría OWASP de esta Tercera Entrega aún no tienen evidencia empírica generada. | `8ca8e7e` (Refactor de cabeceras de seguridad) — **falta commit para HSTS y verificación de refresh/blacklist** |
| OBS-09 | Entrega 1B | C5 — Pruebas JUnit 5 + Postman (Insuficiente, 3%) | "Solo el test de carga de contexto autogenerado; las 9 pruebas del informe figuran como [PASS/FAIL] sin ejecutar." | **Parcialmente resuelta.** Existen pruebas reales para el módulo de seguridad (Auth, User, Role, País, 2FA). **Pendiente**: ampliar cobertura a los módulos pedido/legal/catálogo y generar la colección Postman de 20+ peticiones exigida en esta Tercera Entrega. | `6042b6c` (refactor módulo de seguridad, incluye tests), `74ff606` |
| OBS-10 | Entrega 1B | C6 — Métricas de rendimiento P95 (Ausente, 0%) | "Sin métricas de rendimiento (P95 + speedup)." | **Pendiente.** No se encontró ninguna carpeta `docs/mediciones/perf/` ni script k6 en el repositorio. Debe generarse como parte del Bloque C.1 de esta Tercera Entrega. | _(pendiente)_ |
| OBS-11 | Entrega 1B | C7 — Informe técnico (Insuficiente, 2%) | "El informe en PDF es una plantilla con marcadores sin completar ([INSERTAR código], [INSERTAR captura], [X] ms...), sin resultados, métricas ni conclusiones reales." | **Pendiente.** Se debe redactar `docs/informe-entrega-3.pdf` con contenido real (20-30 páginas) para esta entrega, incluyendo las métricas generadas en OBS-10. | _(pendiente)_ |
| OBS-12 | Entrega 1B | C8 — Repositorio ejecutable + docker-compose (Regular, 4%) | Evidencia verificada: "modelo de datos extenso (~46 entidades JPA), migración Flyway V1 completa, docker-compose (Postgres, Redis, backend, frontend) y workflow CI." Sin observación de bloqueo explícita más allá de la nota "Regular". | **Parcialmente resuelta.** El `docker-compose.yml` existe y orquesta 4 servicios, pero **referencia un directorio `Frontend/` que no existe en el repositorio actual** — brecha nueva detectada en la revisión técnica de esta Tercera Entrega (ver OBS-AUTO-03 más abajo), que debe cerrarse para que `make up` funcione. | _(pendiente — bloquea criterio C2 de esta entrega si no se resuelve)_ |

---

## Observaciones detectadas en la revisión técnica previa a esta Tercera Entrega (no provienen del docente)

Estas se detectaron al auditar el estado actual del repositorio contra la guía de la Tercera Entrega. Se numeran aparte porque no tienen origen en un informe de retroalimentación formal, pero deben tratarse con la misma disciplina de trazabilidad.

| Código | Fuente | Criterio | Observación | Decisión | Commit |
|---|---|---|---|---|---|
| OBS-AUTO-01 | Revisión técnica previa a Entrega 3 | Auth JWT (A.1) | El JWT emitido por `JwtService.java` no incluye los claims `iss`, `aud` ni `nbf` exigidos por la guía de la Tercera Entrega (solo se confirmaron `iat`, `exp`, `type`, `email`). | _(pendiente de decisión)_ | |
| OBS-AUTO-02 | Revisión técnica previa a Entrega 3 | Acceso a datos (A.2.2) | No existe ningún procedimiento almacenado de negocio en el repositorio (solo el trigger `set_actualizado_en`); toda operación multi-tabla se resuelve vía JPA. Es la misma familia de brecha de acceso a datos observada en OBS-07, ahora con un requisito más estricto (obligatoriedad de SP para joins/agregaciones/reportes). | _(pendiente de decisión)_ | |
| OBS-AUTO-03 | Revisión técnica previa a Entrega 3 | Reproducibilidad (B.1) | `docker-compose.yml` referencia `./Frontend` con Dockerfile, pero el directorio no existe en el repositorio; `make up` fallaría desde clonación limpia. Relacionado con OBS-12. | _(pendiente de decisión)_ | |

---

## Resumen para el informe técnico (`docs/informe-entrega-3.pdf`)

- Total de observaciones registradas (docente + auto-detectadas): **15**
- Resueltas por completo: **2** (OBS-01, 06, 07 → 3 de 15, 20%)
- Parcialmente resueltas: **3** (OBS-08, 09, 12 — 20%)
- Pendientes: **9** (OBS-02, 03, 04, 05, 10, 11 + las 3 OBS-AUTO)
- Razón de las no resueltas: la Entrega 1B se calificó con 2.2/10 porque, a la fecha de esa revisión (29-06-2026), el módulo de autenticación y la capa CRUD aún no existían en el código; ambos ya fueron implementados posteriormente y se documentan aquí como resueltos con evidencia de commit. Las observaciones de coherencia documental (OBS-02 a 05) y las de evidencia empírica (OBS-10, 11) son las que definen el trabajo pendiente real para el 24 de julio.

## Etiquetado

Una vez cerradas OBS-01, OBS-06, OBS-07 (ya resueltas) y decididas OBS-02 a OBS-05 (coherencia documental, de bajo esfuerzo), se debe crear la etiqueta sobre el commit que consolide estas correcciones:

```
git tag v0.7.1 <commit-de-cierre>
git push origin v0.7.1
```

Esta etiqueta debe preceder al resto del trabajo de la Tercera Entrega (Bloques A–F), y las observaciones que sigan abiertas (OBS-08 parcial, OBS-10, OBS-11, OBS-12 parcial, y las 3 OBS-AUTO) pasan a ser el punto de partida del plan de cierre de esta entrega.
