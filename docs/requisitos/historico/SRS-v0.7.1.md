# Especificación de Requisitos de Software (SRS)
## Artisync — Plataforma web de comisiones y venta de contenido digital para creadores

**Conforme a:** ISO/IEC/IEEE 29148:2018 (estructura SRS) · INCOSE Guide to Writing Requirements v4 (calidad de requisitos C1–C15)
**Versión:** v0.9.0-rc — Tercera Entrega
**Fecha:** 24 de julio de 2026
**Precede a:** v1.0.0 (Entrega Final)
**Actualiza a:** SRS de la Entrega 1A (v0.3.0)

> Nota de mantenimiento: cada cambio sustantivo respecto a la versión 1A se registra en `docs/requisitos/CHANGELOG-REQ.md`. Los identificadores `REQ-F-NNN` / `REQ-NF-NNN` reemplazan a los códigos `RF-NN` / `RNF-NN` de la Entrega 1A; la tabla de equivalencia está en la sección 6.

---

## 1. Introducción

### 1.1 Propósito
Este documento especifica, de manera completa y verificable, los requisitos funcionales y no funcionales del sistema Artisync en su estado de release candidate (v0.9.0-rc). Sirve como fuente única de verdad para la trazabilidad hacia el código, las pruebas automatizadas y la evidencia empírica exigida en la Tercera Entrega del PFC.

### 1.2 Alcance
Artisync es una plataforma web que centraliza la comercialización de servicios y productos digitales ofrecidos por profesionales creativos (ilustradores, músicos, desarrolladores, diseñadores, etc.). El sistema conecta a **Creadores** (vendedores) con **Clientes**, gestionando perfiles, catálogo, mensajería, contratos con firma electrónica, flujo de pedidos, pagos vía PayPal Orders v2 con patrón *escrow*, y funciones sociales (seguidores, comentarios, sorteos). El desarrollo se enmarca en un proyecto académico de 17 semanas; algunas capacidades (firma legalmente vinculante, logística física, recomendaciones avanzadas) quedan fuera de alcance y se documentan como trabajo futuro.

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
| Rol | Descripción |
|---|---|
| Administrador | Supervisa la plataforma, gestiona cuentas, categorías, publicaciones, contenido reportado y transacciones. |
| Creador de contenido | Publica servicios/productos, gestiona perfil y portafolio, atiende pedidos, organiza sorteos. |
| Cliente registrado | Contrata servicios, compra productos, sigue creadores, participa en sorteos. |
| Visitante anónimo | Explora contenido público sin autenticarse. |

### 2.4 Restricciones
Proyecto académico de 17 semanas; equipo reducido; hosting local durante el desarrollo; integración de pagos limitada al entorno sandbox de PayPal; alcance geográfico inicial: Ecuador (validación de mayoría de edad, moneda USD).

### 2.5 Supuestos y dependencias
Se asume disponibilidad continua de las APIs externas (PayPal sandbox, servicio de IA, almacenamiento S3-compatible) durante las pruebas. La arquitectura se diseña contemplando extensión futura (multi-idioma, multi-moneda) sin comprometerlas en esta entrega.

---

## 3. Requisitos específicos — Funcionales

Cada requisito seguido de: **Rationale**, **Prioridad (MoSCoW)**, **Criterio de aceptación**, **Verificación** y **Estado** en v0.9.0-rc. Los 23 requisitos provienen del corpus de la Entrega 1A y se mantienen con trazabilidad completa (ver tabla de equivalencia en §6).

### Módulo Seguridad y Control de Acceso

**REQ-F-001** (ex RF-01) — El sistema debe permitir el registro de nuevos usuarios con selección de rol (Creador o Cliente); el rol determina las vistas y acciones disponibles durante toda la sesión.
- Rationale: separación de responsabilidades es la base del modelo de negocio de doble lado (oferta/demanda).
- Prioridad: Must
- Aceptación: un Creador no accede a rutas de Cliente y viceversa; el sistema redirige ante acceso no autorizado.
- Verificación: Test (JUnit + MockMvc sobre `AuthController`/`UserController`)
- Estado: implementado

**REQ-F-002** (ex RF-02) — Control de acceso basado en roles (RBAC): cada acción debe estar asociada a un permiso específico asignado al rol.
- Rationale: exigido por RNF de seguridad y por la separación de privilegios del dominio.
- Prioridad: Must
- Aceptación: asignar/revocar un permiso cambia el acceso sin reiniciar sesión (revocación efectiva en la siguiente solicitud).
- Verificación: Test (`RolePermissionControllerTest`)
- Estado: implementado

