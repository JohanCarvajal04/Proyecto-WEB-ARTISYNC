# Especificación de Requisitos de Software (SRS)

## Artisync — Plataforma web de comisiones y venta de contenido digital para creadores

- **Conforme a:** ISO/IEC/IEEE 29148:2018 (estructura SRS) · INCOSE Guide to Writing Requirements v4 (calidad de requisitos C1–C15)
- **Versión:** v1.2.0 — Entrega Final
- **Fecha:** 2026-09-07
- **Precede a:** (Versión final)
- **Actualiza a:** SRS v1.1.1 (2026-09-07) — ver historial completo de versiones en `docs/requisitos/CHANGELOG-REQ.md`

> Nota de mantenimiento: cada cambio sustantivo respecto a la versión 1A se registra en `docs/requisitos/CHANGELOG-REQ.md`. Los identificadores `REQ-F-NNN` / `REQ-NF-NNN` reemplazan a los códigos `RF-NN` / `RNF-NN` de la Entrega 1A; la tabla de equivalencia está en la sección 6.

> **Fuente de verdad del estado.** El campo `Estado` de cada requisito es un espejo de la columna `estado` de [`docs/trazabilidad/matriz.csv`](../trazabilidad/matriz.csv), que es la fuente autoritativa porque es la única que exige, en la misma fila, el módulo, el endpoint, la prueba automatizada y la evidencia que sostienen ese estado. Este documento es la fuente de verdad del **enunciado**, la **prioridad MoSCoW** y el **criterio de aceptación**.
>
> Los valores admitidos son exactamente `pendiente`, `implementado` y `verificado`; `verificado` exige una prueba automatizada que lo respalde. `scripts/validate-traceability.sh` falla si ambos documentos divergen, si un requisito queda por debajo del estado que exige su prioridad sin figurar en [`docs/trazabilidad/excepciones-estado.txt`](../trazabilidad/excepciones-estado.txt), o si un `Must` verificado no declara prueba. Ambos archivos se actualizan en el mismo commit.

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica, de manera completa y verificable, los requisitos funcionales y no funcionales del sistema Artisync en su versión estable **v1.2.0**. Sirve como fuente única de verdad para la trazabilidad hacia el código, las pruebas automatizadas y la evidencia empírica exigida en la Entrega Final del PFC.

### 1.2 Alcance

Artisync es una plataforma web que centraliza la comercialización de servicios y productos digitales ofrecidos por profesionales creativos (ilustradores, músicos, desarrolladores, diseñadores, etc.). El sistema conecta a **Creadores** (vendedores) con **Clientes**, gestionando perfiles, catálogo, mensajería, contratos con firma electrónica, flujo de pedidos, pagos vía PayPal Orders v2 con patrón _escrow_, y funciones sociales (seguidores, comentarios, sorteos). El desarrollo se enmarca en un proyecto académico de 17 semanas; algunas capacidades (firma legalmente vinculante, logística física, recomendaciones avanzadas) quedan fuera de alcance y se documentan como trabajo futuro.

### 1.3 Definiciones, acrónimos y abreviaturas

- **RBAC**: Role-Based Access Control.
- **JWT**: JSON Web Token (RFC 7519).
- **2FA/TOTP**: Autenticación de doble factor basada en contraseñas de un solo uso (RFC 6238).
- **Escrow**: patrón de depósito en garantía; los fondos del Cliente se retienen hasta la aprobación del entregable.
- **SP**: Procedimiento almacenado (Stored Procedure).
- **MoSCoW**: Must, Should, Could, Won't — técnica de priorización de requisitos.

### 1.4 Referencias

ISO/IEC/IEEE 29148:2018; INCOSE Guide to Writing Requirements v4; RFC 7519 (JWT); RFC 7807 (Problem Details); RFC 6238 (TOTP); ISO/IEC 25010:2011 (calidad de software); OWASP Top 10:2021; documento interno `docs/requisitos/historico/entrega-1a.pdf` (corpus original de requisitos, semana del 4 de junio de 2026).

### 1.5 Resumen del documento

La sección 2 describe el producto y sus actores. La sección 3 detalla los requisitos funcionales (REQ-F). La sección 4 detalla los requisitos no funcionales (REQ-NF). La sección 5 presenta la matriz de trazabilidad resumida. La sección 6 documenta la evolución de los requisitos desde la Entrega 1A.

---

## 2. Descripción global

### 2.1 Perspectiva del producto

Artisync es un sistema nuevo, independiente, compuesto por un frontend Angular (SPA), un backend Spring Boot que expone una API REST, una base de datos PostgreSQL, una caché Redis, y tres integraciones externas: PayPal Orders v2 (pagos), un servicio de IA para verificación de documentos, y almacenamiento de objetos compatible con S3.

### 2.2 Funciones del producto (resumen)

Registro y autenticación con RBAC; verificación de identidad y de certificados profesionales; gestión de perfil y portafolio; publicación de productos/servicios en catálogo con atributos dinámicos; búsqueda y filtrado; mensajería en tiempo real con moderación automática de contacto externo; briefing configurable; generación de contrato HTML/PDF con firma electrónica; flujo de pedido por etapas; pagos con PayPal y patrón escrow; entrega con marca de agua y liberación de fondos; revisiones adicionales facturables; funciones sociales (seguidores, comentarios, sorteos).

### 2.3 Características de los usuarios

| Rol                  | Descripción                                                                                                |
| -------------------- | ---------------------------------------------------------------------------------------------------------- |
| Administrador        | Supervisa la plataforma, gestiona cuentas, categorías, publicaciones, contenido reportado y transacciones. |
| Creador de contenido | Publica servicios/productos, gestiona perfil y portafolio, atiende pedidos, organiza sorteos.              |
| Cliente registrado   | Contrata servicios, compra productos, sigue creadores, participa en sorteos.                               |
| Visitante anónimo    | Explora contenido público sin autenticarse.                                                                |

