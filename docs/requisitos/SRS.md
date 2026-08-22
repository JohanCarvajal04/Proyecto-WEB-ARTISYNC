# Especificación de Requisitos de Software (SRS)

## Artisync — Plataforma web de comisiones y venta de contenido digital para creadores

**Conforme a:** ISO/IEC/IEEE 29148:2018 (estructura SRS) · INCOSE Guide to Writing Requirements v4 (calidad de requisitos C1–C15)
**Versión:** v1.0.0 — Entrega Final
**Fecha:** 2026-08-18
**Precede a:** (Versión final)
**Actualiza a:** SRS de la Tercera Entrega (v0.9.0-rc)

> Nota de mantenimiento: cada cambio sustantivo respecto a la versión 1A se registra en `docs/requisitos/CHANGELOG-REQ.md`. Los identificadores `REQ-F-NNN` / `REQ-NF-NNN` reemplazan a los códigos `RF-NN` / `RNF-NN` de la Entrega 1A; la tabla de equivalencia está en la sección 6.

> **Fuente de verdad del estado.** El campo `Estado` de cada requisito es un espejo de la columna `estado` de [`docs/trazabilidad/matriz.csv`](../trazabilidad/matriz.csv), que es la fuente autoritativa porque es la única que exige, en la misma fila, el módulo, el endpoint, la prueba automatizada y la evidencia que sostienen ese estado. Este documento es la fuente de verdad del **enunciado**, la **prioridad MoSCoW** y el **criterio de aceptación**.
>
> Los valores admitidos son exactamente `pendiente`, `implementado` y `verificado`; `verificado` exige una prueba automatizada que lo respalde. `scripts/validate-traceability.sh` falla si ambos documentos divergen, si un requisito queda por debajo del estado que exige su prioridad sin figurar en [`docs/trazabilidad/excepciones-estado.txt`](../trazabilidad/excepciones-estado.txt), o si un `Must` verificado no declara prueba. Ambos archivos se actualizan en el mismo commit.

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica, de manera completa y verificable, los requisitos funcionales y no funcionales del sistema Artisync en su versión estable **v1.0.0**. Sirve como fuente única de verdad para la trazabilidad hacia el código, las pruebas automatizadas y la evidencia empírica exigida en la Entrega Final del PFC.

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

ISO/IEC/IEEE 29148:2018; INCOSE Guide to Writing Requirements v4; RFC 7519 (JWT); RFC 7807 (Problem Details); RFC 6238 (TOTP); ISO/IEC 25010:2011 (calidad de software); OWASP Top 10:2021; documento interno `Entrega 1A.docx` (corpus original de requisitos, semana del 4 de junio de 2026).

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

Cada requisito seguido de: **Rationale**, **Prioridad (MoSCoW)**, **Criterio de aceptación**, **Verificación** y **Estado** en v1.0.0. Los 23 requisitos provienen del corpus de la Entrega 1A y se mantienen con trazabilidad completa (ver tabla de equivalencia en §6).

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
- Aceptación: token válido → 200; token expirado → 401 "Token expirado"; sin cabecera → 401 "Autenticación requerida"; token de sesión cerrada no reutilizable.
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
- Estado: pendiente

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

**REQ-F-016** (ex RF-16) — Formulario de briefing configurable (hasta 10 preguntas); respuestas no editables tras el envío.

- Prioridad: Must
- Verificación: Test
- Estado: verificado

### Módulo Legal, Entregables y Finanzas

**REQ-F-017** (ex RF-17) — Generación automática de contrato HTML desde plantilla activa, sustituyendo variables (partes, servicio, precio, revisiones, fecha).

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
- Verificación: Test (`EntregableControlador`, `PagoControlador`)
- Estado: verificado

**REQ-F-022** (ex RF-22) — Cargo configurable por revisión adicional; ticket que supera el límite genera nuevo enlace de pago; rechazo automático tras 48h sin pago.

- Prioridad: Should
- Verificación: Test (`TicketRevisionControlador`)
- Estado: verificado

### Módulo Social, Comunidad y Sorteos

**REQ-F-023** (ex RF-23) — Creación de sorteos (título, premio, ganadores, fechas, requisito de seguidor); selección aleatoria automática de ganadores al cierre.

- Prioridad: Could
- Verificación: Test + demostración
- Estado: verificado

---

## 4. Requisitos específicos — No funcionales

**REQ-NF-001** (ex RNF-01) — Seguridad/Transporte: redirección forzada a HTTPS (301); rechazo de TLS <1.2; TLS 1.3 preferente.

- Prioridad: Must · Verificación: análisis (SSL Labs) · Estado: implementado (configuración acreditada en `docs/mediciones/sec/a02-tls.txt`; el análisis externo con SSL Labs exige el despliegue público del Bloque A.4, todavía pendiente — excepción declarada en `docs/trazabilidad/excepciones-estado.txt`)

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

