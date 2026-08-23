# v1.0.0 — Entrega Final: primera versión estable de producción

**Fecha:** 2026-08-17
**Tag:** `v1.0.0` (commit `d07656b`)
**Comparar:** [`v0.9.0-rc...v1.0.0`](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/compare/v0.9.0-rc...v1.0.0)

ARTISYNC alcanza su primera versión estable de producción tras la Tercera Entrega (`v0.9.0-rc`). Este release cierra los módulos funcionales restantes (paneles de Creador y Admin, comunicación en tiempo real, verificación de identidad asistida por IA, catálogo público y flujo de pedidos) y consolida seguridad, almacenamiento en la nube y evidencia de calidad para la Entrega Final.

## 🎨 Panel del Creador
Overview, servicios (alta/edición, listado, detalle de comisión), portafolio y sus obras, sorteos (gestión y ganadores), reseñas recibidas, bandeja de briefings, perfil público y certificados de verificación.

## 🛡️ Panel de Administración / Moderación
Gestión de usuarios, revisión de verificaciones, moderación de portafolios y obras, infracciones y levantamiento de suspensiones, asignación de flujo de trabajo por categoría, notificaciones administrativas.

## 🤖 Verificación de identidad asistida por IA
- `VerificacionServicioImpl`: alta en `PENDIENTE`, análisis con IA sin autoescritura de estado, único punto de decisión (`registrarDecision`).
- Endpoints REST (`/api/v1/verificaciones`): cola, detalle con chequeo de pertenencia, descarga de documento.
- Proveedores IA intercambiables (Strategy): `NvidiaIaService` (modo estricto), `GeminiIaService`, `MockIaService` para desarrollo sin API key; manejo de errores `503 ProblemDetail`.
- Expiración automática de solicitudes `PENDIENTE` tras 30 días; corrección de los 7 hallazgos de la revisión final asistida por IA.
- Ajuste final: la subida de documentos de verificación ahora pasa el prefijo `VERIFICACION` al guardar, para que el router de almacenamiento los envíe al volumen local en vez de a Azure.

## 💬 Comunicación en tiempo real
Chat de pedido y briefing sobre WebSockets (STOMP/SockJS), centro de notificaciones con contador de no leídas, ruteo del módulo y modelos de dominio; ajustes finales en `ChatControlador` y en la creación de sala de chat desde `ContratoServicioImpl`/`EntregableServicioImpl`, con cobertura de pruebas nueva.

## 🛒 Catálogo, pedidos y componente social
Catálogo público con filtros y exploración; creación de pedido desde el catálogo; administración de flujos de trabajo por categoría; sorteos abiertos a participación; reseñas del cliente; corrección de autorización de categorías (por permiso, no por rol) y de resolución del flujo por categoría del servicio.

## 📄 Legal, pagos y entregables
Descarga real de entregables con validación de archivos en cliente; checkout de pago corregido contra el webhook; verificación de firma e idempotencia del webhook de PayPal.

## 🔐 Seguridad
2FA vinculado a la contraseña (ticket pre-auth, `jti` en sesiones, cuotas por cuenta, `429 ProblemDetail`); validación de `iss`/`aud`/reloj en JWT y bloqueo de cuentas deshabilitadas; restricción de `POST /api/v1/certificados` a `ADMIN`; guest guard por `homeRoute`; resolución de IP real del cliente sin exponer el backend; análisis estático SpotBugs + find-sec-bugs y escaneo ZAP baseline.

## ☁️ Almacenamiento e infraestructura
Implementación de Azure Blob Storage junto al almacenamiento local (Docker), con prefijos, extensiones y política por caso de uso ([ADR-007](adr/adr-007-almacenamiento-de-archivos.md)); servicio azurite (perfil `azure`) en `docker-compose`. Se agrega `AlmacenamientoRouter`, que decide por prefijo si un archivo va a Azure o al volumen local, con suite de pruebas propia (`AlmacenamientoRouterTest`, `AlmacenamientoCableadoTest`).

## 🚀 Despliegue
`render.yaml` (Blueprint de Render) con `artisync-backend` como Private Service en red interna, `artisync-frontend` como Web Service público y `artisync-redis`; `Dockerfile.render` + `nginx.render.conf` sirven el build de Angular en producción y siguen proxeando `/api/` al backend por red interna (mismo origen, sin CORS cross-origin). El backend no se expone públicamente, igual que en `docker-compose.yml` (decisión de seguridad OBS-AUTO-05 / OWASP A07). La base de datos sigue siendo Azure Database for PostgreSQL ya provisionada.

## 👤 Perfil y validaciones
Solicitud de verificación de identidad desde el perfil del cliente; validación real de mayoría de edad en registro y alta de usuarios.

## 🗄️ Base de datos
Migraciones V6–V9 (estados de verificación, dictamen/decisión, flujo por categoría); función `fn_listar_cola_verificacion` y procedimiento `sp_registrar_decision_verificacion`; pruebas de integración sobre perfil `postgres-it`.

## 📊 Calidad y evidencia
Cobertura JaCoCo, mediciones SUS, Lighthouse CI (perfiles mobile/desktop, 3 corridas), colección Postman ampliada con casos de autorización y validación, documentación de trazabilidad y ADRs sincronizada.

---

**Changelog completo:** ver [`CHANGELOG.md`](../CHANGELOG.md).
**Historial completo de commits:** [`v0.9.0-rc...v1.0.0`](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/compare/v0.9.0-rc...v1.0.0) (181 commits).