### 2.4 Restricciones

Proyecto académico de 17 semanas; equipo reducido; hosting local durante el desarrollo; integración de pagos limitada al entorno sandbox de PayPal; alcance geográfico inicial: Ecuador (validación de mayoría de edad, moneda USD).

### 2.5 Supuestos y dependencias

Se asume disponibilidad continua de las APIs externas (PayPal sandbox, servicio de IA, almacenamiento S3-compatible) durante las pruebas. La arquitectura se diseña contemplando extensión futura (multi-idioma, multi-moneda) sin comprometerlas en esta entrega.

---

## 3. Requisitos específicos — Funcionales

Cada requisito seguido de: **Rationale**, **Prioridad (MoSCoW)**, **Criterio de aceptación**, **Verificación** y **Estado** en v1.2.0. Los 23 requisitos provienen del corpus de la Entrega 1A y se mantienen con trazabilidad completa (ver tabla de equivalencia en §6); los 8 requisitos adicionales de §3.1 se incorporaron en v1.1.2/v1.2.0 — ver `CHANGELOG-REQ.md`.

### 3.0 Historias de Usuario y Criterio INVEST

Para cada uno de los 23 requisitos funcionales especificados a continuación, se ha documentado su correspondiente historia de usuario en formato Connextra. **La totalidad de las 23 historias de usuario han sido evaluadas rigurosamente y cumplen con los seis atributos del modelo INVEST** (Independent, Negotiable, Valuable, Estimable, Small, Testable). El análisis detallado individual que respalda el cumplimiento de INVEST para cada historia se encuentra archivado en el repositorio bajo `docs/requisitos/historias/`. A modo de ilustración, el análisis documentado para la HU-01 (Registro y autenticación) establece: *"Independiente del resto del flujo de autenticación; negociable en los campos exactos del formulario; valiosa (es la puerta de entrada al sistema); estimable y pequeña (un solo formulario + persistencia); testable mediante el criterio de aceptación."* Los ocho requisitos funcionales adicionales de §3.1 (REQ-F-024 a REQ-F-031) tienen igualmente su historia de usuario propia (HU-24 a HU-31, en `docs/requisitos/historias/HU-24-a-HU-31-adicionales.md`), evaluada con el mismo criterio INVEST. Los requisitos no funcionales (REQ-NF) no llevan historia de usuario ni caso de uso propios por diseño: su origen es una norma técnica, un ADR o una decisión de arquitectura, no una necesidad expresada por un rol de usuario — el campo correspondiente en `matriz.csv` permanece vacío intencionalmente para esos casos.

### Módulo Seguridad y Control de Acceso

**REQ-F-001** (ex RF-01) — El sistema debe permitir el registro de nuevos usuarios con selección de rol (Creador o Cliente); el rol determina las vistas y acciones disponibles durante toda la sesión.

- Rationale: separación de responsabilidades es la base del modelo de negocio de doble lado (oferta/demanda).
- Prioridad: Must
- Aceptación: un Creador no accede a rutas de Cliente y viceversa; el sistema redirige ante acceso no autorizado.
- Verificación: Test (JUnit + MockMvc sobre `AuthController`/`UserController`)
- Estado: verificado

**REQ-F-002** (ex RF-02) — Control de acceso basado en roles (RBAC): cada acción debe estar asociada a un permiso específico asignado al rol.

- Rationale: exigido por RNF de seguridad y por la separación de privilegios del dominio.
- Prioridad: Must
- Aceptación: asignar/revocar un permiso cambia el acceso sin reiniciar sesión (revocación efectiva en la siguiente solicitud).
- Verificación: Test (`RolePermissionControllerTest`)
- Estado: verificado

**REQ-F-003** (ex RF-03) — Gestión de sesiones mediante JWT con expiración de 24 horas; rutas protegidas exigen token válido en la cabecera de autorización.

- Rationale: autenticación _stateless_ escalable sin sesión en servidor.
- Prioridad: Must
- Aceptación: token válido → 200; token expirado → 401 "Token expirado"; sin cabecera → 401 "Autenticación requerida"; token de sesión cerrada no reutilizable; el token de acceso expira a las 24h y el refresh token a los 7 días (configurable vía `JWT_REFRESH_EXPIRATION`).
- Verificación: Test (`JwtAuthenticationFilterTest`, `AuthServiceImplTest`)
- Estado: verificado

**REQ-F-004** (ex RF-04) — Recuperación de contraseña mediante enlace de un solo uso, válido 60 minutos, enviado por correo.

- Prioridad: Must
- Aceptación: enlace usado o expirado → mensaje de invalidez; tras el flujo, login inmediato con nueva contraseña.
- Verificación: Test unitario + prueba manual de flujo de correo
- Estado: verificado

**REQ-F-005** (ex RF-05) — 2FA opcional basada en TOTP (RFC 6238), disponible solo para usuarios con identidad verificada.

- Prioridad: Should
- Aceptación: código incorrecto/expirado → "Código inválido o expirado"; usuario no verificado no ve la opción.
- Verificación: Test (`TwoFactorServiceImplTest`, `TwoFactorController`)
- Estado: verificado

### Módulo Perfiles, Verificación y Portafolio

**REQ-F-006** (ex RF-06) — Verificación de mayoría de edad del Creador mediante documento de identidad analizado por servicio externo; el documento se elimina del almacenamiento tras la respuesta.

- Prioridad: Must
- Aceptación: aprobado → estado verificado en ≤60s y notificación; minoría de edad → estado sin cambios y mensaje de rechazo; documento no accesible tras respuesta.
- Verificación: Test + inspección de almacenamiento
- Estado: verificado (flujo cubierto por `VerificacionServicioImplTest`, `VerificacionControladorTest` y `CertificadoIaRepositoryIT`; el proveedor de IA se sustituye por un doble en las pruebas, de modo que la integración con el servicio real queda como riesgo declarado en §6)