- Prioridad: Must · Verificación: demostración (ps aux, healthcheck Docker) · Estado: implementado (los cinco servicios de `artisync/docker-compose.yml` declaran `restart: unless-stopped` y healthcheck; falta archivar una demostración de caída y recuperación para elevarlo a verificado — excepción declarada en `docs/trazabilidad/excepciones-estado.txt`)

**REQ-NF-010** (ex RNF-10) — Módulos WebSocket, REST y generación de PDF desacoplados (sin imports cruzados directos).

- Prioridad: Should · Verificación: inspección de dependencias · Estado: implementado (paquetes separados por módulo)

**REQ-NF-011** (ex RNF-11) — Archivos binarios en almacenamiento externo compatible con S3; sin archivos locales en el servidor.

- Prioridad: Must · Verificación: inspección de URLs · Estado: verificado (Azure Blob Storage implementado)

**REQ-NF-012** (ex RNF-12) — Bloqueo de registro a menores de 18; checkbox obligatorio de términos y privacidad.

- Prioridad: Must · Verificación: test + inspección HTML · Estado: verificado

**REQ-NF-013** (ex RNF-13) — Auditoría inmutable de transiciones de pedido y transacciones; exportación CSV por el administrador.

- Prioridad: Must · Verificación: test (UPDATE/DELETE/TRUNCATE → error de base de datos) · Estado: verificado. Además de `historial_estados_pedido` (dominio) y el exportador de transacciones (`AuditControlador`), existe desde V12\_\_modulo_auditoria.sql una bitácora transversal `auditoria_eventos` con trigger PL/pgSQL que bloquea UPDATE/DELETE/TRUNCATE (SQLState 42501) y GRANT restringido a `SELECT, INSERT` para la cuenta de aplicación, alimentada por un aspecto AOP (`@Auditable`) sobre los 7 módulos, expuesta en `/api/v1/admin/auditoria` con listado filtrado, detalle y exportación CSV. Verificado con `EventoAuditoriaInmutabilidadIT` contra PostgreSQL real.

**REQ-NF-014** (ex RNF-14) — Integración exclusiva con PayPal Orders v2; credenciales en variables de entorno; verificación de firma de webhook.

- Prioridad: Must · Verificación: inspección de Git + simulación de webhook inválido · Estado: verificado (credenciales de PayPal vía `.env`; `PagoServicioImplWebhookTest` cubre firma inválida, cabeceras ausentes, payload ilegible y ausencia de `webhook-id`, además del camino feliz)

---

## 5. Matriz de trazabilidad (resumen)

Ver archivo completo en `docs/trazabilidad/matriz.csv`. Estructura de columnas: `id_requisito, tipo, prioridad_moscow, historia_usuario, caso_de_uso, modulo_codigo, endpoint_api, prueba_automatizada, tipo_acceso, evidencia_empirica, estado`.

## 6. Evolución de requisitos desde la Entrega 1A

| Cambio                | Detalle                                                                                                                                                                    |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Renombrado de IDs     | `RF-NN` → `REQ-F-0NN`, `RNF-NN` → `REQ-NF-0NN`, sin alterar el contenido semántico, para conformidad con ISO/IEC/IEEE 29148.                                               |
| Adición de atributos  | Se agregó `rationale`, `estado` (pendiente/implementado/verificado) y `método de verificación` explícito a cada requisito, ausentes en la tabla original de la Entrega 1A. |
| Resolución de brechas | REQ-F-009, 014, 015, 016, 023 (módulos social, comunicación, WebSockets) implementados y marcados como `verificado` para v1.0.0. **REQ-F-010 no**: sigue en `pendiente`, con la excepción declarada en `docs/trazabilidad/excepciones-estado.txt`. |
| Resolución de brechas | REQ-NF-004, 007, 008, 011 resueltos con el frontend Angular 22 finalizado y la integración de Azure Blob Storage.                                                          |
| Módulo de Auditoría   | REQ-NF-013 implementado formalmente mediante la migración V12, funciones PL/pgSQL, y aspecto AOP (`@Auditable`).                                                           |

Todo cambio adicional debe registrarse en `docs/requisitos/CHANGELOG-REQ.md` con fecha, autor, requisito afectado y motivo, siguiendo la convención Keep a Changelog.

---

## 7. Métricas de calidad del corpus de requisitos

Todas las cifras se derivan de `docs/trazabilidad/matriz.csv` en la fecha de este documento y son reproducibles ejecutando `scripts/validate-traceability.sh`, que además impide que estas métricas se desincronicen del SRS.

### 7.1 Volumen y distribución

| Métrica                            | Valor                                                             |
| ---------------------------------- | ----------------------------------------------------------------- |
| Total de requisitos                | 37                                                                |
| Por tipo                           | 23 funcionales (62,2 %) · 14 no funcionales (37,8 %)              |
| Por prioridad MoSCoW               | 26 Must (70,3 %) · 10 Should (27,0 %) · 1 Could (2,7 %)           |
| Por estrategia de acceso a datos   | 24 CRUD-ORM · 8 SP · 5 sin acceso a datos (frontend/arquitectura) |

