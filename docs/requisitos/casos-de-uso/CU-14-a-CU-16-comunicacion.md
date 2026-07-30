# Casos de Uso — Módulo Comunicación y Notificaciones

---

## CU-14: Conversar en el chat del pedido
**Trazabilidad:** REQ-F-014 / HU-14

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

## CU-16: Completar el briefing del pedido
**Trazabilidad:** REQ-F-016 / HU-16

**1. Actor principal y objetivo:** Cliente — responder el formulario de briefing configurado por el Creador.

**Nivel:** Meta de usuario

**Precondición:** El Creador configuró un formulario de briefing para su servicio (hasta 10 preguntas).

**Garantía de éxito:** Las respuestas quedan almacenadas y no editables, asociadas al pedido.

**2. Escenario principal de éxito:**
1. El Cliente inicia un pedido y el sistema le muestra el formulario de briefing del servicio.
2. El Cliente responde las preguntas.
3. El Cliente envía el formulario.
4. El sistema persiste las respuestas y las marca como no editables.

**3. Extensiones:**
- 3a. El Cliente intenta modificar una respuesta ya enviada.

**4. Manejo de extensiones:**
- 3a1. El sistema rechaza la edición y muestra las respuestas en modo de solo lectura. Termina.
