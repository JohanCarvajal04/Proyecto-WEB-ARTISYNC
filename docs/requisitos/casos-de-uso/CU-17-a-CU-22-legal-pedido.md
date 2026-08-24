# Casos de Uso — Módulo Legal, Pedido y Finanzas

---

## CU-17: Generar contrato del pedido
**Trazabilidad:** REQ-F-017 / HU-17
**Prueba de integración:** `ContratoServicioImplTest`

**1. Actor principal y objetivo:** Sistema (iniciado por el Cliente al contratar un servicio) — generar el documento de contrato a partir de una plantilla.

**Nivel:** Meta de usuario

**Precondición:** El Cliente seleccionó un servicio y confirmó los términos (precio, revisiones incluidas).

**Garantía de éxito:** El contrato HTML se genera con todas las variables correctamente sustituidas.

**2. Escenario principal de éxito:**
1. El Cliente confirma la contratación de un servicio.
2. El sistema recupera la plantilla de contrato activa.
3. El sistema sustituye las variables (partes, servicio, precio, revisiones, fecha) con los datos reales del pedido.
4. El sistema genera el documento HTML y lo asocia al pedido, quedando pendiente de firma (continúa en CU-18).

**3. Extensiones:**
- 2a. No existe una plantilla activa configurada.

**4. Manejo de extensiones:**
- 2a1. El sistema notifica al administrador la ausencia de plantilla y detiene el flujo de contratación. Termina.

---

## CU-18: Firmar el contrato electrónicamente
**Trazabilidad:** REQ-F-018 / HU-18
**Prueba de integración:** `ContratoServicioImplTest`

**1. Actor principal y objetivo:** Cliente o Creador — firmar electrónicamente el contrato generado para su pedido.

**Nivel:** Meta de usuario

**Precondición:** El contrato fue generado (CU-17) y está pendiente de firma de ambas partes.

**Garantía de éxito:** El pedido no avanza de etapa hasta que ambas firmas existan; el PDF final incluye los hashes de firma.

**2. Escenario principal de éxito:**
1. La parte (Cliente o Creador) revisa el contrato.
2. La parte confirma su firma electrónica mediante una acción explícita.
3. El sistema registra la firma con hash y marca de tiempo.
4. Si ambas partes ya firmaron, el sistema habilita la descarga del PDF final y el pedido avanza de etapa (continúa CU-19).

**3. Extensiones:**
- 4a. Solo una de las dos partes ha firmado.

**4. Manejo de extensiones:**
- 4a1. El sistema mantiene el pedido en espera de la firma restante y no genera el PDF final. Termina.

---

## CU-19: Avanzar el flujo de trabajo del pedido
**Trazabilidad:** REQ-F-019 / HU-19
**Prueba de integración:** `FlujoTrabajoServicioImplTest` · `PedidoServicioImplFlujoTest`

**1. Actor principal y objetivo:** Creador — actualizar la etapa actual del pedido a medida que avanza el trabajo.

**Nivel:** Meta de usuario

**Precondición:** El contrato está firmado por ambas partes (CU-18 completado).

**Garantía de éxito:** Cada transición de etapa queda registrada con marca de tiempo y visible para el Cliente en tiempo real.

**2. Escenario principal de éxito:**
1. El Creador consulta las etapas configuradas para la categoría del servicio.
2. El Creador marca la etapa actual como completada.
3. El sistema registra la transición con marca de tiempo.
4. El sistema avanza el pedido a la siguiente etapa y notifica al Cliente en tiempo real.

**3. Extensiones:**
- 2a. La etapa actual es la última configurada (entrega final).

**4. Manejo de extensiones:**
- 2a1. El sistema no avanza de etapa automáticamente; requiere la aprobación del entregable por el Cliente (continúa en CU-21). Termina.

---

## CU-20: Pagar el pedido vía PayPal
**Trazabilidad:** REQ-F-020 / HU-20
**Prueba de integración:** `PagoServicioImplWebhookTest`

**1. Actor principal y objetivo:** Cliente — pagar el pedido a través de PayPal, quedando los fondos en garantía.

**Nivel:** Meta de usuario

**Precondición:** El contrato fue firmado por ambas partes.

**Garantía de éxito:** El estado de fondos del pedido cambia a "en garantía" únicamente tras confirmación válida del webhook de PayPal.

**2. Escenario principal de éxito:**
1. El sistema genera una orden de pago vía PayPal Orders v2.
2. El Cliente completa el pago en la interfaz de PayPal.
3. PayPal envía un webhook de confirmación al backend.
4. El sistema verifica la firma del webhook.
5. El sistema actualiza el estado de fondos del pedido a "en garantía".

**3. Extensiones:**
- 4a. La firma del webhook no es válida.

**4. Manejo de extensiones:**
- 4a1. El sistema descarta el evento, no actualiza el estado del pedido y registra el intento en el log de auditoría. Termina.

---

## CU-21: Aprobar el entregable y liberar fondos
**Trazabilidad:** REQ-F-021 / HU-21
**Prueba de integración:** `EntregableServicioImplTest`

**1. Actor principal y objetivo:** Cliente — aprobar el entregable final para liberar el pago al Creador.

**Nivel:** Meta de usuario

**Precondición:** El Creador subió el entregable con marca de agua; los fondos están en garantía (CU-20 completado).

**Garantía de éxito:** Los fondos se liberan al Creador solo tras la aprobación explícita del Cliente, y la comisión de la plataforma queda registrada.

**2. Escenario principal de éxito:**
1. El Cliente previsualiza el entregable con marca de agua.
2. El Cliente aprueba el entregable.
3. El sistema libera los fondos en garantía al Creador.
4. El sistema habilita la descarga de la versión sin marca de agua para el Cliente.
5. El sistema registra automáticamente la comisión de la plataforma sobre la transacción.

**3. Extensiones:**
- 2a. El Cliente rechaza el entregable y solicita una revisión (continúa en CU-22).

**4. Manejo de extensiones:**
- 2a1. El sistema no libera fondos y crea un ticket de revisión asociado. Termina (continúa en CU-22).

---

## CU-22: Solicitar una revisión adicional
**Trazabilidad:** REQ-F-022 / HU-22
**Prueba de integración:** `TicketRevisionServicioImplTest`

**1. Actor principal y objetivo:** Cliente — solicitar ajustes al entregable una vez agotadas las revisiones incluidas en el contrato.

**Nivel:** Meta de usuario

**Precondición:** El Cliente ya usó todas las revisiones incluidas en el contrato original.

**Garantía de éxito:** La revisión adicional solo se ejecuta tras confirmarse el pago correspondiente, o el ticket se rechaza automáticamente si no se paga a tiempo.

**2. Escenario principal de éxito:**
1. El Cliente solicita una revisión adicional sobre el entregable.
2. El sistema genera un ticket de revisión y un nuevo enlace de pago con el cargo configurado.
3. El Cliente completa el pago del cargo de revisión.
4. El sistema habilita al Creador para trabajar en la revisión solicitada.

**3. Extensiones:**
- 3a. Pasan 48 horas sin que el pago se complete.

**4. Manejo de extensiones:**
- 3a1. El sistema rechaza automáticamente el ticket de revisión y notifica al Cliente. Termina.
