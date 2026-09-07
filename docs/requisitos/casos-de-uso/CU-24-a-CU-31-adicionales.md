# Casos de Uso — Requisitos adicionales (post v1.0.0)

Estos ocho casos de uso corresponden a REQ-F-024 a REQ-F-031, incorporados en v1.1.2 tras auditar el SRS contra código ya implementado y probado. Ver `docs/requisitos/SRS.md` §3.1 y `CHANGELOG-REQ.md` v1.2.0.

---

## CU-24: Solicitar y aprobar un retiro de fondos
**Trazabilidad:** REQ-F-024 / HU-24
**Prueba de integración:** `SolicitudRetiroServicioImplTest`, `SolicitudRetiroConcurrenciaIT`

**1. Actor principal y objetivo:** Creador — solicitar el retiro de su saldo disponible; Auditor Financiero — aprobar, rechazar o reintentar la solicitud.

**Nivel:** Meta de usuario

**Precondición:** El Creador tiene saldo disponible y correo de PayPal configurado.

**Garantía de éxito:** La solicitud aprobada ejecuta el pago vía PayPal Payouts y actualiza el saldo del Creador.

**2. Escenario principal de éxito:**
1. El Creador configura su correo de PayPal.
2. El Creador solicita el retiro de un monto dentro de su saldo disponible y por encima del mínimo configurado.
3. El Auditor Financiero revisa la cola de solicitudes pendientes.
4. El Auditor Financiero aprueba la solicitud; el sistema ejecuta el pago vía PayPal Payouts.

**3. Extensiones:**
- 2a. El monto solicitado es menor al mínimo configurado, mayor al saldo disponible, o ya existe una solicitud en curso.
- 4a. El pago vía PayPal Payouts falla.

**4. Manejo de extensiones:**
- 2a1. El sistema rechaza la solicitud sin crear ningún registro. Termina.
- 4a1. La solicitud queda en estado "Fallido"; el Auditor Financiero puede reintentarla. Termina.

---

## CU-25: Auditar los fondos en garantía (escrow)
**Trazabilidad:** REQ-F-025 / HU-25
**Prueba de integración:** `PagoGarantiaAuditoriaServicioImplTest`

**1. Actor principal y objetivo:** Auditor Financiero — supervisar cuánto dinero está retenido en garantía y en qué estado.

**Nivel:** Meta de usuario

**Precondición:** Existen pedidos con pagos retenidos por el patrón escrow.

**Garantía de éxito:** El Auditor Financiero obtiene un listado filtrable, un detalle con historial de transacciones, y un resumen agregado por estado.

**2. Escenario principal de éxito:**
1. El Auditor Financiero solicita el listado paginado de pagos en garantía, opcionalmente filtrado.
2. El Auditor Financiero abre el detalle de un pago puntual y revisa su historial de transacciones.
3. El Auditor Financiero solicita el resumen agregado de fondos por estado.

**3. Extensiones:**
- 2a. El pago solicitado no existe.

**4. Manejo de extensiones:**
- 2a1. El sistema responde 404. Termina.

---

## CU-26: Exportar un reporte administrativo
**Trazabilidad:** REQ-F-026 / HU-26
**Prueba de integración:** `ReporteFinancieroServicioImplTest`, `ReporteContratoServicioImplTest`

**1. Actor principal y objetivo:** Administrador — obtener el reporte de comisiones de un creador o el listado de contratos formalizados, en un archivo exportable.

**Nivel:** Meta de usuario

**Precondición:** El Administrador tiene el permiso de exportación específico del reporte solicitado.

**Garantía de éxito:** El sistema entrega el documento en el formato solicitado (CSV, XLSX o PDF).

**2. Escenario principal de éxito:**
1. El Administrador consulta el reporte con los filtros deseados.
2. El Administrador solicita la exportación en el formato elegido.
3. El sistema genera y entrega el documento.

**3. Extensiones:**
- 2a. El número de transacciones del reporte excede el tope de filas admitido por el formato solicitado.

**4. Manejo de extensiones:**
- 2a1. El sistema rechaza la exportación con un error de regla de negocio, sin generar el documento. Termina.

---

## CU-27: Consultar y gestionar mis notificaciones
**Trazabilidad:** REQ-F-027 / HU-27
**Prueba de integración:** `NotificacionServiceImplTest`

**1. Actor principal y objetivo:** Usuario autenticado — revisar sus notificaciones y marcarlas como leídas.

**Nivel:** Meta de usuario

**Precondición:** El usuario tiene notificaciones generadas por eventos de otros módulos.

**Garantía de éxito:** El usuario ve únicamente sus propias notificaciones y su contador de no leídas se mantiene correcto.

**2. Escenario principal de éxito:**
1. El usuario lista sus notificaciones paginadas.
2. El usuario marca una o todas las notificaciones como leídas.
3. El sistema recalcula el contador de no leídas.