**REQ-F-007** (ex RF-07) — Verificación de certificados profesionales por IA; puntaje ≥ umbral configurable (0.75 por defecto) habilita sello de verificación.

- Prioridad: Should
- Aceptación: puntaje ≥ umbral → sello inmediato; puntaje menor → sin sello + notificación; umbral configurable sin cambio de código.
- Verificación: Test + demostración
- Estado: verificado

**REQ-F-008** (ex RF-08) — Personalización de perfil público: foto (JPG/PNG ≤5MB), biografía (≤500 caracteres, sin teléfono/correo), hasta 3 URLs de redes sociales.

- Prioridad: Must
- Aceptación: biografía con contacto directo → rechazo; imagen >5MB → rechazo; URL válida se muestra como enlace.
- Verificación: Test (`PerfilCreadorControlador`)
- Estado: verificado

**REQ-F-009** (ex RF-09) — El perfil público muestra seguidores, servicios activos, calificación promedio y estado de verificación; cualquier usuario autenticado puede seguir/dejar de seguir.

- Prioridad: Must
- Aceptación: contador se actualiza de inmediato al seguir/dejar de seguir; usuario no autenticado es redirigido a login.
- Verificación: Test + demostración
- Estado: verificado

**REQ-F-010** (ex RF-10) — Comentarios en ítems de portafolio; el Creador puede eliminarlos (borrado lógico, no visibles en vista pública, consultables por el administrador).

- Prioridad: Should
- Verificación: Test
- Estado: implementado (pendiente escribir la prueba automatizada para subir a verificado; ver matriz.csv)

### Módulo Catálogo Dinámico de Servicios

**REQ-F-011** (ex RF-11) — Publicación de ítems tipo Producto o Servicio, con precio (≥0.01 USD), al menos una imagen (≤10MB) y descripción (20–2000 caracteres) obligatorios.

- Prioridad: Must
- Verificación: Test (`ServicioControlador`)
- Estado: verificado

**REQ-F-012** (ex RF-12) — Hasta 10 atributos personalizados por ítem; formularios adaptados dinámicamente a la categoría del Creador.

- Prioridad: Must
- Verificación: Test
- Estado: verificado

**REQ-F-013** (ex RF-13) — Motor de búsqueda con filtros por categoría, subcategoría, rango de precio y etiquetas, más búsqueda textual sobre título/descripción; edición de ítems en cualquier momento.

- Prioridad: Must
- Verificación: Test (Specification API — `specification/catalogo`)
- Estado: verificado

### Módulo Comunicación y Notificaciones

**REQ-F-014** (ex RF-14) — Mensajería interna en tiempo real vía WebSocket; sala de chat creada automáticamente al firmar el contrato; se cierra al llegar a Entregado o Cancelado.

- Prioridad: Must
- Verificación: Test + prueba de carga WebSocket (ver REQ-NF-005)
- Estado: verificado

**REQ-F-015** (ex RF-15) — Análisis de contenido de mensajes para detectar teléfonos/correos; bloqueo de entrega y aviso; suspensión de 15 días tras 3 infracciones en 30 días.

- Prioridad: Must
- Verificación: Test
- Estado: verificado

**REQ-F-016** (ex RF-16) — Cuestionario (briefing) configurable (hasta 10 preguntas) asociado a un servicio del Creador; si el servicio tiene uno asignado, el Cliente lo responde al crear el pedido (obligatorio antes de que el pedido se registre); respuestas no editables tras el envío. Un servicio sin cuestionario asignado no bloquea la creación del pedido.

- Prioridad: Must
- Verificación: Test
- Estado: verificado

### Módulo Legal, Entregables y Finanzas

**REQ-F-017** (ex RF-17) — Generación automática de contrato HTML desde la plantilla asignada al servicio del pedido (catálogo de plantillas curado por Administrador), o la plantilla predeterminada si el servicio no tiene una propia, sustituyendo variables (partes, servicio, precio, revisiones, fecha).

- Prioridad: Must
- Verificación: Test (`ContratoControlador`)
- Estado: verificado

**REQ-F-018** (ex RF-18) — Firma electrónica como acción explícita de cada parte; el pedido no avanza sin ambas firmas; PDF descargable con hashes de firma.

- Prioridad: Must
- Verificación: Test
- Estado: verificado

**REQ-F-019** (ex RF-19) — Flujo de trabajo del pedido por etapas configurables según categoría; cada transición registrada con marca de tiempo; vista de seguimiento en tiempo real para el Cliente.

- Prioridad: Must
- Verificación: Test (`FlujoTrabajoControlador`, `PedidoControlador`)
- Estado: verificado

**REQ-F-020** (ex RF-20) — Generación de enlace de pago vía PayPal Orders v2 al iniciar pedido; actualización de estado de fondos al recibir webhook confirmado.

- Prioridad: Must
- Verificación: Test (`PayPalWebhookControlador`, sandbox)
- Estado: verificado (el webhook y su validación de firma están cubiertos por `PagoServicioImplWebhookTest`; queda como trabajo futuro la validación end-to-end contra el sandbox real de PayPal)

**REQ-F-021** (ex RF-21) — Entrega con marca de agua para previsualización; aprobación del Cliente libera fondos y habilita descarga limpia; comisión de plataforma registrada automáticamente.

- Prioridad: Must
- Aceptación: la comisión de plataforma es 10% del monto bruto (configurable vía `PLATAFORMA_COMISION_TASA`), registrada automáticamente en la transacción al aprobar el entregable.
- Verificación: Test (`EntregableControlador`, `PagoControlador`)
- Estado: verificado

**REQ-F-022** (ex RF-22) — Cargo configurable por revisión adicional; ticket que supera el límite genera nuevo enlace de pago; rechazo automático tras 48h sin pago.