**REQ-F-003** (ex RF-03) — Gestión de sesiones mediante JWT con expiración de 24 horas; rutas protegidas exigen token válido en la cabecera de autorización.
- Rationale: autenticación *stateless* escalable sin sesión en servidor.
- Prioridad: Must
- Aceptación: token válido → 200; token expirado → 401 "Token expirado"; sin cabecera → 401 "Autenticación requerida"; token de sesión cerrada no reutilizable.
- Verificación: Test (`JwtAuthenticationFilterTest`, `AuthServiceImplTest`)
- Estado: implementado

**REQ-F-004** (ex RF-04) — Recuperación de contraseña mediante enlace de un solo uso, válido 60 minutos, enviado por correo.
- Prioridad: Must
- Aceptación: enlace usado o expirado → mensaje de invalidez; tras el flujo, login inmediato con nueva contraseña.
- Verificación: Test unitario + prueba manual de flujo de correo
- Estado: implementado

**REQ-F-005** (ex RF-05) — 2FA opcional basada en TOTP (RFC 6238), disponible solo para usuarios con identidad verificada.
- Prioridad: Should
- Aceptación: código incorrecto/expirado → "Código inválido o expirado"; usuario no verificado no ve la opción.
- Verificación: Test (`TwoFactorServiceImplTest`, `TwoFactorController`)
- Estado: implementado

### Módulo Perfiles, Verificación y Portafolio

**REQ-F-006** (ex RF-06) — Verificación de mayoría de edad del Creador mediante documento de identidad analizado por servicio externo; el documento se elimina del almacenamiento tras la respuesta.
- Prioridad: Must
- Aceptación: aprobado → estado verificado en ≤60s y notificación; minoría de edad → estado sin cambios y mensaje de rechazo; documento no accesible tras respuesta.
- Verificación: Test + inspección de almacenamiento
- Estado: pendiente (requiere integración real con servicio de IA — ver riesgo en §6)

**REQ-F-007** (ex RF-07) — Verificación de certificados profesionales por IA; puntaje ≥ umbral configurable (0.75 por defecto) habilita sello de verificación.
- Prioridad: Should
- Aceptación: puntaje ≥ umbral → sello inmediato; puntaje menor → sin sello + notificación; umbral configurable sin cambio de código.
- Verificación: Test + demostración
- Estado: pendiente

**REQ-F-008** (ex RF-08) — Personalización de perfil público: foto (JPG/PNG ≤5MB), biografía (≤500 caracteres, sin teléfono/correo), hasta 3 URLs de redes sociales.
- Prioridad: Must
- Aceptación: biografía con contacto directo → rechazo; imagen >5MB → rechazo; URL válida se muestra como enlace.
- Verificación: Test (`PerfilCreadorControlador`)
- Estado: implementado

**REQ-F-009** (ex RF-09) — El perfil público muestra seguidores, servicios activos, calificación promedio y estado de verificación; cualquier usuario autenticado puede seguir/dejar de seguir.
- Prioridad: Must
- Aceptación: contador se actualiza de inmediato al seguir/dejar de seguir; usuario no autenticado es redirigido a login.
- Verificación: Test + demostración
- Estado: pendiente (módulo social sin controlador expuesto aún)

**REQ-F-010** (ex RF-10) — Comentarios en ítems de portafolio; el Creador puede eliminarlos (borrado lógico, no visibles en vista pública, consultables por el administrador).
- Prioridad: Should
- Verificación: Test
- Estado: pendiente (módulo social sin controlador expuesto aún)

### Módulo Catálogo Dinámico de Servicios

**REQ-F-011** (ex RF-11) — Publicación de ítems tipo Producto o Servicio, con precio (≥0.01 USD), al menos una imagen (≤10MB) y descripción (20–2000 caracteres) obligatorios.
- Prioridad: Must
- Verificación: Test (`ServicioControlador`)
- Estado: implementado

**REQ-F-012** (ex RF-12) — Hasta 10 atributos personalizados por ítem; formularios adaptados dinámicamente a la categoría del Creador.
- Prioridad: Must
- Verificación: Test
- Estado: implementado