**3. Extensiones:**
- 2a. El usuario intenta marcar como leída una notificación que no le pertenece.

**4. Manejo de extensiones:**
- 2a1. El sistema responde 404. Termina.

---

## CU-28: Revisar y moderar el catálogo de categorías
**Trazabilidad:** REQ-F-028 / HU-28
**Prueba de integración:** `CategoriaServicioImplTest`, `ServicioCatalogoServicioImplTest`

**1. Actor principal y objetivo:** Moderador/Administrador — aprobar categorías creadas por autoservicio y corregir servicios ajenos mal clasificados.

**Nivel:** Meta de usuario

**Precondición:** Existen categorías pendientes de revisión o servicios con subcategorías inapropiadas.

**Garantía de éxito:** Solo las categorías revisadas aparecen en el catálogo público; un servicio moderado conserva al menos una subcategoría.

**2. Escenario principal de éxito:**
1. El Moderador/Administrador lista las categorías pendientes de revisión.
2. El Moderador/Administrador marca una categoría como revisada.
3. El Moderador/Administrador, ante un servicio mal clasificado, le quita una subcategoría inapropiada.

**3. Extensiones:**
- 3a. La subcategoría a quitar es la última que tiene el servicio.

**4. Manejo de extensiones:**
- 3a1. El sistema rechaza la operación con un error de regla de negocio. Termina.

---

## CU-29: Calificar un pedido entregado
**Trazabilidad:** REQ-F-029 / HU-29
**Prueba de integración:** `ResenaServiceImplTest`

**1. Actor principal y objetivo:** Cliente — dejar una calificación y reseña sobre un pedido ya entregado.

**Nivel:** Meta de usuario

**Precondición:** El entregable del pedido ya fue aprobado por el Cliente.

**Garantía de éxito:** La reseña queda asociada al pedido y el promedio público del Creador se recalcula.

**2. Escenario principal de éxito:**
1. El Cliente crea una reseña (calificación 1-5 y comentario opcional) sobre un pedido con entregable liberado.
2. El sistema recalcula el promedio de calificaciones del Creador.
3. Cualquier visitante consulta las reseñas y el promedio del Creador.

**3. Extensiones:**
- 1a. El entregable del pedido aún no fue liberado.
- 1b. Ya existe una reseña del Cliente sobre ese pedido.

**4. Manejo de extensiones:**
- 1a1. El sistema rechaza la creación con un error de regla de negocio. Termina.
- 1b1. El sistema rechaza la creación como recurso duplicado. Termina.

---

## CU-30: Administrar el ciclo de vida de una cuenta de usuario
**Trazabilidad:** REQ-F-030 / HU-30
**Prueba de integración:** `AdminUserServiceImplTest`

**1. Actor principal y objetivo:** Administrador — gestionar el estado, roles y sesiones de una cuenta de usuario.

**Nivel:** Meta de usuario

**Precondición:** Existe la cuenta objetivo, distinta de la del Administrador que ejecuta la acción.

**Garantía de éxito:** El cambio de estado, rol o sesión se aplica de inmediato sobre la cuenta objetivo.

**2. Escenario principal de éxito:**
1. El Administrador busca y localiza al usuario objetivo.
2. El Administrador aplica la acción deseada: activar/desactivar/suspender, asignar roles, revocar sesiones, o eliminar lógicamente.
3. El sistema aplica el cambio y lo refleja de inmediato (una sesión revocada invalida cualquier JWT activo de ese usuario).

**3. Extensiones:**
- 2a. El Administrador intenta aplicar la acción sobre su propia cuenta (desactivar, cambiar sus propios roles, o eliminarse).

**4. Manejo de extensiones:**
- 2a1. El sistema rechaza la operación con un error de regla de negocio. Termina.

---

## CU-31: Elegir país y dar "me gusta" en el portafolio
**Trazabilidad:** REQ-F-031 / HU-31
**Prueba de integración:** `PaisServiceImplTest`, `LikePortafolioServiceImplTest`

**1. Actor principal y objetivo:** Usuario del sistema — seleccionar su país en un formulario, o expresar interés en un ítem de portafolio.

**Nivel:** Subfunción

**Precondición:** El catálogo de países activos existe; el ítem de portafolio existe.

**Garantía de éxito:** El país queda asociado al perfil; el estado de like refleja correctamente si el usuario ya dio like.

**2. Escenario principal de éxito:**
1. El usuario consulta el listado de países activos para un formulario de perfil.
2. El usuario, en un ítem de portafolio que le interesa, da "me gusta".
3. El sistema actualiza el conteo público de likes del ítem.

**3. Extensiones:**
- 2a. El usuario ya había dado like al mismo ítem.

**4. Manejo de extensiones:**
- 2a1. El sistema rechaza la operación como recurso duplicado. Termina.