### 7.2 Estado de verificación

| Estado         | Requisitos | Porcentaje |
| -------------- | ---------- | ---------- |
| `verificado`   | 28         | 75,7 %     |
| `implementado` | 6          | 16,2 %     |
| `pendiente`    | 3          | 8,1 %      |

Desglose por prioridad, que es lo que evalúa el criterio D0R:

| Prioridad | Verificado | Implementado | Pendiente | Cumple el mínimo exigido           |
| --------- | ---------- | ------------ | --------- | ---------------------------------- |
| Must      | 24 (92,3 %) | 2            | 0         | 24 de 26; 2 con excepción declarada |
| Should    | 3          | 6            | 1         | 9 de 10; 1 con excepción declarada  |
| Could     | 1          | 0            | 0         | Sin mínimo exigible                 |

Los tres requisitos que no alcanzan el estado que su prioridad exige están declarados uno a uno, con su motivo y su condición de cierre, en [`docs/trazabilidad/excepciones-estado.txt`](../trazabilidad/excepciones-estado.txt): REQ-NF-001 y REQ-NF-009 dependen del despliegue público (Bloque A.4), y REQ-F-010 se dejó deliberadamente fuera de v1.0.0.

### 7.3 Cobertura de trazabilidad

| Métrica                                     | Valor            |
| ------------------------------------------- | ---------------- |
| Requisitos presentes en la matriz           | 37 / 37 (100 %)  |
| Requisitos con prueba automatizada asociada | 30 (81,1 %)      |
| Requisitos Must con prueba automatizada     | 24 / 26 (92,3 %) |
| Requisitos con evidencia empírica archivada | 23 (62,2 %)      |

Ningún requisito figura como `verificado` sin una prueba automatizada que lo respalde: es una regla que el validador impone y que hace fallar el pipeline si se incumple.

### 7.4 Estabilidad de requisitos

La tasa de estabilidad se calcula como `1 − (requisitos modificados / requisitos totales)` entre la Entrega 1A y la Entrega Final, tomando como modificación cualquier cambio de **enunciado, prioridad o alcance** registrado en `CHANGELOG-REQ.md`. No cuentan los cambios de estado, que reflejan el avance de la implementación y no inestabilidad de la especificación.

| Métrica                            | Valor                                     |
| ---------------------------------- | ----------------------------------------- |
| Requisitos en la Entrega 1A        | 37 (RF-01 a RF-23 · RNF-01 a RNF-14)      |
| Requisitos en v1.0.0               | 37 (REQ-F-001 a REQ-F-023 · REQ-NF-001 a REQ-NF-014) |
| Añadidos                           | 0                                         |
| Eliminados                         | 0                                         |
| Modificados en enunciado o alcance | 0                                         |
| **Tasa de estabilidad**            | **1 − 0/37 = 1,000 (100 %)**              |
| Tasa de adición                    | 0 %                                       |
| Tasa de eliminación                | 0 %                                       |

El corpus ha permanecido **estable en volumen y alcance** desde la Entrega 1A: los mismos 37 requisitos, con correspondencia uno a uno de identificadores. Lo que cambió entre 1A y v1.0.0 fue la *forma* de la especificación, no su contenido: la renumeración de `RF-NN`/`RNF-NN` a `REQ-F-NNN`/`REQ-NF-NNN` para conformidad con ISO/IEC/IEEE 29148, y el enriquecimiento de cada requisito con rationale, criterio de aceptación medible, método de verificación y estado. Ninguno de esos cambios altera lo que el sistema debe hacer, y por eso no se contabilizan como modificaciones.

Conviene leer esta tasa con cautela metodológica: una estabilidad del 100 % es coherente con un proyecto académico de alcance cerrado y calendario corto, donde el corpus se congeló temprano y no hubo negociación posterior con un cliente real. En un proyecto con stakeholders externos, una cifra así sería más probable indicio de un registro de cambios incompleto que de una especificación perfecta. La limitación se declara en el capítulo de amenazas a la validez del documento académico.

---

## 8. Aprobación

Este SRS se somete a la revisión y aprobación del docente-director del PFC, conforme al apartado A.3.1 de la guía de la Entrega Final.

| Rol                        | Nombre                                     | Fecha | Firma |
| -------------------------- | ------------------------------------------ | ----- | ----- |
| Docente-director del PFC   | Dr. Gleiston Cicerón Guerrero Ulloa, Ph.D. |       |       |
| Representante del equipo   |                                            |       |       |

**Estado de la aprobación: pendiente de firma.** La versión aprobada y firmada se archiva como `docs/requisitos/SRS-v1.0.0.pdf`; las versiones anteriores se conservan en `docs/requisitos/historico/`. Mientras esta sección no lleve la firma del docente-director, el criterio D0R no puede superar el nivel *En desarrollo*, según la regla transversal 9 de la guía.
