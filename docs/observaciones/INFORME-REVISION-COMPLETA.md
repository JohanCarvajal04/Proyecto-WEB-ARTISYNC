# Informe de Revisión Completa — ArtiSync

**Fecha:** 2026-08-26
**Alcance:** Backend (`artisync/Backend`, Spring Boot 4.1.0 / Java 21) y Frontend (`artisync/Frontend`, Angular 22 standalone + signals + zoneless).
**Rama evaluada:** `main` (HEAD `66697eb`), incluyendo el working tree con cambios pendientes de commit (hardening H-05, scheduler de purga de notificaciones H-08, `ValidadorPertenenciaPedido`, migración `V21`, tests nuevos).
**Herramientas utilizadas:**

| Herramienta solicitada | Cómo se ejecutó | Motivo |
|---|---|---|
| `/code-review` | Agente `code-reviewer` (backend y frontend por separado) | La skill `/code-review` cargada asume un Pull Request real de GitHub (`gh pr view`, comentarios vía `gh`); no existe un PR para esta revisión y el plan aprobado excluye publicar en GitHub. Se sustituyó por una revisión equivalente de correctness/simplificación/eficiencia. |
| `claude-security:claude-security` | Agente `security-reviewer` (OWASP Top 10, backend + frontend) | El orquestador `claude-security` requiere la herramienta `Workflow`, disponible solo cuando es el agente principal de una sesión — no puede lanzarse como subagente delegado. Se sustituyó por un escaneo de seguridad equivalente, en modo solo-reporte (sin parches). |
| `angular-developer` | Skill cargada + auditoría manual sobre `artisync/Frontend` | Ejecutada según lo solicitado. |
| `springboot-security` | Skill cargada + auditoría manual sobre `artisync/Backend` | Ejecutada según lo solicitado. |

**Nota de verificación:** dos hallazgos iniciales del análisis `springboot-security` (actuator abierto sin autenticar, CSP con `unsafe-inline`/`unsafe-eval`) fueron **contrastados directamente contra el código actual y descartados como obsoletos/falsos positivos** — el hardening H-05 de la sesión anterior ya está aplicado y verificado (ver sección de correcciones). Todos los demás hallazgos de este informe fueron confirmados con lectura directa del archivo referenciado antes de incluirse.

---

## Resumen ejecutivo

| Severidad | Cantidad |
|---|---|
| 🔴 Crítico | ~~1~~ 0 |
| 🟠 Alto | ~~4~~ 2 |
| 🟡 Medio | ~~6~~ 4 |
| 🟢 Bajo | ~~5~~ 4 |
| **Total abierto** | **10** |

**Actualización 2026-08-26 (post-informe):** los tres hallazgos bloqueantes para
despliegue — 1.1 (crítico, pérdida de datos transaccional) y 3.1/3.2 (IDOR) — ya
están corregidos y verificados; ver el estado marcado en cada hallazgo y en la
sección 7. Los recuentos de esta tabla reflejan el estado abierto actual, no el
momento de la revisión original.

**Actualización 2026-08-26 (2ª tanda):** también se corrigieron 1.2 (consulta
redundante), 1.3 (export de usuarios sin filtros — de paso se corrigió que el
filtrado de pantalla solo operaba sobre la página cargada, no el listado
completo) y 1.4 (export de comisiones sin filtros). De paso se cerró también
1.5 (duplicación de `aParams`), al necesitar un cuarto uso del mismo patrón
para 1.3. Quedan abiertos como 🟠 Alto solo 5.1 y 5.2 (frontend).

| Por herramienta | Crítico | Alto | Medio | Bajo |
|---|---|---|---|---|
| `/code-review` backend | 1 | 0 | 1 | 0 |
| `/code-review` frontend | 0 | 1 | 1 | 1 |
| Seguridad (OWASP) | 0 | 1 | 0 | 0 |
| `springboot-security` | 0 | 1 | 0 | 1 |
| `angular-developer` | 0 | 2 | 4 | 3 |

