# Casos de Uso — Módulo Comunicación y Notificaciones

---

## CU-14: Conversar en el chat del pedido
**Trazabilidad:** REQ-F-014 / HU-14
**Prueba de integración:** `ChatServiceImplTest`

**1. Actor principal y objetivo:** Cliente o Creador — intercambiar mensajes en tiempo real dentro de un pedido activo.

**Nivel:** Meta de usuario

**Precondición:** Ambas partes firmaron el contrato del pedido (ver CU-18).

**Garantía de éxito:** Los mensajes se entregan en tiempo real mientras el pedido está activo.

**2. Escenario principal de éxito:**
1. El sistema crea automáticamente la sala de chat al completarse la firma del contrato.
2. Cliente y Creador se conectan a la sala vía WebSocket.
3. Cualquiera de las partes envía un mensaje.
4. El sistema lo entrega en tiempo real a la otra parte y lo analiza por contenido (ver CU-15).

**3. Extensiones:**
- 1a. El pedido alcanza el estado "Entregado" o "Cancelado".

**4. Manejo de extensiones:**
- 1a1. El sistema cierra la sala de chat para nuevos mensajes, conservando el historial de solo lectura. Termina.

---

## CU-15: Bloquear contenido de contacto externo
**Trazabilidad:** REQ-F-015 / HU-15
**Prueba de integración:** `MensajeFilterServiceImplTest` · `ChatServiceImplTest`

**1. Actor principal y objetivo:** Sistema (actor de apoyo, iniciado por Cliente o Creador al enviar un mensaje) — impedir el intercambio de datos de contacto directo.

**Nivel:** Subfunción (invocado desde CU-14, paso 4)

**Precondición:** Un mensaje está siendo enviado en un chat activo.

**Garantía de éxito:** Ningún mensaje con teléfono o correo llega a la otra parte.

**2. Escenario principal de éxito:**
1. El sistema analiza el texto del mensaje antes de entregarlo.
2. El sistema no detecta patrones de teléfono ni correo.
3. El mensaje se entrega normalmente (continúa CU-14, paso 4).

**3. Extensiones:**
- 2a. El sistema detecta un patrón de teléfono o correo.

**4. Manejo de extensiones:**
- 2a1. El sistema bloquea la entrega, notifica al remitente y registra una infracción.
- 2a2. Si el usuario acumula 3 infracciones en 30 días, el sistema suspende la cuenta por 15 días. Termina.

---

## CU-16: Completar el cuestionario (briefing) al crear el pedido
**Trazabilidad:** REQ-F-016 / HU-16
**Prueba de integración:** `BriefingServiceImplTest` · `PedidoServicioImplTest`

**1. Actor principal y objetivo:** Cliente — responder el cuestionario del servicio, si tiene uno asignado, como parte de la creación del pedido.

**Nivel:** Meta de usuario

**Precondición:** El Creador, al crear o editar el servicio, le asignó uno de sus cuestionarios propios (hasta 10 preguntas). Si el servicio no tiene ninguno asignado, este caso de uso no aplica y el pedido se crea sin pedir preguntas adicionales (ver CU-11/CU-19 — creación del pedido).

**Garantía de éxito:** El pedido solo se crea si todas las preguntas del cuestionario quedan respondidas; las respuestas quedan almacenadas, no editables, asociadas al pedido.

**2. Escenario principal de éxito:**
1. El Cliente elige un servicio que tiene un cuestionario asignado y abre el formulario de creación de pedido.
2. El sistema muestra, dentro del mismo formulario, las preguntas del cuestionario del servicio.
3. El Cliente responde todas las preguntas y confirma la creación del pedido.
4. El sistema valida que todas las preguntas tengan respuesta, crea el pedido y persiste las respuestas como completadas y no editables, en la misma operación.

**3. Extensiones:**
- 3a. El Cliente confirma con una o más preguntas sin responder.
- 3b. El Cliente intenta modificar una respuesta ya enviada, desde el detalle del pedido.

**4. Manejo de extensiones:**
- 3a1. El sistema rechaza la creación del pedido (no se persiste el pedido ni ninguna respuesta) e indica qué pregunta falta. Vuelve al paso 3.
- 3b1. El sistema no ofrece edición: el detalle del pedido muestra las respuestas en modo de solo lectura. Termina.
