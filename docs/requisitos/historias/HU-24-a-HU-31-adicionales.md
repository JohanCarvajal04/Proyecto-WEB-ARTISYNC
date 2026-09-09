# Historias de Usuario — Requisitos adicionales (post v1.0.0)

Estas diez historias corresponden a REQ-F-024 a REQ-F-031 (incorporados en v1.1.2) y REQ-F-032/REQ-F-033 (incorporados en v1.3.0), tras auditar el SRS contra código ya implementado y probado. Ver `docs/requisitos/SRS.md` §3.1 y `CHANGELOG-REQ.md`.

---

## HU-24 — Retirar mi saldo disponible
**Trazabilidad:** REQ-F-024
**Prueba de aceptación:** `SolicitudRetiroServicioImplTest`, `SolicitudRetiroServicioImplPayoutTest`

**As a** Creador,
**I want** configurar mi correo de PayPal y solicitar el retiro de mi saldo disponible,
**so that** pueda recibir fuera de la plataforma el dinero que ya gané.

**INVEST:** Independiente del resto del ciclo de pedido, solo depende de tener saldo liberado por REQ-F-021; negociable en el monto mínimo exacto; valiosa porque es el único modo de que el dinero salga de la plataforma; estimable y pequeña (saldo, solicitud, cola de aprobación); testable mediante los rechazos de monto mínimo, saldo insuficiente y solicitud duplicada.

```gherkin
Escenario: Rechazo por monto menor al mínimo configurado
  Given que el monto mínimo de retiro es USD 10.00
  When el Creador solicita un retiro de USD 5.00
  Then el sistema rechaza la solicitud sin crear ningún registro
```

---

## HU-25 — Supervisar los fondos en garantía (escrow)
**Trazabilidad:** REQ-F-025
**Prueba de aceptación:** `PagoGarantiaAuditoriaServicioImplTest`

**As a** Auditor Financiero,
**I want** listar y filtrar los pagos en garantía y ver un resumen agregado por estado,
**so that** pueda detectar fondos retenidos por más tiempo del esperado o en un estado anómalo.

**INVEST:** Independiente, es una vista de solo lectura sobre datos que ya existen; negociable en los filtros exactos; valiosa porque es el único control sobre dinero de clientes retenido por la plataforma; estimable y pequeña (listar, detalle, resumen); testable mediante el cálculo del resumen agregado.

```gherkin
Escenario: Resumen agregado por estado
  Given que existen pagos en garantía en distintos estados
  When el Auditor Financiero solicita el resumen
  Then el sistema devuelve la cantidad y el monto total agrupado por cada estado
```

---

## HU-26 — Exportar reportes administrativos
**Trazabilidad:** REQ-F-026
**Prueba de aceptación:** `ReporteFinancieroServicioImplTest`, `ReporteContratoServicioImplTest`

**As a** Administrador,
**I want** exportar el reporte de comisiones por creador y el listado de contratos formalizados en CSV, XLSX o PDF,
**so that** pueda auditar y compartir esa información fuera del sistema.

**INVEST:** Independiente de la generación de los datos subyacentes; negociable en los formatos soportados; valiosa para auditoría externa y contabilidad; estimable y pequeña (consultar + exportar); testable mediante el rechazo mediante tope de filas.

```gherkin
Escenario: Rechazo por exceder el tope de filas del formato
  Given que el reporte solicitado excede el tope de filas admitido por el formato
  When el Administrador solicita la exportación
  Then el sistema rechaza la exportación con un error de regla de negocio
```

---

## HU-27 — Recibir y gestionar mis notificaciones
**Trazabilidad:** REQ-F-027
**Prueba de aceptación:** `NotificacionServiceImplTest`, `NotificacionControladorTest`

**As a** usuario autenticado (cualquier rol),
**I want** ver mis notificaciones, marcarlas como leídas y ver cuántas tengo sin leer,
**so that** no pierda eventos importantes de otros módulos (pedidos, pagos, verificación).

**INVEST:** Independiente porque no depende de qué módulo generó la notificación; negociable en el canal de entrega (solo in-app por ahora); valiosa como mecanismo transversal de aviso; estimable y pequeña (listar, marcar, contar); testable mediante el aislamiento por usuario.

```gherkin
Escenario: Una notificación ajena no puede marcarse como leída
  Given que una notificación pertenece a otro usuario
  When el usuario autenticado intenta marcarla como leída
  Then el sistema responde 404
```

---

## HU-28 — Gestionar y moderar el catálogo de categorías, subcategorías y etiquetas
**Trazabilidad:** REQ-F-028
**Prueba de aceptación:** `CategoriaServicioImplTest`, `EtiquetaServicioImplTest`, `ServicioCatalogoServicioImplTest`

**As a** Moderador/Administrador,
**I want** revisar las categorías creadas por autoservicio y moderar servicios ajenos quitándoles una subcategoría inapropiada,
**so that** el catálogo público se mantenga curado y sin abuso del autoservicio.

**INVEST:** Independiente de la publicación de servicios en sí (REQ-F-011); negociable en qué campos son editables tras la revisión; valiosa porque sin curación el catálogo público podría llenarse de categorías duplicadas o inapropiadas; estimable y pequeña (revisar, aprobar, quitar subcategoría); testable mediante el bloqueo de quitar la última subcategoría de un servicio.

```gherkin
Escenario: Una categoría autoservicio no aparece en el catálogo público hasta ser revisada
  Given que un Creador crea una categoría con CATEGORIA_CREAR
  When la categoría aún no ha sido marcada como revisada
  Then no aparece en el listado público de categorías activas
```