- Prioridad: Should
- Verificación: Test (`TicketRevisionControlador`)
- Estado: implementado (el cargo configurable por revisión adicional está implementado y probado en `TicketRevisionServicioImplTest`; el nuevo enlace de pago al superar el límite de revisiones y el rechazo automático tras 48h sin pago no tienen servicio de pago ni scheduler asociado en el código — ver excepción declarada en `docs/trazabilidad/excepciones-estado.txt`)

### Módulo Social, Comunidad y Sorteos

**REQ-F-023** (ex RF-23) — Creación de sorteos (título, premio, ganadores, fechas, requisito de seguidor); selección aleatoria automática de ganadores al cierre.

- Prioridad: Could
- Verificación: Test + demostración
- Estado: verificado

### 3.1 Requisitos adicionales (post v1.0.0)

Los 23 requisitos anteriores (REQ-F-001 a REQ-F-023) son el corpus original heredado de la Entrega 1A. Los ocho requisitos siguientes (REQ-F-024 a REQ-F-031) se incorporan en v1.1.2 tras una auditoría del SRS contra el código real: documentan módulos que ya estaban implementados y probados en el backend, pero que no tenían ningún requisito funcional que los especificara. No representan alcance nuevo del sistema, sino especificación que le faltaba a alcance ya construido — ver `CHANGELOG-REQ.md` v1.2.0 para el detalle de por qué cada uno faltaba.

### Módulo Financiero — Retiros y Auditoría de Pagos

**REQ-F-024** — El Creador debe poder configurar su correo de PayPal y solicitar el retiro de su saldo disponible; el Auditor Financiero (o Administrador) revisa la cola de solicitudes y las aprueba, rechaza o reintenta.

- Rationale: el escrow de REQ-F-021 retiene fondos hasta la aprobación del entregable, pero sin un flujo de retiro el Creador nunca recibe el dinero fuera de la plataforma; es el cierre natural del ciclo de pago.
- Prioridad: Must
- Aceptación: solicitar retiro sin correo de PayPal configurado → rechazo; monto menor al mínimo configurado (USD 10.00 por defecto, `RETIROS_MONTO_MINIMO`) → rechazo; monto mayor al saldo disponible → rechazo; ya existe una solicitud en curso → rechazo; aprobar ejecuta el pago vía PayPal Payouts y cambia el estado; rechazar exige nota administrativa; reintentar solo aplica a solicitudes en estado "Fallido".
- Verificación: Test (`SolicitudRetiroServicioImplTest`, `SolicitudRetiroServicioImplPayoutTest`, `SolicitudRetiroControladorTest`, `SolicitudRetiroAdminControladorTest`, `SolicitudRetiroAutorizacionTest`, `SolicitudRetiroConcurrenciaIT`, `DatosPagoControladorTest`)
- Estado: verificado

**REQ-F-025** — El Auditor Financiero (o Administrador) debe poder listar de forma paginada y filtrada los pagos en garantía (escrow), ver el detalle con su historial de transacciones, y obtener un resumen agregado de fondos por estado.

- Rationale: el patrón escrow retiene dinero de clientes; sin una pantalla de supervisión, nadie puede auditar cuánto dinero está retenido, por cuánto tiempo, ni en qué estado, lo cual es un riesgo financiero y de cumplimiento.
- Prioridad: Must
- Aceptación: acceso restringido a `PAGO_AUDITAR` o rol Administrador; el resumen agrega cantidad y monto total por estado; el detalle de un pago inexistente devuelve 404.
- Verificación: Test (`PagoGarantiaAuditoriaServicioImplTest`)
- Estado: verificado

### Módulo de Reportes

**REQ-F-026** — El Administrador (o titular de `TRANSACCION_VER`/`REPORTE_FINANCIERO_EXPORTAR`/`REPORTE_CONTRATO_EXPORTAR`) debe poder consultar y exportar en CSV, XLSX o PDF: (a) el reporte de comisiones por creador (bruto, comisión, neto, detalle de transacciones) y (b) el listado de contratos formalizados (servicio, partes, precio, estado de firma).

- Rationale: estos reportes exponen datos financieros y de contratos de terceros; sin requisito propio, nadie declaró quién puede generarlos ni bajo qué límites.
- Prioridad: Should
- Aceptación: exportar con un número de transacciones que excede el tope de filas del formato solicitado → rechazo (`ExcepcionReglaNegocio`); el acceso a `/exportar` exige el permiso específico, distinto del permiso de solo lectura del reporte.
- Verificación: Test (`ReporteFinancieroServicioImplTest`, `ReporteFinancieroAutorizacionTest`, `ReporteContratoServicioImplTest`, `ReporteContratoAutorizacionTest`)
- Estado: verificado

### Módulo de Notificaciones

**REQ-F-027** — El sistema debe mantener un centro de notificaciones transversal por usuario, alimentado por eventos de otros módulos (pedidos, pagos, verificación, etc.); el usuario puede listar sus notificaciones paginadas, marcarlas como leídas (una o todas) y consultar el contador de no leídas.

- Rationale: varios módulos (verificación, pedidos, retiros) ya notifican al usuario en la práctica; sin este requisito, el mecanismo transversal que los sostiene no está especificado.
- Prioridad: Should
- Aceptación: listar devuelve solo las notificaciones del usuario autenticado; marcar como leída una notificación ajena → 404; el contador de no leídas baja a 0 tras "marcar todas".
- Verificación: Test (`NotificacionServiceImplTest`, `NotificacionControladorTest`)
- Estado: verificado

### Módulo Catálogo — Gestión Auxiliar

**REQ-F-028** — El Creador debe poder crear categorías propias (quedan sin revisar hasta que un Moderador/Administrador las apruebe); el Moderador/Administrador debe poder gestionar categorías, subcategorías y etiquetas del catálogo (crear, editar, desactivar), revisar las pendientes de creadores, y moderar servicios ajenos quitándoles una subcategoría inapropiada.