**REQ-F-013** (ex RF-13) — Motor de búsqueda con filtros por categoría, subcategoría, rango de precio y etiquetas, más búsqueda textual sobre título/descripción; edición de ítems en cualquier momento.
- Prioridad: Must
- Verificación: Test (Specification API — `specification/catalogo`)
- Estado: implementado

### Módulo Comunicación y Notificaciones

**REQ-F-014** (ex RF-14) — Mensajería interna en tiempo real vía WebSocket; sala de chat creada automáticamente al firmar el contrato; se cierra al llegar a Entregado o Cancelado.
- Prioridad: Must
- Verificación: Test + prueba de carga WebSocket (ver REQ-NF-005)
- Estado: pendiente (Módulo 6 no implementado — ver `implementation_plan.md`)

**REQ-F-015** (ex RF-15) — Análisis de contenido de mensajes para detectar teléfonos/correos; bloqueo de entrega y aviso; suspensión de 15 días tras 3 infracciones en 30 días.
- Prioridad: Must
- Verificación: Test
- Estado: pendiente

**REQ-F-016** (ex RF-16) — Formulario de briefing configurable (hasta 10 preguntas); respuestas no editables tras el envío.
- Prioridad: Must
- Verificación: Test
- Estado: pendiente

### Módulo Legal, Entregables y Finanzas

**REQ-F-017** (ex RF-17) — Generación automática de contrato HTML desde plantilla activa, sustituyendo variables (partes, servicio, precio, revisiones, fecha).
- Prioridad: Must
- Verificación: Test (`ContratoControlador`)
- Estado: implementado

**REQ-F-018** (ex RF-18) — Firma electrónica como acción explícita de cada parte; el pedido no avanza sin ambas firmas; PDF descargable con hashes de firma.
- Prioridad: Must
- Verificación: Test
- Estado: implementado

**REQ-F-019** (ex RF-19) — Flujo de trabajo del pedido por etapas configurables según categoría; cada transición registrada con marca de tiempo; vista de seguimiento en tiempo real para el Cliente.
- Prioridad: Must
- Verificación: Test (`FlujoTrabajoControlador`, `PedidoControlador`)
- Estado: implementado

**REQ-F-020** (ex RF-20) — Generación de enlace de pago vía PayPal Orders v2 al iniciar pedido; actualización de estado de fondos al recibir webhook confirmado.
- Prioridad: Must
- Verificación: Test (`PayPalWebhookControlador`, sandbox)
- Estado: implementado (webhook), pendiente validación end-to-end con sandbox real

**REQ-F-021** (ex RF-21) — Entrega con marca de agua para previsualización; aprobación del Cliente libera fondos y habilita descarga limpia; comisión de plataforma registrada automáticamente.
- Prioridad: Must
- Verificación: Test (`EntregableControlador`, `PagoControlador`)
- Estado: implementado

**REQ-F-022** (ex RF-22) — Cargo configurable por revisión adicional; ticket que supera el límite genera nuevo enlace de pago; rechazo automático tras 48h sin pago.
- Prioridad: Should
- Verificación: Test (`TicketRevisionControlador`)
- Estado: implementado

### Módulo Social, Comunidad y Sorteos

**REQ-F-023** (ex RF-23) — Creación de sorteos (título, premio, ganadores, fechas, requisito de seguidor); selección aleatoria automática de ganadores al cierre.
- Prioridad: Could
- Verificación: Test + demostración
- Estado: pendiente (módulo social sin controlador expuesto aún)

---

## 4. Requisitos específicos — No funcionales

**REQ-NF-001** (ex RNF-01) — Seguridad/Transporte: redirección forzada a HTTPS (301); rechazo de TLS <1.2; TLS 1.3 preferente.
- Prioridad: Must · Verificación: análisis (SSL Labs) · Estado: pendiente (despliegue local aún sin TLS terminado)

**REQ-NF-002** (ex RNF-02) — Contraseñas con hash bcrypt, factor de coste ≥10; nunca texto plano.
- Prioridad: Must · Verificación: inspección de BD · Estado: implementado

**REQ-NF-003** (ex RNF-03) — JWT firmado HS256 con clave ≥256 bits en variable de entorno (nunca en código/repositorio); rechazo de firma inválida con 401.
- Prioridad: Must · Verificación: análisis + test · Estado: implementado (clave vía `.env`; **pendiente**: añadir claims `iss`, `aud`, `jti` para conformidad total con A.1 de esta guía)

**REQ-NF-004** (ex RNF-04) — LCP del catálogo ≤2s bajo 4G simulada con ≥20 servicios publicados.
- Prioridad: Should · Verificación: Lighthouse · Estado: pendiente (requiere frontend)