---

## HU-29 — Calificar y reseñar un pedido entregado
**Trazabilidad:** REQ-F-029
**Prueba de aceptación:** `ResenaServiceImplTest`

**As a** Cliente,
**I want** calificar de 1 a 5 estrellas y reseñar un pedido una vez que recibí el entregable,
**so that** pueda dejar constancia pública de mi experiencia con el Creador.

**INVEST:** Independiente del flujo de pedido en sí, solo depende de que el entregable ya fue liberado (REQ-F-021); negociable en el largo máximo del comentario; valiosa porque alimenta la calificación promedio que ya exhibe REQ-F-009; estimable y pequeña (crear, editar, eliminar, listar); testable mediante el bloqueo de reseña duplicada.

```gherkin
Escenario: No se puede reseñar un pedido sin entregable liberado
  Given que el entregable de un pedido aún no fue aprobado
  When el Cliente intenta crear una reseña sobre ese pedido
  Then el sistema rechaza la creación con un error de regla de negocio
```

---

## HU-30 — Administrar el ciclo de vida de las cuentas de usuario
**Trazabilidad:** REQ-F-030
**Prueba de aceptación:** `AdminUserServiceImplTest`

**As a** Administrador,
**I want** listar, crear, editar, activar/desactivar/suspender, asignar roles, revocar sesiones y eliminar lógicamente cuentas de usuario,
**so that** pueda gestionar el ciclo de vida completo de las cuentas sin depender de acceso directo a la base de datos.

**INVEST:** Independiente del control de acceso operativo por permiso (REQ-F-002); negociable en qué acciones requieren doble confirmación; valiosa porque es la única vía administrativa para gestionar cuentas; estimable y pequeña (una acción por endpoint); testable mediante las auto-protecciones del administrador sobre su propia cuenta.

```gherkin
Escenario: Un administrador no puede eliminar su propia cuenta
  Given que un Administrador autenticado intenta eliminar su propio usuario
  When se procesa la solicitud
  Then el sistema rechaza la operación con un error de regla de negocio
```

---

## HU-31 — Consultar el catálogo de países y dar "me gusta" en el portafolio
**Trazabilidad:** REQ-F-031
**Prueba de aceptación:** `PaisServiceImplTest`, `LikePortafolioServiceImplTest`

**As a** usuario del sistema,
**I want** elegir mi país desde un catálogo mantenido por el Administrador, y dar o quitar "me gusta" a un ítem de portafolio que me interese,
**so that** los formularios de perfil sean consistentes y pueda expresar interés en el trabajo de un Creador.

**INVEST:** Independiente, son dos capacidades menores sin dependencia entre sí; negociable en si el catálogo de países se amplía a validaciones adicionales (código telefónico, moneda); valiosa por consistencia de datos y por señal social de interés; estimable y pequeña; testable mediante el bloqueo de doble like.

```gherkin
Escenario: No se puede dar like dos veces al mismo ítem
  Given que un usuario ya dio like a un ítem de portafolio
  When el mismo usuario intenta darle like de nuevo
  Then el sistema rechaza la operación como recurso duplicado
```

---

## HU-32 — Gestionar las obras de mi portafolio
**Trazabilidad:** REQ-F-032
**Prueba de aceptación:** `PortafolioItemControladorTest`, `PortafolioItemControladorRutasTest`, `PortafolioItemServicioImplTest`

**As a** Creador,
**I want** subir, listar, actualizar y eliminar las obras de mi portafolio, hasta un máximo de 50,
**so that** pueda mostrar mi trabajo sin arriesgar el rendimiento del catálogo público ni exponer a otros usuarios a un archivo malicioso.

**INVEST:** Independiente del resto del perfil, solo depende de tener un portafolio creado; negociable en el límite exacto de 50 obras; valiosa porque el portafolio es la vitrina principal del Creador; estimable y pequeña (subir, listar, actualizar, eliminar, descargar); testable mediante el rechazo al superar el límite y el forzado de descarga.

```gherkin
Escenario: Rechazo al superar el máximo de obras del portafolio
  Given que un portafolio ya tiene 50 obras
  When su dueño intenta subir una obra número 51
  Then el sistema rechaza la operación sin crear el nuevo ítem
```

---

## HU-33 — Administrar infracciones y suspensiones
**Trazabilidad:** REQ-F-033
**Prueba de aceptación:** `InfraccionServiceImplTest` (cubre `historialPorUsuario`; falta prueba para `listarInfracciones` y `revertirSuspension`)

**As a** Administrador,
**I want** listar todas las infracciones registradas, consultar el historial de un usuario y revertir manualmente una suspensión,
**so that** pueda corregir una suspensión automática que resultó ser un falso positivo (REQ-F-015) sin necesitar acceso directo a la base de datos.

**INVEST:** Independiente de la detección automática de REQ-F-015 (esta historia es sobre la administración posterior, no la detección); negociable en si se añade un motivo obligatorio al revertir; valiosa porque sin ella un falso positivo deja a un usuario suspendido 15 días sin recurso; estimable y pequeña (listar, historial, revertir); testable mediante la reversión de una suspensión ya inexistente.

```gherkin
Escenario: Revertir la suspensión de una cuenta ya no suspendida
  Given que una cuenta no tiene ninguna suspensión activa
  When el Administrador solicita revertir su suspensión
  Then el sistema no debe dejar la cuenta en un estado inconsistente
```