- Rationale: REQ-F-011/012/013 dan por hecho un catálogo de categorías/subcategorías/etiquetas ya existente, pero ningún requisito especifica cómo se crean, revisan ni moderan esos catálogos, y hoy tienen CRUD y flujo de revisión propios.
- Prioridad: Must (es prerrequisito operativo de REQ-F-011/012/013)
- Aceptación: una categoría creada por un Creador con `CATEGORIA_CREAR` queda pendiente de revisión y no aparece en el listado público hasta ser marcada revisada; quitar la última subcategoría de un servicio en moderación → rechazo (`ExcepcionReglaNegocio`).
- Verificación: Test (`CategoriaServicioImplTest`, `CategoriaControladorTest`, `CategoriaAutorizacionTest`, `EtiquetaServicioImplTest`, `ServicioCatalogoServicioImplTest` — cubre la moderación de subcategorías)
- Estado: verificado

### Módulo Social — Reseñas

**REQ-F-029** — El Cliente debe poder calificar (1-5 estrellas) y reseñar un pedido solo después de que el entregable fue liberado (aprobado); puede editar o eliminar su propia reseña; cualquier visitante puede consultar las reseñas y el promedio de un Creador.

- Rationale: REQ-F-009 ya menciona que el perfil "muestra... calificación promedio", pero ese requisito solo cubre la exhibición, no el flujo completo de creación/edición/eliminación que alimenta ese promedio.
- Prioridad: Must
- Aceptación: crear reseña sobre un pedido sin entregable liberado → rechazo; crear una segunda reseña sobre el mismo pedido → rechazo (`ExcepcionRecursoDuplicado`); el promedio público se recalcula tras cada alta/edición/baja.
- Verificación: Test (`ResenaServiceImplTest`, `ResenaControladorTest`)
- Estado: verificado

### Módulo Seguridad — Administración de Cuentas

**REQ-F-030** — El Administrador (o titular de los permisos `USUARIO_*`/`ROL_GESTIONAR`/`SESION_REVOCAR`) debe poder listar, exportar, crear, editar, activar/desactivar/suspender, asignar roles, revocar sesiones y eliminar lógicamente cuentas de usuario desde el panel administrativo.

- Rationale: es distinto de REQ-F-002 (RBAC operativo por permiso): este requisito es la gestión del ciclo de vida de la cuenta en sí, con auto-protecciones (un administrador no puede desactivarse, cambiarse sus propios roles ni eliminarse a sí mismo).
- Prioridad: Should
- Aceptación: un administrador que intenta desactivar, cambiar roles o eliminar su propia cuenta → rechazo (`ExcepcionReglaNegocio`); revocar sesiones invalida inmediatamente cualquier JWT activo del usuario afectado.
- Verificación: Test (`AdminUserServiceImplTest`, `AdminUserControllerTest`)
- Estado: verificado

### Módulo Auxiliar — Catálogo de Países y Portafolio Social

**REQ-F-031** — El sistema debe mantener un catálogo maestro de países (CRUD administrativo, listado de activos) para uso en formularios de perfil/verificación, y permitir a cualquier usuario autenticado dar o quitar "me gusta" a un ítem de portafolio, con conteo público.

- Rationale: son dos capacidades menores, sin impacto financiero ni legal, agrupadas por bajo riesgo; documentarlas cierra la brecha de cobertura sin inflar la prioridad del corpus.
- Prioridad: Could
- Aceptación: dar like dos veces al mismo ítem → rechazo (`ExcepcionRecursoDuplicado`); el estado de like es consultable sin autenticación (usuario anónimo ve el conteo, no si "ya dio like").
- Verificación: Test (`PaisServiceImplTest`, `PaisControllerTest`, `LikePortafolioServiceImplTest`, `LikePortafolioControladorTest`)
- Estado: verificado

---

## 4. Requisitos específicos — No funcionales

**REQ-NF-001** (ex RNF-01) — Seguridad/Transporte: redirección forzada a HTTPS (301); rechazo de TLS <1.2; TLS 1.3 preferente.

- Prioridad: Must · Verificación: análisis (SSL Labs) · Estado: implementado (configuración acreditada en `docs/mediciones/sec/owasp/a02-tls.txt`; el sistema ya está desplegado en Render — ver `render.yaml` y `docs/mediciones/lighthouse/REPORTE-LIGHTHOUSE.md` — pero el análisis externo con SSL Labs contra el dominio público todavía no se ejecutó ni archivó — excepción declarada en `docs/trazabilidad/excepciones-estado.txt`)

**REQ-NF-002** (ex RNF-02) — Contraseñas con hash bcrypt, factor de coste ≥10; nunca texto plano.

- Prioridad: Must · Verificación: inspección de BD · Estado: verificado

**REQ-NF-003** (ex RNF-03) — JWT firmado HS256 con clave ≥256 bits en variable de entorno (nunca en código/repositorio); rechazo de firma inválida con 401.

- Prioridad: Must · Verificación: análisis + test · Estado: verificado (clave vía `.env`, validada al arrancar; los claims `iss`, `aud`, `nbf` y `jti` se emiten y se validan al parsear — ver OBS-AUTO-01 y OBS-AUTO-08 en `docs/observaciones/OBSERVACIONES.md`)

**REQ-NF-004** (ex RNF-04) — LCP del catálogo ≤2s bajo 4G simulada con ≥20 servicios publicados.

- Prioridad: Should · Verificación: Lighthouse · Estado: implementado (LCP medido ~2.8s)

**REQ-NF-005** (ex RNF-05) — WebSocket: ≥10 conexiones simultáneas sin degradación; latencia extremo-a-extremo ≤500ms en red local.