**REQ-NF-005** (ex RNF-05) — WebSocket: ≥10 conexiones simultáneas sin degradación; latencia extremo-a-extremo ≤500ms en red local.
- Prioridad: Should · Verificación: script de carga (ws/wscat) · Estado: pendiente (módulo de chat no implementado)

**REQ-NF-006** (ex RNF-06) — Generación de contrato PDF ≤5s bajo carga normal.
- Prioridad: Should · Verificación: timestamps de log, 5 mediciones · Estado: pendiente de medición formal

**REQ-NF-007** (ex RNF-07) — Interfaz sin desbordamiento horizontal en 360/768/1440px; controles operables táctilmente (≥44px).
- Prioridad: Should · Verificación: DevTools · Estado: pendiente (requiere frontend)

**REQ-NF-008** (ex RNF-08) — Formularios de catálogo dinámicos sin recarga; flujo de contratación en ≤5 pantallas.
- Prioridad: Should · Verificación: prueba manual · Estado: pendiente (requiere frontend)

**REQ-NF-009** (ex RNF-09) — Disponibilidad durante semanas de evaluación 16–17; reinicio automático ante fallos.
- Prioridad: Must · Verificación: demostración (ps aux, healthcheck Docker) · Estado: parcial (healthcheck en `docker-compose.yml`, falta gestor de reinicio explícito documentado)

**REQ-NF-010** (ex RNF-10) — Módulos WebSocket, REST y generación de PDF desacoplados (sin imports cruzados directos).
- Prioridad: Should · Verificación: inspección de dependencias · Estado: implementado (paquetes separados por módulo)

**REQ-NF-011** (ex RNF-11) — Archivos binarios en almacenamiento externo compatible con S3; sin archivos locales en el servidor.
- Prioridad: Must · Verificación: inspección de URLs · Estado: pendiente (no se encontró integración S3/R2 en el código revisado)

**REQ-NF-012** (ex RNF-12) — Bloqueo de registro a menores de 18; checkbox obligatorio de términos y privacidad.
- Prioridad: Must · Verificación: test + inspección HTML · Estado: pendiente de verificación (requiere frontend + regla de negocio confirmada en backend)

**REQ-NF-013** (ex RNF-13) — Auditoría inmutable de transiciones de pedido y transacciones; exportación CSV por el administrador.
- Prioridad: Must · Verificación: test (DELETE/PATCH → 403) · Estado: parcial (historial existe; falta confirmar bloqueo explícito de edición/borrado y exportación CSV)

**REQ-NF-014** (ex RNF-14) — Integración exclusiva con PayPal Orders v2; credenciales en variables de entorno; verificación de firma de webhook.
- Prioridad: Must · Verificación: inspección de Git + simulación de webhook inválido · Estado: implementado (config PayPal vía `.env`; falta test explícito de firma inválida)

---

## 5. Matriz de trazabilidad (resumen)

Ver archivo completo en `docs/trazabilidad/matriz.csv`. Estructura de columnas: `id_requisito, tipo, prioridad_moscow, historia_usuario, caso_de_uso, modulo_codigo, endpoint_api, prueba_automatizada, tipo_acceso, evidencia_empirica, estado`.

## 6. Evolución de requisitos desde la Entrega 1A

| Cambio | Detalle |
|---|---|
| Renombrado de IDs | `RF-NN` → `REQ-F-0NN`, `RNF-NN` → `REQ-NF-0NN`, sin alterar el contenido semántico, para conformidad con ISO/IEC/IEEE 29148. |
| Adición de atributos | Se agregó `rationale`, `estado` (pendiente/implementado/verificado) y `método de verificación` explícito a cada requisito, ausentes en la tabla original de la Entrega 1A. |
| Hallazgo de brecha | REQ-F-009, 010, 014, 015, 016, 023 dependen de módulos (social, comunicación) sin controlador implementado aún — declarados `pendiente`. Ver ADR correspondiente y plan de cierre antes del 24 de julio. |
| Hallazgo de brecha | REQ-NF-001, 004, 007, 008, 011 dependen del frontend Angular, que a la fecha de este documento está en etapa inicial. |

Todo cambio adicional debe registrarse en `docs/requisitos/CHANGELOG-REQ.md` con fecha, autor, requisito afectado y motivo, siguiendo la convención Keep a Changelog.