*(El hallazgo de IDOR en historial/seguimiento fue detectado tanto por el escaneo de seguridad general como por `springboot-security`; se cuenta una sola vez en el total, atribuido a `springboot-security` por ser más específico.)*

---

## 1. Hallazgos — `/code-review` (correctness, reutilización, eficiencia)

### 1.1 ✅ CORREGIDO (2026-08-26) — Pérdida silenciosa de respuestas de briefing por `UnexpectedRollbackException`
**Archivos:** [BriefingServiceImpl.java:207-217](artisync/Backend/src/main/java/uteq/edu/ec/artisync/service/comunicacion/impl/BriefingServiceImpl.java#L207-L217), [ContratoServicioImpl.java:44-45](artisync/Backend/src/main/java/uteq/edu/ec/artisync/service/legal/impl/ContratoServicioImpl.java#L44-L45)

**Solución aplicada:** `ContratoServicioImpl.generarContrato` pasó a
`@Transactional(propagation = Propagation.REQUIRES_NEW)`, aislando su commit del de
`responderBriefing` (mismo patrón ya usado por `AuditoriaServicioImpl.registrar`).
Cubierto por el nuevo test `BriefingContratoTransaccionTest`
(`service/comunicacion/impl/BriefingContratoTransaccionTest.java`), un
`@SpringBootTest` que ejerce los proxies transaccionales reales sobre H2: siembra un
pedido con contrato ya existente, llama a `responderBriefing` y confirma que las
respuestas y `completado=true` quedan persistidos sin `UnexpectedRollbackException`.
Se verificó explícitamente que el test falla si se revierte la propagación a
`@Transactional` (REQUIRED) por defecto.

`responderBriefing` es `@Transactional` (propagación `REQUIRED` por defecto) y, tras guardar las respuestas del cliente, llama a `contratoServicio.generarContrato(...)`, que también es `@Transactional(REQUIRED)` y se une a la **misma transacción física**. Cuando ya existe un contrato para el pedido (caso normal, contemplado explícitamente en el código), `generarContrato` lanza `ExcepcionReglaNegocio`, que el interceptor AOP de esa transacción marca como `rollbackOnly` **antes** de que `BriefingServiceImpl` pueda capturarla. El `try/catch` de `BriefingServiceImpl` evita que la excepción se propague, pero no puede revertir la marca de rollback: al confirmar la transacción externa, Spring lanza `UnexpectedRollbackException` y se pierden tanto las respuestas del briefing recién guardadas como el flag `completado=true`.

**Escenario de fallo:** un cliente responde el briefing de un pedido que ya tiene contrato generado manualmente → HTTP 500 y sus respuestas nunca quedan guardadas, aunque el mensaje de éxito estaba "planeado" en el flujo.

**Los tests con mocks no lo detectan:** `BriefingServiceImplTest.java` mockea `IContratoServicio` con Mockito, por lo que nunca pasa por el proxy transaccional real de Spring y por sí solo daba una falsa sensación de cobertura. Ahora existe además `BriefingContratoTransaccionTest`, un `@SpringBootTest` que sí ejerce los proxies reales y habría detectado la regresión.

**Recomendación (aplicada):** `Propagation.REQUIRES_NEW` en `generarContrato`.

### 1.2 ✅ CORREGIDO (2026-08-26) — Doble consulta a base de datos en `mapToRespuesta`
**Archivo:** [PedidoServicioImpl.java:420-437](artisync/Backend/src/main/java/uteq/edu/ec/artisync/service/pedido/impl/PedidoServicioImpl.java#L420-L437)

**Solución aplicada:** `mapToRespuesta` deriva `etapaActual` del último elemento de
`historial` (ya cargado y ordenado ASC) en vez de repetir la consulta vía
`obtenerEtapaActual`. `obtenerEtapaActual` se mantiene para `mapToResumido`, que no
tiene la misma redundancia. Cubierto por `PedidoServicioImplTest`
(`obtenerPedidoPorId_etapaActualDelHistorial`,
`obtenerPedidoPorId_sinHistorial_etapaActualSinEstado`).

`mapToRespuesta` obtiene el historial completo ordenado y luego llama por separado a `obtenerEtapaActual(idPedido)`, que ejecuta una segunda consulta (`findTopByPedidoIdPedidoOrderByFechaTransicionDesc`) para obtener la última transición — dato ya disponible en la lista `historial` recién cargada (`historial.get(historial.size()-1)`). Cada llamada a `crearPedido`, `actualizarTerminos`, `obtenerPedidoPorId` y `avanzarEtapa` paga esta consulta redundante.

**Recomendación (aplicada):** derivar la etapa actual directamente de la lista `historial` ya cargada en memoria, sin la segunda consulta.

### 1.3 ✅ CORREGIDO (2026-08-26) — Exportación de usuarios ignora los filtros activos
**Archivo:** [users.component.ts:59-72](artisync/Frontend/src/app/features/administracion/pages/users/users.component.ts#L59-L72) vs. `filteredUsers()` ([:98-120](artisync/Frontend/src/app/features/administracion/pages/users/users.component.ts#L98-L120))

**Solución aplicada:** al investigar se confirmó que el problema era más profundo —
`AdminUserController.getAllUsers` no aceptaba ningún filtro, así que `filteredUsers()`
solo filtraba en memoria la página cargada (10 usuarios), no el listado completo. Se
implementó filtrado real en backend: `FiltroUsuario` (búsqueda/rol/estado),
`UsuarioSpecification` (Criteria API con subquery de rol sobre `usuario_roles`, mismo
patrón que `EventoAuditoriaSpecification`), aplicado tanto a `getAllUsers` como a
`exportar`. El frontend reemplazó el `computed filteredUsers` por peticiones reales al
backend (debounce en la búsqueda, inmediato en los `<select>`). Cubierto por
`UsuarioSpecificationTest` (backend, contra H2 real) y los tests actualizados de
`AdminUserServiceImplTest`/`AdminUserControllerTest`.

La tabla de usuarios se filtra en pantalla por búsqueda, rol y estado, pero el botón "Exportar" llama a `adminUserService.exportar(formato)` sin pasar ninguno de esos filtros — el archivo exportado siempre contiene **todos** los usuarios, no el subconjunto visible. Un administrador que filtra por "ADMIN + Suspendido" para un reporte de auditoría recibe en cambio la base completa de usuarios, sin ninguna advertencia visual.

Por contraste, `reportes-contratos.component.ts` y `reportes-finanzas.component.ts` sí pasan correctamente el filtro activo a su exportación — la inconsistencia era específica de `users.component.ts` (y de `comisiones.component.ts`, ver 1.4).

**Recomendación (aplicada):** filtrado real en el backend, pasado tanto al listado como a la exportación.

### 1.4 ✅ CORREGIDO (2026-08-26) — Exportación de comisiones ignora los filtros activos
**Archivo:** [comisiones.component.ts:77-89](artisync/Frontend/src/app/features/creador/pages/comisiones/comisiones.component.ts#L77-L89) vs. `comisionesFiltradas()` ([:42-63](artisync/Frontend/src/app/features/creador/pages/comisiones/comisiones.component.ts#L42-L63))

**Solución aplicada:** a diferencia de 1.3, aquí `listarMisComisiones()` ya trae el
dataset completo del creador y `comisionesFiltradas()` ya filtra bien en pantalla — en
vez de portar a Java la heurística de texto `esEtapaActiva` (frágil, basada en
substrings del nombre de la etapa), `exportar()` envía los `idPedido` ya filtrados en
pantalla (`comisionesFiltradas().map(c => c.idPedido)`) y `exportarMisComisiones`
filtra su propio listado por esos ids antes de generar el reporte — garantiza "se
exporta lo que se ve" sin duplicar lógica de negocio, y sin abrir una vía de IDOR (la
lista base ya está acotada al creador autenticado). Cubierto por
`PedidoServicioImplTest` (`exportarMisComisiones_sinIds_exportaTodas`,
`exportarMisComisiones_conIds_exportaSoloEsosPedidos`,
`exportarMisComisiones_idAjeno_noAparece`).

Mismo patrón que 1.3: `pedidoService.exportarMisComisiones(formato)` no recibe los filtros de `activos/cerrados/vencidos`, etapa ni búsqueda aplicados en pantalla. Severidad menor porque son datos del propio creador (sin fuga entre cuentas), pero seguía siendo una discrepancia entre lo que se ve y lo que se exporta.

### 1.5 ✅ CORREGIDO (2026-08-26, bonus) — Lógica de parámetros de filtro duplicada en 3 servicios
**Archivos:** [auditoria.service.ts:51-59](artisync/Frontend/src/app/features/administracion/services/auditoria.service.ts#L51-L59), [reporte-contrato.service.ts:30-38](artisync/Frontend/src/app/features/administracion/services/reporte-contrato.service.ts#L30-L38), [reporte-financiero.service.ts:34-42](artisync/Frontend/src/app/features/administracion/services/reporte-financiero.service.ts#L34-L42)

**Solución aplicada:** al necesitar un cuarto `aParams` idéntico para `AdminUserService`
(1.3), se extrajo `shared/utils/params-desde-filtro.ts` (mismo patrón que
`descarga-archivo.ts`) y se reutiliza en los cuatro servicios.

Los tres servicios definían un método privado `aParams(filtro)` idéntico para construir `HttpParams` filtrando `undefined`/`null`/`''`. Riesgo de que una corrección se aplicara en una copia y no en las otras.

**Recomendación (aplicada):** extraer un util compartido (`shared/utils/params-desde-filtro.ts`), siguiendo el mismo patrón ya usado para `descarga-archivo.ts`.

**Nota positiva:** el componente `boton-exportar` y las utilidades de `descarga-archivo.ts` son una consolidación genuina y bien documentada de lógica de descarga que antes estaba triplicada.

---

## 2. Hallazgos — Seguridad general (OWASP, sustituto de `claude-security`)

### 2.1 ✅ CORREGIDO (2026-08-26) — IDOR en historial y seguimiento de pedidos
Ver detalle completo y solución aplicada en la sección 3.1 (`springboot-security`) — confirmado también por este análisis independiente.

### 2.2 Verificado sin hallazgos abiertos
El escaneo confirmó que lo siguiente está correctamente implementado en el código actual (no se listan como hallazgos):
- IDOR en contratos (`ContratoControlador`/`ContratoServicioImpl`) ya corregido mediante `ValidadorPertenenciaPedido`.
- Escapado HTML de campos de usuario en la generación de contratos (`HtmlUtils.htmlEscape`), previniendo XSS almacenado en el PDF/HTML del contrato.
- Hardening de CSP y actuator (ver sección 4, corrección de falso positivo).
- Procedimiento `sp_purgar_notificaciones` y su invocación desde el scheduler: totalmente parametrizados, sin concatenación de SQL.
- Almacenamiento del token de acceso en memoria (signal de Angular) en vez de `localStorage`, con refresh token en cookie httpOnly — patrón correcto.
- Endpoints públicos de catálogo (`CatalogoControlador`, `CreadorServicioControlador`) son intencionalmente públicos y coherentes con `SecurityConfig`.

### 2.3 Seguimiento sugerido (fuera del diff actual)
`PayPalWebhookControlador` es `permitAll()` por diseño y depende de que `IPagoServicio.procesarWebhookPayPal` valide la firma de transmisión de PayPal. No se auditó en profundidad la lógica de verificación de firma por estar fuera del cambio actual — se recomienda una revisión dedicada dado que afecta el estado de pagos.

---

## 3. Hallazgos — `springboot-security`

### 3.1 ✅ CORREGIDO (2026-08-26) — IDOR: historial y seguimiento de pedidos sin verificación de pertenencia
**Archivos:** [PedidoControlador.java:110-120](artisync/Backend/src/main/java/uteq/edu/ec/artisync/controller/pedido/PedidoControlador.java#L110-L120), [PedidoServicioImpl.java:329-377](artisync/Backend/src/main/java/uteq/edu/ec/artisync/service/pedido/impl/PedidoServicioImpl.java#L329-L377)

**Solución aplicada:** `obtenerHistorial`/`obtenerSeguimiento` reciben ahora
`idUsuarioSolicitante` (`IPedidoServicio`, `PedidoServicioImpl`) y validan con
`ValidadorPertenenciaPedido.validarPertenenciaOAdmin`, igual que `obtenerPedidoPorId`.
Los controladores pasan `@AuthenticationPrincipal CustomUserDetails`. Cubierto por
`PedidoServicioImplTest` (`obtenerHistorial_rechazaAjeno`, `obtenerHistorial_adminPuedeVer`,
`obtenerSeguimiento_rechazaAjeno`, `obtenerSeguimiento_adminPuedeVer`).

`GET /api/v1/pedidos/{id}/historial` y `GET /api/v1/pedidos/{id}/seguimiento` solo exigen `isAuthenticated()` y no pasan el usuario autenticado al servicio; a diferencia de `obtenerPedidoPorId` y los métodos de `ContratoServicioImpl`/`TicketRevisionServicioImpl` (que sí usan el `ValidadorPertenenciaPedido` recién extraído en este mismo cambio), estos dos métodos no validan que el pedido pertenezca al usuario solicitante.

**Explotación:** cualquier usuario autenticado puede enumerar `idPedido` y leer el historial de estados, observaciones internas y progreso de **cualquier** pedido de la plataforma, sin relación con él.

**Recomendación:** aplicar exactamente el mismo patrón ya usado en el resto de la clase — añadir `@AuthenticationPrincipal CustomUserDetails userDetails` en el controlador y llamar a `ValidadorPertenenciaPedido.validarPertenenciaOAdmin(pedido, userDetails.getIdUsuario())` en ambos métodos del servicio.

### 3.2 ✅ CORREGIDO (2026-08-26) — IDOR: consulta de briefing sin verificación de pertenencia
**Archivos:** [BriefingControlador.java:94-99](artisync/Backend/src/main/java/uteq/edu/ec/artisync/controller/comunicacion/BriefingControlador.java#L94-L99), [BriefingServiceImpl.java:157-162](artisync/Backend/src/main/java/uteq/edu/ec/artisync/service/comunicacion/impl/BriefingServiceImpl.java#L157-L162)

**Solución aplicada:** `BriefingService.obtenerBriefing` recibe ahora
`idUsuarioSolicitante` y valida con `ValidadorPertenenciaPedido.validarPertenenciaOAdmin`
sobre `enviado.getPedido()`. El controlador pasa `@AuthenticationPrincipal
CustomUserDetails`, igual que el resto de endpoints de la clase. Cubierto por
`BriefingServiceImplTest` (`obtenerBriefing_cliente_puedeConsultar`,
`obtenerBriefing_creador_puedeConsultar`, `obtenerBriefing_usuarioAjeno_lanzaAccessDenied`).

`GET /api/v1/pedidos/{idPedido}/briefing` es el único endpoint de `BriefingControlador` que **no** recibe `@AuthenticationPrincipal CustomUserDetails` (confirmado leyendo el archivo completo: los otros 4 endpoints de la clase sí lo reciben y lo usan). `obtenerBriefing(Long idPedido)` en el servicio busca directamente por `idPedido` sin ningún chequeo de pertenencia, mientras que `enviarBriefing` y `responderBriefing` en la misma clase sí verifican que el llamante sea el creador/cliente del pedido.

**Explotación:** cualquier usuario autenticado puede leer las respuestas de briefing (potencialmente datos de presupuesto/proyecto/personales) de un pedido de otro cliente, solo incrementando `idPedido`.

**Recomendación:** añadir `idUsuarioSolicitante` al método del servicio y aplicar `ValidadorPertenenciaPedido.validarPertenenciaOAdmin`, igual que en el resto de la clase.

### 3.3 🟢 BAJO — Falta `@Valid` en `PeticionAvanzarEtapa`
**Archivo:** [PedidoControlador.java:97](artisync/Backend/src/main/java/uteq/edu/ec/artisync/controller/pedido/PedidoControlador.java#L97)

Es el único `@RequestBody` del proyecto sin `@Valid`. El campo `observacion` del DTO tampoco tiene restricción de tamaño, permitiendo texto sin límite en el historial de auditoría inmutable del pedido. Impacto bajo (solo CREADOR/ADMIN autenticados), pero inconsistente con el resto del código.

**Recomendación:** añadir `@Valid` en el controlador y `@Size(max=...)` en el campo `observacion`.

### 3.4 Sugerencias adicionales de `springboot-security`
- Añadir `dependency-check-maven` (OWASP Dependency-Check) al `pom.xml` junto al SpotBugs/find-sec-bugs ya configurado, para detectar CVEs conocidos en dependencias como `openhtmltopdf`, `poi-ooxml`, el SDK de PayPal o `azure-storage-blob`.
- Evaluar `SameSite=Strict` para la cookie de refresh token si el flujo nunca requiere navegación cross-site de nivel superior (actualmente `Lax`, razonable pero no óptimo).
- Extender el `AuthRateLimitFilter` (actualmente solo en `/auth/**`) a endpoints de lectura enumerables como `pedidos/{id}/*`, para que una vez corregido el IDOR no quede expuesto a enumeración masiva.
- Una vez corregidos 3.1/3.2, revisar que `ManejadorGlobalExcepciones` mapee `AccessDeniedException` de `ValidadorPertenenciaPedido` consistentemente a 403 sin filtrar stack traces.
- Reducir `management.endpoints.web.exposure.include` a solo `health` en producción si `info`/`metrics` no se consumen activamente por monitoreo.

---

## 4. Corrección de falsos positivos detectados durante la consolidación

El primer análisis de `springboot-security` reportó dos hallazgos que, al verificarse directamente contra el archivo actual, **no reflejan el estado real del código** (posible lectura de una copia obsoleta en un entorno aislado):

| Hallazgo reportado inicialmente | Estado real verificado |
|---|---|
| "Actuator completamente abierto sin autenticar" | **Falso.** [SecurityConfig.java:71-72](artisync/Backend/src/main/java/uteq/edu/ec/artisync/config/SecurityConfig.java#L71-L72): solo `/actuator/health` es público; `/actuator/metrics/**` y `/actuator/info` requieren `ROLE_ADMIN`. |
| "CSP permite `unsafe-inline`/`unsafe-eval` en `script-src`" | **Falso.** [SecurityConfig.java:52](artisync/Backend/src/main/java/uteq/edu/ec/artisync/config/SecurityConfig.java#L52): `script-src 'self'` sin directivas inseguras (solo `style-src` mantiene `'unsafe-inline'`, de menor riesgo). |
| "`show-details=always` en health" | **Falso.** [application.properties:79](artisync/Backend/src/main/resources/application.properties#L79): `management.endpoint.health.show-details=when-authorized`. |

Esto confirma que el hardening H-05 documentado en sesiones anteriores está correctamente aplicado y no requiere acción adicional.

---

## 5. Hallazgos — `angular-developer`

### 5.1 🟠 ALTO — Cobertura de pruebas críticamente baja en una app zoneless
Solo 6 archivos `.spec.ts` existen para 144 archivos `.ts` de producción, y un único componente de página tiene test: [auditoria.component.spec.ts](artisync/Frontend/src/app/features/administracion/pages/auditoria/auditoria.component.spec.ts). En una aplicación **zoneless** (`provideZonelessChangeDetection()`), un componente que muta un campo de clase en vez de un signal renderiza de forma silenciosamente incorrecta y nada lo detecta sin tests. El spec existente es un buen molde (usa `provideZonelessChangeDetection`, `HttpTestingController`, asserts sobre signals) pero no se replicó al resto de páginas/modales/componentes de chat.

### 5.2 🟠 ALTO — Directivas legacy y tres estrategias de formularios conviviendo
`*ngIf`/`*ngFor`/`[(ngModel)]` conviven con `@if`/`@for`/`@switch` en al menos 11 archivos (p. ej. [user-form-modal.component.ts:126-154](artisync/Frontend/src/app/shared/components/user-form-modal/user-form-modal.component.ts#L126-L154), y `[(ngModel)]` en `mod-categorias`, `verificaciones`, `users`, `paises`, `roles-permissions`, `portafolio-creador`, `pedido-crear`, `flujos-admin`, `pedido-detalle`, `portafolio-edit`). Además `pais-form-modal`/`user-form-modal` usan Reactive Forms mientras otras pantallas CRUD casi idénticas usan `ngModel` o signals manuales — tres patrones de formulario compitiendo sin consolidar.

**Recomendación:** migrar a un único enfoque (idealmente Signal Forms, dado que el resto del estado ya es 100% signals) y reemplazar `*ngFor`/`ngModel` por `@for`/binding con signals.

### 5.3 🟡 MEDIO — `effect()` usado para disparar fetches HTTP imperativos
[app.ts:22-28](artisync/Frontend/src/app/app.ts#L22-L28) y [user-form-modal.component.ts:395-400](artisync/Frontend/src/app/shared/components/user-form-modal/user-form-modal.component.ts#L395-L400) usan `effect()` con `.subscribe()` interno para cargar datos al cambiar un signal — el caso de uso exacto que `resource()` fue diseñado para reemplazar. El segundo caso además no cancela una petición en curso si el efecto se re-ejecuta rápido.

### 5.4 🟡 MEDIO — Componentes de dropdown/datepicker sin semántica ARIA ni soporte de teclado
El [boton-exportar](artisync/Frontend/src/app/shared/components/boton-exportar/boton-exportar.component.html) (reutilizado en auditoría, usuarios, pedidos y comisiones) no tiene `aria-haspopup`/`aria-expanded`/`role="menu"` ni cierre con `Escape`. El datepicker/dropdown de país en `user-form-modal.component.ts` es un widget completamente manual, solo-mouse, sin `role="listbox"` ni navegación por flechas.

### 5.5 🟡 MEDIO — Diálogos modales sin semántica ni gestión de foco
[confirm-dialog.component.ts](artisync/Frontend/src/app/shared/components/confirm-dialog/confirm-dialog.component.ts) no tiene `role="dialog"`/`aria-modal`/`aria-labelledby`. `pais-form-modal` tiene `role="dialog" aria-modal="true"` pero sin `aria-labelledby` enlazado a su título, y ningún modal atrapa el foco ni lo devuelve al elemento que lo abrió al cerrar.

### 5.6 🟡 MEDIO — Chat sin región viva ni etiqueta accesible en el textarea
[chat-pedido.component.html:90-123](artisync/Frontend/src/app/features/comunicacion/components/chat-pedido/chat-pedido.component.html#L90-L123): la lista de mensajes que recibe actualizaciones por WebSocket no tiene `aria-live="polite"` (los usuarios de lector de pantalla no se enteran de mensajes nuevos), y el `<textarea>` del compositor solo tiene `placeholder`, sin `aria-label`/`<label>`.

### 5.7 🟢 BAJO/MEDIO — `[innerHTML]` en vista de contrato sin sanitización documentada
[contrato-vista.component.html:186](artisync/Frontend/src/app/features/legal/pages/contrato-vista/contrato-vista.component.html#L186) — no explotable hoy porque el backend escapa los campos de usuario antes de interpolarlos (ver 2.2), pero al ser HTML de contrato generado desde datos de usuario, se recomienda documentar/testear explícitamente esa dependencia en vez de confiar implícitamente en el sanitizador por defecto de Angular.

### 5.8 🟢 BAJO — Decorador `@Input` legado en un componente 100% signals
[chat-pedido.component.ts:18](artisync/Frontend/src/app/features/comunicacion/components/chat-pedido/chat-pedido.component.ts#L18) usa `@Input({ required: true })` en vez de `input.required<number>()`, inconsistente con el resto de la clase.

### 5.9 🟢 BAJO — Boilerplate manual de `Subscription`/`ngOnDestroy`
[dashboard-layout.component.ts:27-40](artisync/Frontend/src/app/layouts/dashboard-layout/dashboard-layout.component.ts#L27-L40) y componentes similares desuscriben manualmente en `ngOnDestroy` donde `takeUntilDestroyed()` eliminaría el boilerplate.

### 5.10 Sugerencias adicionales de `angular-developer`
- Ningún uso de `@defer` en toda la app — candidatos naturales: popovers del datepicker, páginas de reportes con gráficos/tablas pesadas, y cualquier modal (`@defer (on interaction)`).
- Varios `.subscribe()` en `ngOnInit` que solo cargan y asignan un signal (`explorar.component.ts`, `servicio-detalle.component.ts`, `briefing-pedido.component.ts`) son candidatos razonables para `resource()`.
- `user-form-modal.component.ts` importa `CommonModule` completo solo por `NgClass` — reducir a la importación específica una vez migrado el `*ngFor`.
- El lazy-loading de rutas está bien implementado en todo `app.routes.ts` (ningún feature module cargado de forma eager) — sin acción requerida, mencionado como buena práctica ya presente.

---

## 6. Relación con hallazgos previos ya conocidos

- Los 6 hallazgos P1 confirmados en la sesión de hoy (registro de memoria S89, 9:11pm) se mantienen como referencia — este informe no los repite en detalle, pero el hardening de CSP/actuator (H-05) y el scheduler de purga (H-08) que estaban en curso **ya se verificaron aplicados y correctos** (sección 4).
- Los dos IDOR nuevos (3.1 y 3.2) son huecos que quedaron **fuera** del alcance de la extracción de `ValidadorPertenenciaPedido` — el patrón correcto ya existe en el mismo cambio, solo falta aplicarlo a estos dos endpoints.
- No se identificó nada que contradiga la auditoría de backups (S89) ni el resto de hallazgos previamente documentados en `docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`.

---

## 7. Recomendaciones priorizadas

1. ~~**Antes de cualquier despliegue:** corregir el bug de transacción en `BriefingServiceImpl`/`ContratoServicioImpl` (1.1)~~ — ✅ **Corregido 2026-08-26** (`Propagation.REQUIRES_NEW`, ver 1.1).
2. ~~**Antes de cualquier despliegue:** cerrar los dos IDOR (3.1, 3.2) aplicando `ValidadorPertenenciaPedido`~~ — ✅ **Corregido 2026-08-26** (ver 3.1, 3.2).
3. ~~**Corto plazo:** corregir el export de usuarios sin filtros (1.3) por el riesgo de reportes de auditoría incorrectos.~~ — ✅ **Corregido 2026-08-26** (filtrado real en backend, ver 1.3).
4. **Corto plazo:** añadir tests a los componentes de página del frontend, empezando por los que tocan flujos críticos (pedidos, contratos, pagos, chat).
5. **Mediano plazo:** consolidar la estrategia de formularios en Angular (5.2) y cerrar las brechas de accesibilidad en modales/dropdowns/chat (5.4-5.6).
6. ~~**Mediano plazo:** extraer el util `aParams`/`params-desde-filtro` compartido (1.5) y aplicar `@Valid` en `PeticionAvanzarEtapa` (3.3).~~ — el util compartido ya está ✅ **corregido** (1.5); `@Valid`/`@Size` en `PeticionAvanzarEtapa` (3.3) sigue abierto.
7. **Seguimiento:** revisar la verificación de firma del webhook de PayPal (2.3) y añadir OWASP Dependency-Check al pipeline (3.4).
8. ~~Corregir 1.2 (consulta redundante) y 1.4 (export de comisiones sin filtros).~~ — ✅ **Corregidos 2026-08-26.**