- Prioridad: Should · Verificación: script de carga (ws/wscat) · Estado: implementado de validación

**REQ-NF-006** (ex RNF-06) — Generación de contrato PDF ≤5s bajo carga normal.

- Prioridad: Should · Verificación: timestamps de log, 5 mediciones · Estado: implementado de validación

**REQ-NF-007** (ex RNF-07) — Interfaz sin desbordamiento horizontal en 360/768/1440px; controles operables táctilmente (≥44px).

- Prioridad: Should · Verificación: DevTools · Estado: implementado

**REQ-NF-008** (ex RNF-08) — Formularios de catálogo dinámicos sin recarga; flujo de contratación en ≤5 pantallas.

- Prioridad: Should · Verificación: prueba manual · Estado: implementado

**REQ-NF-009** (ex RNF-09) — Disponibilidad durante semanas de evaluación 16–17; reinicio automático ante fallos.

- Prioridad: Must · Verificación: demostración (ps aux, healthcheck Docker) · Estado: implementado (los cinco servicios de `artisync/docker-compose.yml` declaran `restart: unless-stopped` y healthcheck, y el sistema ya está desplegado en Render; falta archivar una demostración de caída y recuperación, local o en el entorno real, para elevarlo a verificado — excepción declarada en `docs/trazabilidad/excepciones-estado.txt`)

**REQ-NF-010** (ex RNF-10) — Módulos WebSocket, REST y generación de PDF desacoplados (sin imports cruzados directos).

- Prioridad: Should · Verificación: inspección de dependencias · Estado: implementado (paquetes separados por módulo)

**REQ-NF-011** (ex RNF-11) — Archivos binarios en almacenamiento externo compatible con S3; sin archivos locales en el servidor.

- Prioridad: Must · Verificación: inspección de URLs · Estado: implementado (`AlmacenamientoAzure` existe y está probado — `AlmacenamientoAzureTest`, `AlmacenamientoAzureIntegracionTest` —, pero `documentos.proveedor` tiene `local` como valor por defecto en `application.properties` y `render.yaml` no fija `DOCUMENTOS_PROVEEDOR=azure` en las variables de entorno del despliegue; sin verificar esa variable en el entorno real de producción no se puede certificar `verificado` — ver excepción declarada en `docs/trazabilidad/excepciones-estado.txt`)

**REQ-NF-012** (ex RNF-12) — Bloqueo de registro a menores de 18; checkbox obligatorio de términos y privacidad.

- Prioridad: Must · Verificación: test + inspección HTML · Estado: verificado

**REQ-NF-013** (ex RNF-13) — Auditoría inmutable de transiciones de pedido y transacciones; exportación CSV por el administrador.

- Prioridad: Must · Verificación: test (UPDATE/DELETE/TRUNCATE → error de base de datos) · Estado: verificado. Además de `historial_estados_pedido` (dominio) y el exportador de transacciones (`AuditControlador`), existe desde V15\_\_modulo_auditoria.sql una bitácora transversal `auditoria_eventos` con trigger PL/pgSQL que bloquea UPDATE/DELETE/TRUNCATE (SQLState 42501) y GRANT restringido a `SELECT, INSERT` para la cuenta de aplicación, alimentada por un aspecto AOP (`@Auditable`) sobre los 7 módulos, expuesta en `/api/v1/admin/auditoria` con listado filtrado, detalle y exportación CSV. Verificado con `EventoAuditoriaInmutabilidadIT` contra PostgreSQL real.

**REQ-NF-014** (ex RNF-14) — Integración exclusiva con PayPal Orders v2; credenciales en variables de entorno; verificación de firma de webhook.

- Prioridad: Must · Verificación: inspección de Git + simulación de webhook inválido · Estado: verificado (credenciales de PayPal vía `.env`; `PagoServicioImplWebhookTest` cubre firma inválida, cabeceras ausentes, payload ilegible y ausencia de `webhook-id`, además del camino feliz)

### 4.1 Requisitos no funcionales adicionales (post v1.0.0)

Igual que en §3.1, los tres requisitos siguientes (REQ-NF-015 a REQ-NF-017) se incorporan en v1.1.2 tras auditar el SRS contra la evidencia ya archivada en `docs/mediciones/` y el código real: formalizan umbrales y comportamientos que el equipo ya perseguía o medía de facto, pero que ningún requisito capturaba.

**REQ-NF-015** — Ante indisponibilidad de Redis, los servicios que dependen de él (cuota de intentos de login, lista de revocación de JWT, caché del catálogo) deben degradar de forma explícita y documentada (fail-open o fail-closed según el servicio, ver ADR-004), nunca fallar en silencio; el TTL de la caché del catálogo debe ser configurable por variable de entorno.

- Prioridad: Must · Verificación: Test (simulación de caída de Redis) · Estado: verificado (`IntentosAutenticacionServiceTest` prueba explícitamente los dos escenarios "Redis caído, fail-open" para verificación de cuota y limpieza; `app.cache.catalogo.ttl-seconds` es configurable vía `CATALOGO_CACHE_TTL`, ver `docs/adr/adr-004-estrategia-cache.md`)

**REQ-NF-016** — La cobertura de código del backend (líneas) debe ser ≥70% según JaCoCo, medida sobre la rama principal antes de cada entrega.

- Prioridad: Should · Verificación: `docs/mediciones/jacoco/REPORTE-JACOCO.md` · Estado: verificado (86,75% líneas / 75,03% ramas, medición del 2026-09-05, supera el umbral con margen)

**REQ-NF-017** — La usabilidad percibida del frontend, medida con System Usability Scale (SUS) sobre una muestra representativa de usuarios, debe alcanzar un puntaje ≥68/100.

- Prioridad: Should · Verificación: `docs/mediciones/sus/REPORTE-SUS.md` · Estado: implementado, no cumple el umbral (61,25/100 medido el 2026-08-16, calificación "D"; por debajo de 68 — brecha reconocida, excepción declarada en `docs/trazabilidad/excepciones-estado.txt`)

---

## 5. Matriz de trazabilidad (resumen)

Ver archivo completo en `docs/trazabilidad/matriz.csv`. Estructura de columnas: `id_requisito, tipo, prioridad_moscow, historia_usuario, caso_de_uso, modulo_codigo, endpoint_api, prueba_automatizada, tipo_acceso, evidencia_empirica, estado`.

## 6. Evolución de requisitos desde la Entrega 1A

| Cambio                | Detalle                                                                                                                                                                    |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Renombrado de IDs     | `RF-NN` → `REQ-F-0NN`, `RNF-NN` → `REQ-NF-0NN`, sin alterar el contenido semántico, para conformidad con ISO/IEC/IEEE 29148.                                               |
| Adición de atributos  | Se agregó `rationale`, `estado` (pendiente/implementado/verificado) y `método de verificación` explícito a cada requisito, ausentes en la tabla original de la Entrega 1A. |
| Resolución de brechas | REQ-F-009, 014, 015, 016, 023 (módulos social, comunicación, WebSockets) implementados y marcados como `verificado` para v1.0.0. REQ-F-010 (comentarios de portafolio) pasó de `pendiente` a `implementado`: se construyó la rodaja vertical completa (servicio, controlador y moderación admin) con borrado lógico, ya sin necesidad de excepción por ser Should. Queda pendiente su prueba automatizada para subirlo a `verificado`. |
| Resolución de brechas | REQ-NF-004, 007, 008, 011 resueltos con el frontend Angular 22 finalizado y la integración de Azure Blob Storage.                                                          |
| Módulo de Auditoría   | REQ-NF-013 implementado formalmente mediante la migración V12, funciones PL/pgSQL, y aspecto AOP (`@Auditable`).                                                           |
| Reescritura de enunciado (v1.1.0) | REQ-F-016 y REQ-F-017 cambiaron de enunciado (ver `CHANGELOG-REQ.md` v1.1.0): el cuestionario (briefing) pasó de ser un formulario genérico enviado manualmente por el Creador tras crear el pedido, a estar ligado a un servicio concreto y ser obligatorio dentro del mismo formulario de creación del pedido; la plantilla de contrato pasó de ser una única plantilla global sembrada por migración, a un catálogo de plantillas curado por Administrador que el Creador elige por servicio. |

Todo cambio adicional debe registrarse en `docs/requisitos/CHANGELOG-REQ.md` con fecha, autor, requisito afectado y motivo, siguiendo la convención Keep a Changelog.

---

## 7. Métricas de calidad del corpus de requisitos

Todas las cifras se derivan de `docs/trazabilidad/matriz.csv` en la fecha de este documento y son reproducibles ejecutando `scripts/validate-traceability.sh`, que además impide que estas métricas se desincronicen del SRS.

### 7.1 Volumen y distribución

| Métrica                            | Valor                                                             |
| ---------------------------------- | ----------------------------------------------------------------- |
| Total de requisitos                | 48                                                                |
| Por tipo                           | 31 funcionales (64,6 %) · 17 no funcionales (35,4 %)              |
| Por prioridad MoSCoW               | 31 Must (64,6 %) · 15 Should (31,3 %) · 2 Could (4,2 %)           |
| Por estrategia de acceso a datos   | 33 CRUD-ORM · 8 SP · 7 sin acceso a datos (frontend/arquitectura) |

El corpus original de la Entrega 1A (37 requisitos, REQ-F-001 a REQ-F-023 y REQ-NF-001 a REQ-NF-014) se amplió en v1.1.2 con 11 requisitos adicionales (REQ-F-024 a REQ-F-031, REQ-NF-015 a REQ-NF-017) que documentan funcionalidad ya implementada — ver §3.1, §4.1 y `CHANGELOG-REQ.md` v1.2.0.

### 7.2 Estado de verificación

| Estado         | Requisitos | Porcentaje |
| -------------- | ---------- | ---------- |
| `verificado`   | 36         | 75,0 %     |
| `implementado` | 12         | 25,0 %     |
| `pendiente`    | 0          | 0,0 %      |

Desglose por prioridad, que es lo que evalúa el criterio D0R:

| Prioridad | Verificado | Implementado | Pendiente | Cumple el mínimo exigido           |
| --------- | ---------- | ------------ | --------- | ---------------------------------- |
| Must      | 28 (90,3 %) | 3            | 0         | 28 de 31; 3 con excepción declarada |
| Should    | 6          | 9            | 0         | 15 de 15; 2 declarados por honestidad, sin exigir excepción |
| Could     | 2          | 0            | 0         | Sin mínimo exigible                 |

Los tres requisitos Must que no alcanzan `verificado` están declarados uno a uno, con su motivo y su condición de cierre, en [`docs/trazabilidad/excepciones-estado.txt`](../trazabilidad/excepciones-estado.txt): REQ-NF-001 y REQ-NF-009 tienen la funcionalidad y configuración implementadas, pero falta archivar el análisis externo (SSL Labs) y la demostración de caída/recuperación respectivamente, contra el despliegue real ya existente en Render; REQ-NF-011 tiene el almacenamiento en Azure implementado y probado, pero la variable de entorno que lo activa en producción (`DOCUMENTOS_PROVEEDOR`) no está fijada en `render.yaml`. Además, dos requisitos Should en `implementado` se documentan igual por honestidad aunque ya cumplen su mínimo formal: REQ-F-022 (falta el scheduler de rechazo automático a 48h) y REQ-NF-017 (el resultado medido de usabilidad, 61,25/100, no alcanza el umbral propio del proyecto).

### 7.3 Cobertura de trazabilidad

| Métrica                                     | Valor            |
| ------------------------------------------- | ---------------- |
| Requisitos presentes en la matriz           | 48 / 48 (100 %)  |
| Requisitos con prueba automatizada asociada | 39 (81,3 %)      |
| Requisitos Must con prueba automatizada     | 29 / 31 (93,5 %) |
| Requisitos con evidencia empírica archivada | 31 (64,6 %)      |

Ningún requisito figura como `verificado` sin una prueba automatizada que lo respalde: es una regla que el validador impone y que hace fallar el pipeline si se incumple. Los 2 requisitos Must sin prueba automatizada (REQ-NF-001, REQ-NF-009) son, no por casualidad, los mismos que no alcanzan `verificado`: su verificación depende de un análisis externo o una demostración operativa, no de una prueba de código.

### 7.4 Estabilidad de requisitos

La tasa de estabilidad se calcula como `1 − (requisitos modificados / requisitos totales)` entre la Entrega 1A y la Entrega Final, tomando como modificación cualquier cambio de **enunciado, prioridad o alcance** registrado en `CHANGELOG-REQ.md`. No cuentan los cambios de estado, que reflejan el avance de la implementación y no inestabilidad de la especificación.

| Métrica                            | Valor                                     |
| ---------------------------------- | ----------------------------------------- |
| Requisitos en la Entrega 1A        | 37 (RF-01 a RF-23 · RNF-01 a RNF-14)      |
| Requisitos en v1.2.0                | 48 (REQ-F-001 a REQ-F-031 · REQ-NF-001 a REQ-NF-017) |
| Añadidos                           | 11 (REQ-F-024 a REQ-F-031, REQ-NF-015 a REQ-NF-017 — ver CHANGELOG-REQ.md v1.2.0) |
| Eliminados                         | 0                                         |
| Modificados en enunciado o alcance | 2 (REQ-F-016, REQ-F-017 — ver CHANGELOG-REQ.md v1.1.0) |
| **Tasa de estabilidad del corpus heredado** | **1 − 2/37 = 0,946 (94,6 %)**       |
| Tasa de adición                    | 11/37 = 29,7 % sobre el corpus original   |
| Tasa de eliminación                | 0 %                                       |

El corpus heredado de la Entrega 1A permaneció **estable en volumen**: los mismos 37 requisitos originales, con correspondencia uno a uno de identificadores; ninguno se eliminó. Lo que cambió entre 1A y v1.0.0 fue la *forma* de la especificación, no su contenido: la renumeración de `RF-NN`/`RNF-NN` a `REQ-F-NNN`/`REQ-NF-NNN` para conformidad con ISO/IEC/IEEE 29148, y el enriquecimiento de cada requisito con rationale, criterio de aceptación medible, método de verificación y estado — esos cambios no alteran lo que el sistema debe hacer y no se contabilizan como modificaciones. Entre v1.0.0 y v1.1.0 sí hubo dos cambios sustantivos de enunciado sobre el corpus heredado: REQ-F-016 (cuestionario ligado al servicio y obligatorio al crear el pedido, en vez de envío manual posterior) y REQ-F-017 (catálogo de plantillas de contrato curado por Administrador, en vez de una plantilla global única); ambos están documentados con su motivo en `CHANGELOG-REQ.md` v1.1.0. Por separado, en v1.2.0 se incorporaron 11 requisitos nuevos (REQ-F-024 a REQ-F-031, REQ-NF-015 a REQ-NF-017) que no son alcance nuevo del sistema, sino especificación de funcionalidad que ya estaba implementada y probada — ver §3.1, §4.1 y `CHANGELOG-REQ.md` v1.2.0. La tasa de estabilidad de 94,6% se calcula solo sobre el corpus heredado (denominador 37), porque mide cuánto cambió el *enunciado* de lo ya especificado; los 11 requisitos nuevos se reportan aparte como tasa de adición, no como inestabilidad, porque documentan alcance que nunca había sido especificado, no un enunciado que cambió de significado.

Conviene leer estas cifras con cautela metodológica: que el corpus heredado casi no cambiara de enunciado (94,6% de estabilidad) mientras el corpus total creció 29,7% en la misma entrega es coherente con un proyecto académico de alcance cerrado, donde la especificación original se congeló temprano y la brecha entre "lo que se construyó" y "lo que se documentó" se cerró al final, no durante el desarrollo. En un proyecto con stakeholders externos, una tasa de adición de esta magnitud al cierre de una entrega sería una señal de alarma sobre el proceso de especificación continua, no solo un ajuste de documentación. La limitación se declara en el capítulo de amenazas a la validez del documento académico.

---

## 8. Aprobación

Este SRS se somete a la revisión y aprobación del docente-director del PFC, conforme al apartado A.3.1 de la guía de la Entrega Final.

| Rol                        | Nombre                                     | Fecha | Firma |
| -------------------------- | ------------------------------------------ | ----- | ----- |
| Docente-director del PFC   | Dr. Gleiston Cicerón Guerrero Ulloa, Ph.D. |       |       |
| Representante del equipo   |                                            |       |       |

**Estado de la aprobación: pendiente de firma.** La firma depende de la disponibilidad de un tercero externo al equipo (el docente-director) y no puede completarse unilateralmente antes de la entrega. Dado que la Entrega Final se presenta durante la semana del examen final (semana 19, 7–11 de septiembre de 2026), la revisión y, de proceder, la formalización de esta firma se realizarán presencialmente **el día del examen**, que es la primera instancia en que ambas partes coinciden. Hasta que esta sección lleve la firma del docente-director, el criterio D0R no puede superar el nivel *En desarrollo*, según la regla transversal 9 de la guía. La versión aprobada y firmada, cuando exista, se archiva como `docs/requisitos/SRS-v1.2.0.pdf`; las versiones anteriores (incluida v1.0.0) se conservan en `docs/requisitos/historico/`.

---

